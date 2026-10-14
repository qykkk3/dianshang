package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.BusinessException;
import com.mall.common.PageResult;
import com.mall.common.enums.OrderStatus;
import com.mall.common.util.ExcelUtils;
import com.mall.common.SysConfigKeys;
import com.mall.dto.OrderItemDTO;
import com.mall.dto.OrderPreviewDTO;
import com.mall.dto.OrderQueryDTO;
import com.mall.dto.OrderSubmitDTO;
import com.mall.entity.Cart;
import com.mall.entity.Category;
import com.mall.entity.Coupon;
import com.mall.entity.ExpressTrack;
import com.mall.entity.OrderItem;
import com.mall.entity.Orders;
import com.mall.entity.Product;
import com.mall.entity.ProductSku;
import com.mall.entity.Refund;
import com.mall.entity.SysConfig;
import com.mall.entity.User;
import com.mall.entity.UserAddress;
import com.mall.entity.UserCoupon;
import com.mall.mapper.CartMapper;
import com.mall.mapper.CategoryMapper;
import com.mall.mapper.CouponMapper;
import com.mall.mapper.ExpressTrackMapper;
import com.mall.mapper.OrderItemMapper;
import com.mall.mapper.OrdersMapper;
import com.mall.mapper.ProductMapper;
import com.mall.mapper.ProductSkuMapper;
import com.mall.mapper.RefundMapper;
import com.mall.mapper.SysConfigMapper;
import com.mall.mapper.UserAddressMapper;
import com.mall.mapper.UserCouponMapper;
import com.mall.mapper.UserMapper;
import com.mall.security.LoginUser;
import com.mall.security.SecurityUtils;
import com.mall.service.OrderService;
import com.mall.service.RefundService;
import com.mall.vo.AddressVO;
import com.mall.vo.CartVO;
import com.mall.vo.ExpressTrackVO;
import com.mall.vo.OrderCountVO;
import com.mall.vo.OrderDetailVO;
import com.mall.vo.OrderItemVO;
import com.mall.vo.OrderPreviewVO;
import com.mall.vo.OrderSubmitVO;
import com.mall.vo.OrderVO;
import com.mall.vo.UserCouponVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 订单服务实现
 *
 * <p>金额一律由服务端依据 SKU 现价重算，前端传入金额不参与计算；
 * 下单即原子扣减库存，取消/关闭/退货入库时回补库存与销量。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final DateTimeFormatter ORDER_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter EXPORT_DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 折扣券最高优惠金额 */
    private static final BigDecimal MAX_DISCOUNT_AMOUNT = new BigDecimal("100.00");
    /** 实付金额下限 */
    private static final BigDecimal MIN_PAY_AMOUNT = new BigDecimal("0.01");
    /** 导出最大条数 */
    private static final int EXPORT_MAX = 5000;

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartMapper cartMapper;
    private final UserAddressMapper userAddressMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponMapper couponMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final ExpressTrackMapper expressTrackMapper;
    private final RefundMapper refundMapper;
    private final RefundService refundService;
    private final SysConfigMapper sysConfigMapper;

    @Value("${mall.shop.freight-fee:10.00}")
    private BigDecimal freightFee;

    @Value("${mall.shop.free-freight-amount:99.00}")
    private BigDecimal freeFreightAmount;

    @Value("${mall.shop.order-auto-cancel-minutes:30}")
    private Integer orderAutoCancelMinutes;

    /* ==================================================================
     *  一、结算预览
     * ================================================================== */

    @Override
    public OrderPreviewVO preview(OrderPreviewDTO dto) {
        Long userId = SecurityUtils.getUserId();
        List<LineItem> items = prepare(userId, dto.getCartIds(), dto.getBuyNow());
        BigDecimal totalAmount = sumTotal(items);
        BigDecimal freightAmount = calcFreight(totalAmount);
        BigDecimal discountAmount = BigDecimal.ZERO;
        Long userCouponId = null;

        if (dto.getUserCouponId() != null) {
            CouponSelection selection = requireAvailableCoupon(userId, dto.getUserCouponId(), totalAmount, items);
            discountAmount = calcDiscount(selection.getCoupon(), totalAmount);
            userCouponId = selection.getUserCoupon().getId();
        }

        OrderPreviewVO vo = new OrderPreviewVO();
        vo.setItems(items.stream().map(this::toPreviewItem).collect(Collectors.toList()));
        vo.setAddress(defaultAddress(userId));
        vo.setTotalAmount(totalAmount);
        vo.setFreightAmount(freightAmount);
        vo.setDiscountAmount(discountAmount);
        vo.setPayAmount(calcPayAmount(totalAmount, freightAmount, discountAmount));
        vo.setAvailableCoupons(availableCoupons(userId, totalAmount, items));
        vo.setUserCouponId(userCouponId);
        return vo;
    }

    /* ==================================================================
     *  二、提交订单
     * ================================================================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderSubmitVO submit(OrderSubmitDTO dto) {
        Long userId = SecurityUtils.getUserId();
        // 1. 收货地址校验与快照
        UserAddress address = userAddressMapper.selectById(dto.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("收货地址不存在或无权使用");
        }
        // 2. 服务端重新校验并计算金额（不信任前端金额）
        List<LineItem> items = prepare(userId, dto.getCartIds(), dto.getBuyNow());
        BigDecimal totalAmount = sumTotal(items);
        BigDecimal freightAmount = calcFreight(totalAmount);
        BigDecimal discountAmount = BigDecimal.ZERO;
        UserCoupon userCoupon = null;
        if (dto.getUserCouponId() != null) {
            CouponSelection selection = requireAvailableCoupon(userId, dto.getUserCouponId(), totalAmount, items);
            userCoupon = selection.getUserCoupon();
            discountAmount = calcDiscount(selection.getCoupon(), totalAmount);
        }
        BigDecimal payAmount = calcPayAmount(totalAmount, freightAmount, discountAmount);

        // 3. 写订单主表
        Orders orders = new Orders();
        orders.setOrderNo(generateOrderNo());
        orders.setUserId(userId);
        orders.setTotalAmount(totalAmount);
        orders.setFreightAmount(freightAmount);
        orders.setDiscountAmount(discountAmount);
        orders.setPayAmount(payAmount);
        orders.setUserCouponId(userCoupon == null ? null : userCoupon.getId());
        orders.setStatus(OrderStatus.UNPAID.getCode());
        orders.setReceiverName(address.getReceiver());
        orders.setReceiverPhone(address.getPhone());
        orders.setReceiverAddress(AddressServiceImpl.buildFullAddress(address.getProvince(),
                address.getCity(), address.getDistrict(), address.getDetail()));
        orders.setRemark(dto.getRemark());
        orders.setItemCount(items.stream().mapToInt(LineItem::getQuantity).sum());
        ordersMapper.insert(orders);

        // 4. 写订单明细（商品快照）
        for (LineItem item : items) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(orders.getId());
            orderItem.setOrderNo(orders.getOrderNo());
            orderItem.setUserId(userId);
            orderItem.setProductId(item.getProduct().getId());
            orderItem.setSkuId(item.getSku().getId());
            orderItem.setProductName(item.getProduct().getName());
            orderItem.setProductImage(StringUtils.hasText(item.getSku().getImage())
                    ? item.getSku().getImage() : item.getProduct().getMainImage());
            orderItem.setSpecInfo(item.getSku().getSpecInfo());
            orderItem.setPrice(item.getSku().getPrice());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setTotalAmount(item.totalAmount());
            orderItem.setIsCommented(0);
            orderItemMapper.insert(orderItem);
            // 5. 下单即扣减库存（原子扣减，失败则整体回滚）
            deductStock(item);
        }

        // 6. 核销优惠券
        if (userCoupon != null) {
            useCoupon(userCoupon, orders.getId());
        }
        // 7. 清理购物车记录
        if (dto.getCartIds() != null && !dto.getCartIds().isEmpty()) {
            cartMapper.delete(Wrappers.<Cart>lambdaQuery()
                    .eq(Cart::getUserId, userId)
                    .in(Cart::getId, dto.getCartIds()));
        }

        OrderSubmitVO vo = new OrderSubmitVO();
        vo.setOrderId(orders.getId());
        vo.setOrderNo(orders.getOrderNo());
        vo.setPayAmount(payAmount);
        log.info("买家下单成功：userId={}, orderNo={}, payAmount={}", userId, orders.getOrderNo(), payAmount);
        return vo;
    }

    /* ==================================================================
     *  三、支付 / 取消 / 收货 / 删除
     * ================================================================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(String orderNo) {
        Orders orders = requireOwnOrder(orderNo);
        if (!OrderStatus.UNPAID.getCode().equals(orders.getStatus())) {
            throw new BusinessException("当前订单状态不可支付");
        }
        Orders update = new Orders();
        update.setId(orders.getId());
        update.setStatus(OrderStatus.PAID.getCode());
        // 模拟支付：无外部支付通道，直接置为已支付
        update.setPayType(1);
        update.setPayTime(LocalDateTime.now());
        ordersMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(String orderNo) {
        Orders orders = requireOwnOrder(orderNo);
        if (!OrderStatus.UNPAID.getCode().equals(orders.getStatus())) {
            throw new BusinessException("仅待付款订单可以取消");
        }
        cancelInternal(orders);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceive(String orderNo) {
        Orders orders = requireOwnOrder(orderNo);
        if (!OrderStatus.SHIPPED.getCode().equals(orders.getStatus())) {
            throw new BusinessException("仅待收货订单可以确认收货");
        }
        LocalDateTime now = LocalDateTime.now();
        Orders update = new Orders();
        update.setId(orders.getId());
        update.setStatus(OrderStatus.FINISHED.getCode());
        update.setReceiveTime(now);
        update.setFinishTime(now);
        ordersMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(String orderNo) {
        Orders orders = requireOwnOrder(orderNo);
        Integer status = orders.getStatus();
        if (!OrderStatus.FINISHED.getCode().equals(status) && !OrderStatus.CANCELLED.getCode().equals(status)) {
            throw new BusinessException("仅已完成或已取消的订单可以删除");
        }
        // order_item 无 deleted 字段，保持不动，随订单主表逻辑删除一并对买家不可见
        ordersMapper.deleteById(orders.getId());
    }

    /* ==================================================================
     *  四、查询
     * ================================================================== */

    @Override
    public PageResult<OrderVO> pageBuyerOrders(OrderQueryDTO query) {
        Long userId = SecurityUtils.getUserId();
        autoCancelExpiredOrders(userId);
        LambdaQueryWrapper<Orders> wrapper = buildWrapper(query);
        wrapper.eq(Orders::getUserId, userId);
        return pageOrders(wrapper, query);
    }

    @Override
    public PageResult<OrderVO> pageAdminOrders(OrderQueryDTO query) {
        LambdaQueryWrapper<Orders> wrapper = buildWrapper(query);
        wrapper.eq(query.getUserId() != null, Orders::getUserId, query.getUserId());
        return pageOrders(wrapper, query);
    }

    @Override
    public OrderDetailVO getBuyerDetail(String orderNo) {
        Long userId = SecurityUtils.getUserId();
        Orders orders = ordersMapper.selectOne(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getOrderNo, orderNo)
                .eq(Orders::getUserId, userId));
        if (orders == null) {
            throw new BusinessException("订单不存在或无权查看");
        }
        return toDetailVO(orders);
    }

    @Override
    public OrderDetailVO getAdminDetail(Long id) {
        Orders orders = ordersMapper.selectById(id);
        if (orders == null) {
            throw new BusinessException("订单不存在");
        }
        return toDetailVO(orders);
    }

    @Override
    public List<ExpressTrackVO> listTracks(String orderNo) {
        Long userId = SecurityUtils.getUserId();
        Orders orders = ordersMapper.selectOne(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getOrderNo, orderNo)
                .eq(Orders::getUserId, userId));
        if (orders == null) {
            throw new BusinessException("订单不存在或无权查看");
        }
        List<ExpressTrack> tracks = expressTrackMapper.selectList(Wrappers.<ExpressTrack>lambdaQuery()
                .eq(ExpressTrack::getOrderId, orders.getId())
                .orderByDesc(ExpressTrack::getAcceptTime));
        return tracks.stream().map(this::toTrackVO).collect(Collectors.toList());
    }

    @Override
    public OrderCountVO countByStatus() {
        Long userId = SecurityUtils.getUserId();
        autoCancelExpiredOrders(userId);
        List<Orders> list = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                .select(Orders::getId, Orders::getStatus, Orders::getReceiveTime)
                .eq(Orders::getUserId, userId));
        OrderCountVO vo = new OrderCountVO();
        for (Orders orders : list) {
            Integer status = orders.getStatus();
            if (OrderStatus.UNPAID.getCode().equals(status)) {
                vo.setUnpaid(vo.getUnpaid() + 1);
            } else if (OrderStatus.PAID.getCode().equals(status)) {
                vo.setPaid(vo.getPaid() + 1);
            } else if (OrderStatus.SHIPPED.getCode().equals(status)) {
                vo.setShipped(vo.getShipped() + 1);
            } else if (OrderStatus.FINISHED.getCode().equals(status)) {
                vo.setFinished(vo.getFinished() + 1);
            } else if (OrderStatus.AFTER_SALE.getCode().equals(status)) {
                vo.setAfterSale(vo.getAfterSale() + 1);
            }
        }
        // 待评价：已完成订单中未评价的明细数
        List<Orders> finishedOrders = list.stream()
                .filter(o -> OrderStatus.FINISHED.getCode().equals(o.getStatus()))
                .collect(Collectors.toList());
        if (!finishedOrders.isEmpty()) {
            Long toComment = orderItemMapper.selectCount(Wrappers.<OrderItem>lambdaQuery()
                    .in(OrderItem::getOrderId, finishedOrders.stream().map(Orders::getId).collect(Collectors.toList()))
                    .eq(OrderItem::getIsCommented, 0));
            vo.setToComment(toComment == null ? 0L : toComment);
        }
        return vo;
    }

    /* ==================================================================
     *  五、管理端操作
     * ================================================================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ship(Long id, String company, String no) {
        Orders orders = requireOrder(id);
        if (!OrderStatus.PAID.getCode().equals(orders.getStatus())) {
            throw new BusinessException("仅待发货订单可以发货");
        }
        LocalDateTime now = LocalDateTime.now();
        Orders update = new Orders();
        update.setId(orders.getId());
        update.setStatus(OrderStatus.SHIPPED.getCode());
        update.setShipTime(now);
        update.setExpressCompany(company);
        update.setExpressNo(no);
        ordersMapper.updateById(update);
        // 发货自动生成两条轨迹：已揽收（当前时间）、运输中（+2 小时）
        insertTrack(orders, company, no, "已揽收", "快件已由" + company + "揽收", null, now);
        insertTrack(orders, company, no, "运输中", "快件已从发货仓发出，正在运输途中", null, now.plusHours(2));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addTrack(Long id, String status, String context, String location) {
        Orders orders = requireOrder(id);
        if (!StringUtils.hasText(orders.getExpressCompany()) || !StringUtils.hasText(orders.getExpressNo())) {
            throw new BusinessException("该订单尚未发货，无法追加物流轨迹");
        }
        insertTrack(orders, orders.getExpressCompany(), orders.getExpressNo(),
                StringUtils.hasText(status) ? status : "运输中",
                StringUtils.hasText(context) ? context : "物流信息更新",
                location, LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAdminRemark(Long id, String adminRemark) {
        Orders orders = requireOrder(id);
        Orders update = new Orders();
        update.setId(orders.getId());
        update.setAdminRemark(adminRemark);
        ordersMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePrice(Long id, BigDecimal payAmount) {
        Orders orders = requireOrder(id);
        if (!OrderStatus.UNPAID.getCode().equals(orders.getStatus())) {
            throw new BusinessException("仅待付款订单可以改价");
        }
        if (payAmount == null || payAmount.compareTo(MIN_PAY_AMOUNT) < 0) {
            throw new BusinessException("实付金额不能小于 0.01");
        }
        BigDecimal newPay = payAmount.setScale(2, RoundingMode.HALF_UP);
        // 保持金额等式「商品总额 + 运费 - 优惠 = 实付」成立：
        // 改价差额计入优惠金额（正数表示让利，负数表示加收，加收时记为负优惠）
        BigDecimal expected = calcPayAmount(orders.getTotalAmount(), orders.getFreightAmount(), orders.getDiscountAmount());
        BigDecimal diff = expected.subtract(newPay);
        BigDecimal newDiscount = orders.getDiscountAmount().add(diff);
        if (newDiscount.compareTo(BigDecimal.ZERO) < 0) {
            newDiscount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        Orders update = new Orders();
        update.setId(orders.getId());
        update.setPayAmount(newPay);
        update.setDiscountAmount(newDiscount.setScale(2, RoundingMode.HALF_UP));
        ordersMapper.updateById(update);
        log.info("订单改价：orderNo={} 原应付={} 新应付={} 调整后优惠={}",
                orders.getOrderNo(), orders.getPayAmount(), newPay, newDiscount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void close(Long id) {
        Orders orders = requireOrder(id);
        if (!OrderStatus.UNPAID.getCode().equals(orders.getStatus())) {
            throw new BusinessException("仅待付款订单可以关闭");
        }
        // 与买家取消逻辑一致：回滚库存/销量/优惠券
        cancelInternal(orders);
    }

    @Override
    public void exportAdminOrders(OrderQueryDTO query, HttpServletResponse response) {
        LambdaQueryWrapper<Orders> wrapper = buildWrapper(query);
        wrapper.eq(query.getUserId() != null, Orders::getUserId, query.getUserId());
        wrapper.orderByDesc(Orders::getCreateTime);

        // 分页插件限制了单页最大条数（maxLimit=200），此处循环翻页累计，最多导出 EXPORT_MAX 条
        long batch = Math.min(200L, EXPORT_MAX);
        List<Orders> list = new ArrayList<>(Math.min(EXPORT_MAX, 256));
        long current = 1L;
        while (list.size() < EXPORT_MAX) {
            Page<Orders> page = ordersMapper.selectPage(new Page<>(current, batch, false), wrapper);
            List<Orders> records = page.getRecords();
            if (records == null || records.isEmpty()) {
                break;
            }
            for (Orders orders : records) {
                if (list.size() >= EXPORT_MAX) {
                    break;
                }
                list.add(orders);
            }
            if (records.size() < batch) {
                break;
            }
            current++;
        }

        Map<Long, String> nicknameMap = nicknameMap(list.stream()
                .map(Orders::getUserId).collect(Collectors.toSet()));

        String[] headers = {"订单号", "买家", "收货人", "电话", "收货地址", "商品总额", "运费", "优惠",
                "实付", "状态", "下单时间", "支付时间", "发货时间", "快递公司", "快递单号"};
        List<Object[]> rows = new ArrayList<>(list.size());
        for (Orders orders : list) {
            rows.add(new Object[]{
                    orders.getOrderNo(),
                    nicknameMap.getOrDefault(orders.getUserId(), ""),
                    orders.getReceiverName(),
                    orders.getReceiverPhone(),
                    orders.getReceiverAddress(),
                    orders.getTotalAmount(),
                    orders.getFreightAmount(),
                    orders.getDiscountAmount(),
                    orders.getPayAmount(),
                    OrderStatus.descOf(orders.getStatus()),
                    formatTime(orders.getCreateTime()),
                    formatTime(orders.getPayTime()),
                    formatTime(orders.getShipTime()),
                    orders.getExpressCompany(),
                    orders.getExpressNo()
            });
        }
        ExcelUtils.export(response, "订单列表", "订单数据", headers, rows);
    }

    @Override
    public Map<String, Object> adminStatistics() {
        List<Orders> list = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery().select(Orders::getStatus));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("unpaid", countStatus(list, OrderStatus.UNPAID.getCode()));
        result.put("paid", countStatus(list, OrderStatus.PAID.getCode()));
        result.put("shipped", countStatus(list, OrderStatus.SHIPPED.getCode()));
        result.put("finished", countStatus(list, OrderStatus.FINISHED.getCode()));
        result.put("afterSale", countStatus(list, OrderStatus.AFTER_SALE.getCode()));
        return result;
    }

    /* ==================================================================
     *  六、内部方法
     * ================================================================== */

    /**
     * 解析订单来源并逐项校验，返回可下单的商品明细
     */
    private List<LineItem> prepare(Long userId, List<Long> cartIds, OrderItemDTO buyNow) {
        boolean hasBuyNow = buyNow != null && buyNow.getSkuId() != null;
        boolean hasCart = cartIds != null && !cartIds.isEmpty();
        if (hasBuyNow && hasCart) {
            throw new BusinessException("请勿同时使用购物车结算与立即购买");
        }
        if (!hasBuyNow && !hasCart) {
            throw new BusinessException("请选择要购买的商品");
        }
        List<LineItem> items = new ArrayList<>();
        if (hasBuyNow) {
            items.add(buildLineItem(buyNow.getSkuId(), buyNow.getQuantity()));
        } else {
            List<Cart> carts = cartMapper.selectList(Wrappers.<Cart>lambdaQuery()
                    .eq(Cart::getUserId, userId)
                    .in(Cart::getId, cartIds));
            if (carts.size() != new LinkedHashSet<>(cartIds).size()) {
                throw new BusinessException("购物车记录不存在或无权操作");
            }
            for (Cart cart : carts) {
                items.add(buildLineItem(cart.getSkuId(), cart.getQuantity()));
            }
        }
        if (items.isEmpty()) {
            throw new BusinessException("请选择要购买的商品");
        }
        return items;
    }

    /**
     * 校验商品/SKU 并构建下单明细
     */
    private LineItem buildLineItem(Long skuId, Integer quantity) {
        if (skuId == null) {
            throw new BusinessException("商品规格不能为空");
        }
        if (quantity == null || quantity < 1) {
            throw new BusinessException("购买数量必须大于 0");
        }
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new BusinessException("商品规格不存在");
        }
        Product product = productMapper.selectById(sku.getProductId());
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException("商品「" + (product == null ? "未知" : product.getName()) + "」已下架");
        }
        if (sku.getStatus() == null || sku.getStatus() != 1) {
            throw new BusinessException("商品「" + product.getName() + "」的该规格已停用");
        }
        if (sku.getStock() == null || sku.getStock() < quantity) {
            throw new BusinessException("库存不足：" + product.getName()
                    + (StringUtils.hasText(sku.getSpecInfo()) ? "（" + sku.getSpecInfo() + "）" : "")
                    + "，当前仅剩 " + (sku.getStock() == null ? 0 : sku.getStock()) + " 件");
        }
        return new LineItem(product, sku, quantity);
    }

    /**
     * 商品总额 = Σ SKU 单价 × 数量
     */
    private BigDecimal sumTotal(List<LineItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (LineItem item : items) {
            total = total.add(item.totalAmount());
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 运费：满包邮门槛免运费，否则收取固定运费
     *
     * <p>门槛与运费优先读取后台「系统配置」（sys_config 表，可在管理后台在线修改），
     * 读取不到时回退到 application.yml 中的 mall.shop.* 配置。</p>
     */
    private BigDecimal calcFreight(BigDecimal totalAmount) {
        BigDecimal threshold = configDecimal(SysConfigKeys.FREE_FREIGHT_AMOUNT, freeFreightAmount);
        if (threshold != null && totalAmount.compareTo(threshold) >= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal fee = configDecimal(SysConfigKeys.FREIGHT_FEE, freightFee);
        return (fee == null ? BigDecimal.ZERO : fee).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 读取系统配置中的数值项，读取不到或格式非法时返回默认值
     */
    private BigDecimal configDecimal(String key, BigDecimal defaultValue) {
        try {
            SysConfig config = sysConfigMapper.selectOne(
                    new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key).last("LIMIT 1"));
            if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
                return defaultValue;
            }
            return new BigDecimal(config.getConfigValue().trim());
        } catch (Exception e) {
            log.warn("读取系统配置 {} 失败，使用默认值 {}", key, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 读取系统配置中的整数项，读取不到或格式非法时返回默认值
     */
    private Integer configInteger(String key, Integer defaultValue) {
        BigDecimal value = configDecimal(key, defaultValue == null ? null : BigDecimal.valueOf(defaultValue));
        return value == null ? defaultValue : value.intValue();
    }

    /**
     * 应付金额 = 商品总额 + 运费 - 优惠，最低 0.01
     */
    private BigDecimal calcPayAmount(BigDecimal totalAmount, BigDecimal freightAmount, BigDecimal discountAmount) {
        BigDecimal pay = totalAmount.add(freightAmount).subtract(discountAmount);
        if (pay.compareTo(MIN_PAY_AMOUNT) < 0) {
            pay = MIN_PAY_AMOUNT;
        }
        return pay.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算优惠金额：满减券直减；折扣券按折扣率，最高优惠 100 元；均不超过商品总额
     */
    private BigDecimal calcDiscount(Coupon coupon, BigDecimal totalAmount) {
        if (coupon == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount;
        if (Integer.valueOf(1).equals(coupon.getType())) {
            discount = coupon.getAmount() == null ? BigDecimal.ZERO : coupon.getAmount();
        } else if (Integer.valueOf(2).equals(coupon.getType())) {
            BigDecimal rate = coupon.getDiscount() == null ? new BigDecimal("10") : coupon.getDiscount();
            if (rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(new BigDecimal("10")) >= 0) {
                return BigDecimal.ZERO;
            }
            discount = totalAmount.multiply(BigDecimal.ONE.subtract(
                    rate.divide(new BigDecimal("10"), 4, RoundingMode.HALF_UP)));
            if (discount.compareTo(MAX_DISCOUNT_AMOUNT) > 0) {
                discount = MAX_DISCOUNT_AMOUNT;
            }
        } else {
            return BigDecimal.ZERO;
        }
        if (discount.compareTo(totalAmount) > 0) {
            discount = totalAmount;
        }
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO;
        }
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 校验优惠券可用性：归属、状态、有效期、门槛、适用范围
     *
     * @return 用户券与其券模板
     */
    private CouponSelection requireAvailableCoupon(Long userId, Long userCouponId, BigDecimal totalAmount,
                                                   List<LineItem> items) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || !userCoupon.getUserId().equals(userId)) {
            throw new BusinessException("优惠券不存在或无权使用");
        }
        if (userCoupon.getStatus() == null || userCoupon.getStatus() != 0) {
            throw new BusinessException("该优惠券已使用或已过期");
        }
        if (userCoupon.getExpireTime() != null && userCoupon.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("该优惠券已过期");
        }
        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
        if (coupon == null) {
            throw new BusinessException("优惠券信息不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStatus() == null || coupon.getStatus() != 1) {
            throw new BusinessException("该优惠券已停用");
        }
        if (coupon.getStartTime() != null && coupon.getStartTime().isAfter(now)) {
            throw new BusinessException("该优惠券尚未生效");
        }
        if (coupon.getEndTime() != null && coupon.getEndTime().isBefore(now)) {
            throw new BusinessException("该优惠券已过期");
        }
        if (coupon.getMinAmount() != null && totalAmount.compareTo(coupon.getMinAmount()) < 0) {
            throw new BusinessException("订单金额未满 " + coupon.getMinAmount().stripTrailingZeros().toPlainString()
                    + " 元，无法使用该优惠券");
        }
        if (!scopeMatched(coupon, items)) {
            throw new BusinessException("该优惠券不适用于本次购买的商品");
        }
        return new CouponSelection(userCoupon, coupon);
    }

    /**
     * 优惠券适用范围校验：1全场、2指定分类（含一级分类）、3指定商品
     */
    private boolean scopeMatched(Coupon coupon, List<LineItem> items) {
        Integer scope = coupon.getScope();
        if (scope == null || scope == 1) {
            return true;
        }
        Set<Long> scopeIds = parseIds(coupon.getScopeIds());
        if (scopeIds.isEmpty()) {
            return true;
        }
        if (scope == 3) {
            return items.stream().anyMatch(item -> scopeIds.contains(item.getProduct().getId()));
        }
        if (scope == 2) {
            Set<Long> categoryIds = new LinkedHashSet<>();
            for (LineItem item : items) {
                Long categoryId = item.getProduct().getCategoryId();
                if (categoryId == null) {
                    continue;
                }
                categoryIds.add(categoryId);
                // 二级分类命中一级分类同样可用
                Category category = categoryMapper.selectById(categoryId);
                if (category != null && category.getParentId() != null && category.getParentId() > 0) {
                    categoryIds.add(category.getParentId());
                }
            }
            return categoryIds.stream().anyMatch(scopeIds::contains);
        }
        return false;
    }

    /**
     * 查询当前订单可用的优惠券
     */
    private List<UserCouponVO> availableCoupons(Long userId, BigDecimal totalAmount, List<LineItem> items) {
        List<UserCoupon> userCoupons = userCouponMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, 0)
                .gt(UserCoupon::getExpireTime, LocalDateTime.now())
                .orderByDesc(UserCoupon::getReceiveTime));
        if (userCoupons.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> couponIds = userCoupons.stream().map(UserCoupon::getCouponId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Coupon> couponMap = couponIds.isEmpty() ? Collections.emptyMap()
                : couponMapper.selectBatchIds(couponIds).stream()
                .collect(Collectors.toMap(Coupon::getId, c -> c, (a, b) -> a, HashMap::new));

        LocalDateTime now = LocalDateTime.now();
        List<UserCouponVO> result = new ArrayList<>();
        for (UserCoupon userCoupon : userCoupons) {
            Coupon coupon = couponMap.get(userCoupon.getCouponId());
            if (coupon == null || coupon.getStatus() == null || coupon.getStatus() != 1) {
                continue;
            }
            if (coupon.getStartTime() != null && coupon.getStartTime().isAfter(now)) {
                continue;
            }
            if (coupon.getEndTime() != null && coupon.getEndTime().isBefore(now)) {
                continue;
            }
            if (coupon.getMinAmount() != null && totalAmount.compareTo(coupon.getMinAmount()) < 0) {
                continue;
            }
            if (!scopeMatched(coupon, items)) {
                continue;
            }
            result.add(toUserCouponVO(userCoupon, coupon));
        }
        return result;
    }

    /**
     * 用户券 → 前端展示 VO
     */
    private UserCouponVO toUserCouponVO(UserCoupon userCoupon, Coupon coupon) {
        UserCouponVO vo = new UserCouponVO();
        vo.setId(userCoupon.getId());
        vo.setUserId(userCoupon.getUserId());
        vo.setCouponId(userCoupon.getCouponId());
        vo.setCouponName(userCoupon.getCouponName());
        vo.setStatus(userCoupon.getStatus());
        vo.setStatusName(couponStatusName(userCoupon.getStatus()));
        vo.setOrderId(userCoupon.getOrderId());
        vo.setReceiveTime(userCoupon.getReceiveTime());
        vo.setUseTime(userCoupon.getUseTime());
        vo.setExpireTime(userCoupon.getExpireTime());
        if (coupon != null) {
            vo.setType(coupon.getType());
            vo.setAmount(coupon.getAmount());
            vo.setDiscount(coupon.getDiscount());
            vo.setMinAmount(coupon.getMinAmount());
            vo.setDescription(coupon.getDescription());
        }
        return vo;
    }

    /**
     * 用户券状态名称：0未使用 1已使用 2已过期
     */
    private String couponStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "未使用";
            case 1 -> "已使用";
            case 2 -> "已过期";
            default -> "未知";
        };
    }

    /**
     * 原子扣减 SKU 库存并累加销量，同步商品总库存与销量
     */
    private void deductStock(LineItem item) {
        int qty = item.getQuantity();
        int rows = productSkuMapper.update(null, Wrappers.<ProductSku>lambdaUpdate()
                .setSql("stock = stock - " + qty)
                .setSql("sales = sales + " + qty)
                .eq(ProductSku::getId, item.getSku().getId())
                .ge(ProductSku::getStock, qty));
        if (rows == 0) {
            // 并发下库存被抢空，抛出异常让事务整体回滚
            throw new BusinessException("库存不足：" + item.getProduct().getName());
        }
        // 商品维度同步：总库存 -qty、累计销量 +qty（带库存下界保护）
        productMapper.update(null, Wrappers.<Product>lambdaUpdate()
                .setSql("stock = GREATEST(stock - " + qty + ", 0)")
                .setSql("sales = sales + " + qty)
                .eq(Product::getId, item.getProduct().getId()));
    }

    /**
     * 回补库存与销量（取消订单 / 退货入库）
     */
    private void restoreStock(Long skuId, Integer quantity) {
        if (skuId == null || quantity == null || quantity <= 0) {
            return;
        }
        ProductSku sku = productSkuMapper.selectById(skuId);
        productSkuMapper.update(null, Wrappers.<ProductSku>lambdaUpdate()
                .setSql("stock = stock + " + quantity)
                .setSql("sales = GREATEST(sales - " + quantity + ", 0)")
                .eq(ProductSku::getId, skuId));
        if (sku != null && sku.getProductId() != null) {
            productMapper.update(null, Wrappers.<Product>lambdaUpdate()
                    .setSql("stock = stock + " + quantity)
                    .setSql("sales = GREATEST(sales - " + quantity + ", 0)")
                    .eq(Product::getId, sku.getProductId()));
        }
    }

    /**
     * 核销优惠券并累加券的已使用数量
     */
    private void useCoupon(UserCoupon userCoupon, Long orderId) {
        userCouponMapper.update(null, Wrappers.<UserCoupon>lambdaUpdate()
                .set(UserCoupon::getStatus, 1)
                .set(UserCoupon::getOrderId, orderId)
                .set(UserCoupon::getUseTime, LocalDateTime.now())
                .eq(UserCoupon::getId, userCoupon.getId())
                .eq(UserCoupon::getStatus, 0));
        if (userCoupon.getCouponId() != null) {
            couponMapper.update(null, Wrappers.<Coupon>lambdaUpdate()
                    .setSql("used_count = used_count + 1")
                    .eq(Coupon::getId, userCoupon.getCouponId()));
        }
    }

    /**
     * 退还优惠券并回退券的已使用数量
     */
    private void returnCoupon(Long userCouponId) {
        if (userCouponId == null) {
            return;
        }
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || userCoupon.getStatus() == null || userCoupon.getStatus() != 1) {
            return;
        }
        userCouponMapper.update(null, Wrappers.<UserCoupon>lambdaUpdate()
                .set(UserCoupon::getStatus, 0)
                .set(UserCoupon::getOrderId, null)
                .set(UserCoupon::getUseTime, null)
                .eq(UserCoupon::getId, userCouponId));
        if (userCoupon.getCouponId() != null) {
            couponMapper.update(null, Wrappers.<Coupon>lambdaUpdate()
                    .setSql("used_count = GREATEST(used_count - 1, 0)")
                    .eq(Coupon::getId, userCoupon.getCouponId()));
        }
    }

    /**
     * 取消订单并回滚库存/销量/优惠券（买家取消与管理端关闭共用）
     */
    private void cancelInternal(Orders orders) {
        Orders update = new Orders();
        update.setId(orders.getId());
        update.setStatus(OrderStatus.CANCELLED.getCode());
        update.setCloseTime(LocalDateTime.now());
        ordersMapper.updateById(update);
        // 回补库存与销量
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, orders.getId()));
        for (OrderItem item : items) {
            restoreStock(item.getSkuId(), item.getQuantity());
        }
        // 退还优惠券
        returnCoupon(orders.getUserCouponId());
    }

    /**
     * 分页查询订单并填充明细、买家昵称、状态名称
     */
    private PageResult<OrderVO> pageOrders(LambdaQueryWrapper<Orders> wrapper, OrderQueryDTO query) {
        long current = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        long size = query.getSize() == null || query.getSize() < 1 ? 10 : query.getSize();
        wrapper.orderByDesc(Orders::getCreateTime);
        Page<Orders> page = ordersMapper.selectPage(new Page<>(current, size), wrapper);
        List<Orders> records = page.getRecords();

        Map<Long, List<OrderItem>> itemMap = loadOrderItems(records.stream()
                .map(Orders::getId).collect(Collectors.toList()));
        Map<Long, String> nicknameMap = nicknameMap(records.stream()
                .map(Orders::getUserId).collect(Collectors.toSet()));
        Set<Long> refundOrderIds = refundOrderIds(records.stream().map(Orders::getId).collect(Collectors.toList()));

        List<OrderVO> list = new ArrayList<>(records.size());
        for (Orders orders : records) {
            OrderVO vo = buildOrderVO(orders, itemMap.get(orders.getId()),
                    nicknameMap.get(orders.getUserId()), refundOrderIds.contains(orders.getId()));
            list.add(vo);
        }
        PageResult<OrderVO> result = new PageResult<>();
        result.setRecords(list);
        result.setTotal(page.getTotal());
        result.setPage(page.getCurrent());
        result.setSize(page.getSize());
        result.setPages((page.getTotal() + page.getSize() - 1) / page.getSize());
        return result;
    }

    /**
     * 构建订单基础 VO（列表与详情共用）
     */
    private OrderVO buildOrderVO(Orders orders, List<OrderItem> items, String buyerNickname, boolean hasRefund) {
        OrderDetailVO vo = new OrderDetailVO();
        BeanUtils.copyProperties(orders, vo);
        vo.setStatusName(OrderStatus.descOf(orders.getStatus()));
        vo.setBuyerNickname(buyerNickname);
        vo.setHasRefund(hasRefund);
        List<OrderItem> detailItems = items == null ? new ArrayList<>() : items;
        vo.setItems(detailItems.stream().map(this::toOrderItemVO).collect(Collectors.toList()));
        if (vo.getItemCount() == null) {
            vo.setItemCount(detailItems.stream().mapToInt(i -> i.getQuantity() == null ? 0 : i.getQuantity()).sum());
        }
        return vo;
    }

    /**
     * 构建订单详情 VO：基础信息 + 物流轨迹 + 售后工单
     */
    private OrderDetailVO toDetailVO(Orders orders) {
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, orders.getId())
                .orderByAsc(OrderItem::getId));
        Set<Long> refundOrderIds = refundOrderIds(Collections.singletonList(orders.getId()));
        String nickname = nicknameMap(new LinkedHashSet<>(Collections.singletonList(orders.getUserId())))
                .get(orders.getUserId());
        OrderVO base = buildOrderVO(orders, items, nickname, refundOrderIds.contains(orders.getId()));
        OrderDetailVO vo = new OrderDetailVO();
        BeanUtils.copyProperties(base, vo);

        List<ExpressTrack> tracks = expressTrackMapper.selectList(Wrappers.<ExpressTrack>lambdaQuery()
                .eq(ExpressTrack::getOrderId, orders.getId())
                .orderByDesc(ExpressTrack::getAcceptTime));
        vo.setTracks(tracks.stream().map(this::toTrackVO).collect(Collectors.toList()));

        List<Refund> refunds = refundMapper.selectList(Wrappers.<Refund>lambdaQuery()
                .eq(Refund::getOrderId, orders.getId())
                .orderByDesc(Refund::getCreateTime));
        vo.setRefunds(refunds.stream().map(refundService::toVO).collect(Collectors.toList()));
        return vo;
    }

    /**
     * 查询条件：订单号模糊、状态、下单时间区间、关键词（订单号或商品名）
     */
    private LambdaQueryWrapper<Orders> buildWrapper(OrderQueryDTO query) {
        LambdaQueryWrapper<Orders> wrapper = Wrappers.lambdaQuery();
        if (query == null) {
            return wrapper;
        }
        wrapper.like(StringUtils.hasText(query.getOrderNo()), Orders::getOrderNo, query.getOrderNo());
        wrapper.eq(query.getStatus() != null, Orders::getStatus, query.getStatus());
        LocalDateTime start = parseTime(query.getStartTime(), false);
        LocalDateTime end = parseTime(query.getEndTime(), true);
        wrapper.ge(start != null, Orders::getCreateTime, start);
        wrapper.le(end != null, Orders::getCreateTime, end);
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            // 关键词命中订单号或订单明细中的商品名称
            List<OrderItem> matched = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                    .select(OrderItem::getOrderId)
                    .like(OrderItem::getProductName, keyword));
            Set<Long> orderIds = matched.stream().map(OrderItem::getOrderId)
                    .filter(Objects::nonNull).collect(Collectors.toSet());
            wrapper.and(w -> {
                w.like(Orders::getOrderNo, keyword);
                if (!orderIds.isEmpty()) {
                    w.or().in(Orders::getId, orderIds);
                }
            });
        }
        return wrapper;
    }

    /**
     * 解析时间参数，支持 yyyy-MM-dd 与 yyyy-MM-dd HH:mm:ss
     *
     * @param endOfDay 仅日期时是否取当天 23:59:59
     */
    private LocalDateTime parseTime(String text, boolean endOfDay) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String value = text.trim();
        try {
            if (value.length() <= 10) {
                LocalDate date = LocalDate.parse(value);
                return endOfDay ? date.atTime(23, 59, 59) : date.atStartOfDay();
            }
            return LocalDateTime.parse(value.replace('T', ' '), EXPORT_DT_FMT);
        } catch (Exception e) {
            log.warn("时间参数解析失败，已忽略：{}", text);
            return null;
        }
    }

    /**
     * 批量加载订单明细
     */
    private Map<Long, List<OrderItem>> loadOrderItems(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .in(OrderItem::getOrderId, orderIds)
                .orderByAsc(OrderItem::getId));
        Map<Long, List<OrderItem>> map = new HashMap<>();
        for (OrderItem item : items) {
            map.computeIfAbsent(item.getOrderId(), k -> new ArrayList<>()).add(item);
        }
        return map;
    }

    /**
     * 批量加载买家昵称（昵称为空时回退账号）
     */
    private Map<Long, String> nicknameMap(Set<Long> userIds) {
        Set<Long> ids = userIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<User> users = userMapper.selectBatchIds(ids);
        Map<Long, String> map = new HashMap<>();
        for (User user : users) {
            map.put(user.getId(), StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        }
        return map;
    }

    /**
     * 查询这批订单中存在售后单的订单ID
     */
    private Set<Long> refundOrderIds(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<Refund> refunds = refundMapper.selectList(Wrappers.<Refund>lambdaQuery()
                .select(Refund::getOrderId)
                .in(Refund::getOrderId, orderIds));
        return refunds.stream().map(Refund::getOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /**
     * 生成订单号：MO + yyyyMMddHHmmss + 4 位随机数，重复则重试
     */
    private String generateOrderNo() {
        for (int i = 0; i < 10; i++) {
            String orderNo = "MO" + LocalDateTime.now().format(ORDER_NO_FMT)
                    + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
            Long count = ordersMapper.selectCount(Wrappers.<Orders>lambdaQuery()
                    .eq(Orders::getOrderNo, orderNo));
            if (count == null || count == 0) {
                return orderNo;
            }
        }
        throw new BusinessException("订单号生成失败，请稍后重试");
    }

    /**
     * 写入物流轨迹
     */
    private void insertTrack(Orders orders, String company, String no, String status,
                             String context, String location, LocalDateTime acceptTime) {
        ExpressTrack track = new ExpressTrack();
        track.setOrderId(orders.getId());
        track.setOrderNo(orders.getOrderNo());
        track.setExpressCompany(company);
        track.setExpressNo(no);
        track.setStatus(status);
        track.setContext(context);
        track.setLocation(location);
        track.setAcceptTime(acceptTime);
        expressTrackMapper.insert(track);
    }

    /**
     * 自动关闭超时未支付订单
     *
     * <p>阈值优先读取系统配置 order_auto_cancel_minutes，其次 application.yml 的 mall.shop.order-auto-cancel-minutes。</p>
     */
    private void autoCancelExpiredOrders(Long userId) {
        Integer minutes = configInteger(SysConfigKeys.ORDER_AUTO_CANCEL_MINUTES, orderAutoCancelMinutes);
        if (minutes == null || minutes <= 0) {
            return;
        }
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(minutes);
        List<Orders> expired = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getUserId, userId)
                .eq(Orders::getStatus, OrderStatus.UNPAID.getCode())
                .lt(Orders::getCreateTime, deadline));
        for (Orders orders : expired) {
            cancelInternal(orders);
            log.info("订单超时未支付自动取消：orderNo={}", orders.getOrderNo());
        }
    }

    /**
     * 查询订单并校验归属（买家操作）
     */
    private Orders requireOwnOrder(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            throw new BusinessException("订单号不能为空");
        }
        Long userId = SecurityUtils.getUserId();
        Orders orders = ordersMapper.selectOne(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getOrderNo, orderNo)
                .eq(Orders::getUserId, userId));
        if (orders == null) {
            throw new BusinessException("订单不存在或无权操作");
        }
        return orders;
    }

    /**
     * 按主键查询订单（管理端操作）
     */
    private Orders requireOrder(Long id) {
        Orders orders = id == null ? null : ordersMapper.selectById(id);
        if (orders == null) {
            throw new BusinessException("订单不存在");
        }
        return orders;
    }

    private long countStatus(List<Orders> list, Integer status) {
        return list.stream().filter(o -> status.equals(o.getStatus())).count();
    }

    private Set<Long> parseIds(String ids) {
        if (!StringUtils.hasText(ids)) {
            return Collections.emptySet();
        }
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : time.format(EXPORT_DT_FMT);
    }

    private OrderItemVO toOrderItemVO(OrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        BeanUtils.copyProperties(item, vo);
        return vo;
    }

    private ExpressTrackVO toTrackVO(ExpressTrack track) {
        ExpressTrackVO vo = new ExpressTrackVO();
        BeanUtils.copyProperties(track, vo);
        return vo;
    }

    /**
     * 详情/预览用的购物车项 VO
     */
    private CartVO toPreviewItem(LineItem item) {
        CartVO vo = new CartVO();
        vo.setProductId(item.getProduct().getId());
        vo.setSkuId(item.getSku().getId());
        vo.setProductName(item.getProduct().getName());
        vo.setProductImage(StringUtils.hasText(item.getSku().getImage())
                ? item.getSku().getImage() : item.getProduct().getMainImage());
        vo.setSpecInfo(item.getSku().getSpecInfo());
        vo.setPrice(item.getSku().getPrice());
        vo.setQuantity(item.getQuantity());
        vo.setStock(item.getSku().getStock());
        vo.setChecked(1);
        vo.setStatus(1);
        vo.setTotalAmount(item.totalAmount());
        return vo;
    }

    /**
     * 默认收货地址：优先默认地址，其次最新一条
     */
    private AddressVO defaultAddress(Long userId) {
        List<UserAddress> list = userAddressMapper.selectList(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getCreateTime)
                .last("limit 1"));
        if (list.isEmpty()) {
            return null;
        }
        UserAddress address = list.get(0);
        AddressVO vo = new AddressVO();
        BeanUtils.copyProperties(address, vo);
        vo.setFullAddress(AddressServiceImpl.buildFullAddress(address.getProvince(), address.getCity(),
                address.getDistrict(), address.getDetail()));
        return vo;
    }

    /**
     * 下单商品明细（含商品与 SKU 实体）
     */
    @Getter
    @AllArgsConstructor
    private static class LineItem {
        /** 商品 */
        private final Product product;

        /** SKU */
        private final ProductSku sku;

        /** 购买数量 */
        private final Integer quantity;

        /** 小计 = 单价 × 数量 */
        BigDecimal totalAmount() {
            BigDecimal price = sku.getPrice() == null ? BigDecimal.ZERO : sku.getPrice();
            return price.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        }
    }

    /**
     * 优惠券选择结果：用户券 + 券模板
     */
    @Getter
    @AllArgsConstructor
    private static class CouponSelection {

        /** 用户券 */
        private final UserCoupon userCoupon;

        /** 券模板（含类型、金额/折扣、门槛与适用范围） */
        private final Coupon coupon;
    }
}
