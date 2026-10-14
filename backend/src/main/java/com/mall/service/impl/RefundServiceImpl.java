package com.mall.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.BusinessException;
import com.mall.common.PageResult;
import com.mall.common.enums.OrderStatus;
import com.mall.common.enums.RefundStatus;
import com.mall.dto.RefundApplyDTO;
import com.mall.dto.RefundAuditDTO;
import com.mall.entity.OrderItem;
import com.mall.entity.Orders;
import com.mall.entity.Product;
import com.mall.entity.ProductSku;
import com.mall.entity.Refund;
import com.mall.entity.User;
import com.mall.mapper.OrderItemMapper;
import com.mall.mapper.OrdersMapper;
import com.mall.mapper.ProductMapper;
import com.mall.mapper.ProductSkuMapper;
import com.mall.mapper.RefundMapper;
import com.mall.mapper.UserMapper;
import com.mall.security.LoginUser;
import com.mall.security.SecurityUtils;
import com.mall.service.RefundService;
import com.mall.vo.RefundVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 售后服务实现
 *
 * <p>售后状态机：</p>
 * <pre>
 *   0待审核 --同意(type=1)--> 4退款完成
 *   0待审核 --同意(type=2/3)--> 3待买家退货 --退款完成--> 4退款完成
 *   0待审核 --拒绝--> 2已拒绝（订单状态回退）
 *   0待审核 --买家撤销--> 5已撤销（订单状态回退）
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    /** 允许申请的售后类型 */
    private static final Set<Integer> ALLOWED_TYPES = Set.of(1, 2, 3);
    /** 视为进行中的售后状态：待审核 / 已同意 / 待买家退货 */
    private static final List<Integer> ACTIVE_STATUS = List.of(
            RefundStatus.PENDING.getCode(), RefundStatus.AGREED.getCode(), RefundStatus.WAIT_RETURN.getCode());

    private final RefundMapper refundMapper;
    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductMapper productMapper;

    /* ==================================================================
     *  一、买家端
     * ================================================================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long apply(RefundApplyDTO dto) {
        Long userId = SecurityUtils.getUserId();
        // 1. 订单归属与状态校验
        Orders orders = ordersMapper.selectById(dto.getOrderId());
        if (orders == null || !orders.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在或无权操作");
        }
        if (!OrderStatus.canApplyAfterSale(orders.getStatus())) {
            throw new BusinessException("当前订单状态（" + OrderStatus.descOf(orders.getStatus()) + "）不支持申请售后");
        }
        // 2. 售后类型合法
        if (dto.getType() == null || !ALLOWED_TYPES.contains(dto.getType())) {
            throw new BusinessException("售后类型不合法");
        }
        // 3. 防止重复申请：同订单（整单售后）或同明细不可存在进行中的售后
        List<Refund> exists = refundMapper.selectList(Wrappers.<Refund>lambdaQuery()
                .eq(Refund::getOrderId, orders.getId())
                .in(Refund::getStatus, ACTIVE_STATUS));
        Long orderItemId = dto.getOrderItemId();
        boolean duplicated = exists.stream().anyMatch(r ->
                (orderItemId == null && r.getOrderItemId() == null)
                        || (orderItemId != null && orderItemId.equals(r.getOrderItemId())));
        if (duplicated) {
            throw new BusinessException("该订单或该商品已存在进行中的售后申请，请勿重复提交");
        }

        // 4. 明细校验与退款金额
        OrderItem orderItem = null;
        if (orderItemId != null) {
            orderItem = orderItemMapper.selectById(orderItemId);
            if (orderItem == null || !orderItem.getOrderId().equals(orders.getId())) {
                throw new BusinessException("订单商品不存在");
            }
        }
        BigDecimal refundAmount = dto.getRefundAmount();
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            // 默认：单品取该明细小计，整单取订单实付
            refundAmount = orderItem != null
                    ? (orderItem.getTotalAmount() == null ? BigDecimal.ZERO : orderItem.getTotalAmount())
                    : (orders.getPayAmount() == null ? BigDecimal.ZERO : orders.getPayAmount());
        }
        refundAmount = refundAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal maxAmount = orders.getPayAmount() == null ? BigDecimal.ZERO : orders.getPayAmount();
        if (refundAmount.compareTo(maxAmount) > 0) {
            throw new BusinessException("退款金额不能超过订单实付金额 " + maxAmount.toPlainString() + " 元");
        }

        // 5. 写售后工单
        Refund refund = new Refund();
        refund.setRefundNo(generateRefundNo());
        refund.setOrderId(orders.getId());
        refund.setOrderNo(orders.getOrderNo());
        refund.setOrderItemId(orderItemId);
        refund.setUserId(userId);
        refund.setType(dto.getType());
        refund.setReason(dto.getReason());
        refund.setDescription(dto.getDescription());
        refund.setImages(dto.getImages() == null || dto.getImages().isEmpty()
                ? null : String.join(",", dto.getImages()));
        refund.setRefundAmount(refundAmount);
        refund.setStatus(RefundStatus.PENDING.getCode());
        refundMapper.insert(refund);

        // 6. 订单置为售后中
        updateOrderStatus(orders.getId(), OrderStatus.AFTER_SALE.getCode());
        log.info("买家申请售后：refundNo={}, orderNo={}, type={}, amount={}",
                refund.getRefundNo(), orders.getOrderNo(), dto.getType(), refundAmount);
        return refund.getId();
    }

    @Override
    public PageResult<RefundVO> pageBuyerRefunds(Integer status, Integer page, Integer size) {
        Long userId = SecurityUtils.getUserId();
        Page<Refund> result = refundMapper.selectPage(buildPage(page, size), Wrappers.<Refund>lambdaQuery()
                .eq(Refund::getUserId, userId)
                .eq(status != null, Refund::getStatus, status)
                .orderByDesc(Refund::getCreateTime));
        return toPageResult(result);
    }

    @Override
    public RefundVO getBuyerDetail(Long id) {
        Refund refund = refundMapper.selectById(id);
        if (refund == null || !refund.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException("售后单不存在或无权查看");
        }
        return toVO(refund);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        Refund refund = requireOwnRefund(id);
        if (!RefundStatus.PENDING.getCode().equals(refund.getStatus())) {
            throw new BusinessException("仅待审核的售后单可以撤销");
        }
        Refund update = new Refund();
        update.setId(refund.getId());
        update.setStatus(RefundStatus.CANCELLED.getCode());
        refundMapper.updateById(update);
        // 订单状态回退：已收货回到已完成，否则回到待收货
        Orders orders = ordersMapper.selectById(refund.getOrderId());
        if (orders != null && OrderStatus.AFTER_SALE.getCode().equals(orders.getStatus())) {
            updateOrderStatus(orders.getId(), orders.getReceiveTime() != null
                    ? OrderStatus.FINISHED.getCode() : OrderStatus.SHIPPED.getCode());
        }
    }

    /* ==================================================================
     *  二、管理端
     * ================================================================== */

    @Override
    public PageResult<RefundVO> pageAdminRefunds(String keyword, Integer status, Integer page, Integer size) {
        final Set<Long> matchedOrderIds;
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            // 关键词同时匹配售后单号 / 订单号，订单号通过订单表反查订单ID
            List<Orders> matchedOrders = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                    .select(Orders::getId)
                    .like(Orders::getOrderNo, kw));
            matchedOrderIds = matchedOrders.stream().map(Orders::getId).collect(Collectors.toSet());
        } else {
            matchedOrderIds = Collections.emptySet();
        }
        Page<Refund> result = refundMapper.selectPage(buildPage(page, size), Wrappers.<Refund>lambdaQuery()
                .eq(status != null, Refund::getStatus, status)
                .and(StringUtils.hasText(keyword), w -> {
                    w.like(Refund::getRefundNo, keyword.trim());
                    if (!matchedOrderIds.isEmpty()) {
                        w.or().in(Refund::getOrderId, matchedOrderIds);
                    }
                })
                .orderByDesc(Refund::getCreateTime));
        return toPageResult(result);
    }

    @Override
    public RefundVO getAdminDetail(Long id) {
        Refund refund = refundMapper.selectById(id);
        if (refund == null) {
            throw new BusinessException("售后单不存在");
        }
        return toVO(refund);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, RefundAuditDTO dto) {
        Refund refund = refundMapper.selectById(id);
        if (refund == null) {
            throw new BusinessException("售后单不存在");
        }
        Integer target = dto.getStatus();
        if (target == null || (target != 1 && target != 2 && target != 4)) {
            throw new BusinessException("审核结果不合法");
        }
        Integer current = refund.getStatus();
        // 状态机校验
        if (target == 1 && !RefundStatus.PENDING.getCode().equals(current)) {
            throw new BusinessException("仅待审核的售后单可以审核同意");
        }
        if (target == 2 && !(RefundStatus.PENDING.getCode().equals(current)
                || RefundStatus.WAIT_RETURN.getCode().equals(current))) {
            throw new BusinessException("当前售后状态不可拒绝");
        }
        if (target == 4 && !(RefundStatus.PENDING.getCode().equals(current)
                || RefundStatus.AGREED.getCode().equals(current)
                || RefundStatus.WAIT_RETURN.getCode().equals(current))) {
            throw new BusinessException("当前售后状态不可操作退款完成");
        }
        Orders orders = ordersMapper.selectById(refund.getOrderId());
        if (orders == null) {
            throw new BusinessException("订单不存在");
        }

        // 审核信息
        LoginUser loginUser = SecurityUtils.getLoginUser();
        Refund update = new Refund();
        update.setId(refund.getId());
        update.setAuditUserId(loginUser.getUserId());
        update.setAuditUser(StringUtils.hasText(loginUser.getNickname())
                ? loginUser.getNickname() : loginUser.getUsername());
        update.setAuditRemark(dto.getAuditRemark());
        update.setAuditTime(LocalDateTime.now());
        if (dto.getRefundAmount() != null
                && dto.getRefundAmount().compareTo(BigDecimal.ZERO) > 0) {
            update.setRefundAmount(dto.getRefundAmount().setScale(2, RoundingMode.HALF_UP));
        }
        BigDecimal refundAmount = update.getRefundAmount() != null
                ? update.getRefundAmount() : refund.getRefundAmount();

        if (target == 1) {
            // 同意：仅退款直接完成；退货退款/换货进入待买家退货
            if (Integer.valueOf(1).equals(refund.getType())) {
                update.setStatus(RefundStatus.FINISHED.getCode());
                update.setFinishTime(LocalDateTime.now());
                refundMapper.updateById(update);
                // 仅退款：按退款金额决定订单最终状态
                settleOrderAfterRefund(orders, refundAmount);
            } else {
                update.setStatus(RefundStatus.WAIT_RETURN.getCode());
                refundMapper.updateById(update);
            }
        } else if (target == 2) {
            // 拒绝：订单从售后中回退
            update.setStatus(RefundStatus.REJECTED.getCode());
            refundMapper.updateById(update);
            if (OrderStatus.AFTER_SALE.getCode().equals(orders.getStatus())) {
                updateOrderStatus(orders.getId(), orders.getReceiveTime() != null
                        ? OrderStatus.FINISHED.getCode() : OrderStatus.SHIPPED.getCode());
            }
        } else {
            // 退款完成：退货入库，回补库存与销量
            update.setStatus(RefundStatus.FINISHED.getCode());
            update.setFinishTime(LocalDateTime.now());
            refundMapper.updateById(update);
            restoreStockForRefund(refund);
            settleOrderAfterRefund(orders, refundAmount);
        }
        log.info("售后审核完成：refundNo={}, status={}, auditUser={}",
                refund.getRefundNo(), target, update.getAuditUser());
    }

    /* ==================================================================
     *  三、内部方法
     * ================================================================== */

    /**
     * 退款完成后的订单状态：退款金额达到订单实付则订单取消，否则订单完成
     */
    private void settleOrderAfterRefund(Orders orders, BigDecimal refundAmount) {
        BigDecimal payAmount = orders.getPayAmount() == null ? BigDecimal.ZERO : orders.getPayAmount();
        Integer finalStatus = (refundAmount != null && refundAmount.compareTo(payAmount) >= 0)
                ? OrderStatus.CANCELLED.getCode() : OrderStatus.FINISHED.getCode();
        Orders update = new Orders();
        update.setId(orders.getId());
        update.setStatus(finalStatus);
        if (OrderStatus.CANCELLED.getCode().equals(finalStatus)) {
            update.setCloseTime(LocalDateTime.now());
        } else {
            update.setFinishTime(LocalDateTime.now());
        }
        ordersMapper.updateById(update);
    }

    /**
     * 退货入库：回补售后单对应明细的 SKU/商品库存与销量；整单售后回补全部明细
     */
    private void restoreStockForRefund(Refund refund) {
        List<OrderItem> items;
        if (refund.getOrderItemId() != null) {
            OrderItem item = orderItemMapper.selectById(refund.getOrderItemId());
            items = item == null ? new ArrayList<>() : List.of(item);
        } else {
            items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                    .eq(OrderItem::getOrderId, refund.getOrderId()));
        }
        for (OrderItem item : items) {
            int qty = item.getQuantity() == null ? 0 : item.getQuantity();
            if (qty <= 0 || item.getSkuId() == null) {
                continue;
            }
            ProductSku sku = productSkuMapper.selectById(item.getSkuId());
            productSkuMapper.update(null, Wrappers.<ProductSku>lambdaUpdate()
                    .setSql("stock = stock + " + qty)
                    .setSql("sales = GREATEST(sales - " + qty + ", 0)")
                    .eq(ProductSku::getId, item.getSkuId()));
            if (sku != null && sku.getProductId() != null) {
                productMapper.update(null, Wrappers.<Product>lambdaUpdate()
                        .setSql("stock = stock + " + qty)
                        .setSql("sales = GREATEST(sales - " + qty + ", 0)")
                        .eq(Product::getId, sku.getProductId()));
            }
        }
    }

    /**
     * 更新订单状态（仅在读到的状态需要变更时调用）
     */
    private void updateOrderStatus(Long orderId, Integer status) {
        Orders update = new Orders();
        update.setId(orderId);
        update.setStatus(status);
        ordersMapper.updateById(update);
    }

    /**
     * 查询售后单并校验归属（买家）
     */
    private Refund requireOwnRefund(Long id) {
        Refund refund = id == null ? null : refundMapper.selectById(id);
        if (refund == null || !refund.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException("售后单不存在或无权操作");
        }
        return refund;
    }

    /**
     * 生成售后单号：RF + yyyyMMddHHmmss + 4 位随机数，重复则重试
     */
    private String generateRefundNo() {
        for (int i = 0; i < 10; i++) {
            String refundNo = "RF" + LocalDateTime.now().format(NO_FMT)
                    + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
            Long count = refundMapper.selectCount(Wrappers.<Refund>lambdaQuery()
                    .eq(Refund::getRefundNo, refundNo));
            if (count == null || count == 0) {
                return refundNo;
            }
        }
        throw new BusinessException("售后单号生成失败，请稍后重试");
    }

    private Page<Refund> buildPage(Integer page, Integer size) {
        long current = page == null || page < 1 ? 1 : page;
        long pageSize = size == null || size < 1 ? 10 : size;
        return new Page<>(current, pageSize);
    }

    /**
     * 分页结果转换：批量填充买家昵称与商品信息，避免 N+1
     */
    private PageResult<RefundVO> toPageResult(Page<Refund> page) {
        List<Refund> records = page.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(page, refund -> convert(refund, null, null));
        }
        Map<Long, String> nicknameMap = loadNicknames(records.stream()
                .map(Refund::getUserId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, OrderItem> itemMap = loadOrderItems(records);
        Map<Long, OrderItem> firstItemMap = loadFirstOrderItems(records);
        return PageResult.of(page, refund -> convert(refund, nicknameMap.get(refund.getUserId()),
                resolveItem(refund, itemMap, firstItemMap)));
    }

    /**
     * 售后单 → VO（单条查询使用）
     */
    @Override
    public RefundVO toVO(Refund refund) {
        OrderItem item = null;
        if (refund.getOrderItemId() != null) {
            item = orderItemMapper.selectById(refund.getOrderItemId());
        }
        if (item == null) {
            List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                    .eq(OrderItem::getOrderId, refund.getOrderId())
                    .orderByAsc(OrderItem::getId)
                    .last("limit 1"));
            item = items.isEmpty() ? null : items.get(0);
        }
        String nickname = loadNicknames(Collections.singleton(refund.getUserId())).get(refund.getUserId());
        return convert(refund, nickname, item);
    }

    /**
     * 组装售后 VO
     */
    private RefundVO convert(Refund refund, String buyerNickname, OrderItem item) {
        RefundVO vo = new RefundVO();
        vo.setId(refund.getId());
        vo.setRefundNo(refund.getRefundNo());
        vo.setOrderId(refund.getOrderId());
        vo.setOrderNo(refund.getOrderNo());
        vo.setOrderItemId(refund.getOrderItemId());
        vo.setUserId(refund.getUserId());
        vo.setBuyerNickname(buyerNickname);
        vo.setType(refund.getType());
        vo.setTypeName(typeName(refund.getType()));
        vo.setReason(refund.getReason());
        vo.setDescription(refund.getDescription());
        vo.setImages(parseImages(refund.getImages()));
        vo.setRefundAmount(refund.getRefundAmount());
        vo.setStatus(refund.getStatus());
        vo.setStatusName(RefundStatus.descOf(refund.getStatus()));
        vo.setAuditUser(refund.getAuditUser());
        vo.setAuditRemark(refund.getAuditRemark());
        vo.setAuditTime(refund.getAuditTime());
        vo.setFinishTime(refund.getFinishTime());
        vo.setCreateTime(refund.getCreateTime());
        if (item != null) {
            vo.setProductName(item.getProductName());
            vo.setProductImage(item.getProductImage());
        }
        return vo;
    }

    private String typeName(Integer type) {
        if (type == null) {
            return "未知";
        }
        return switch (type) {
            case 1 -> "仅退款";
            case 2 -> "退货退款";
            case 3 -> "换货";
            default -> "未知";
        };
    }

    private List<String> parseImages(String images) {
        if (!StringUtils.hasText(images)) {
            return new ArrayList<>();
        }
        return Arrays.stream(images.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private Map<Long, String> loadNicknames(Set<Long> userIds) {
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
     * 批量加载售后单指定的明细
     */
    private Map<Long, OrderItem> loadOrderItems(List<Refund> records) {
        Set<Long> itemIds = records.stream().map(Refund::getOrderItemId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (itemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return orderItemMapper.selectBatchIds(itemIds).stream()
                .collect(Collectors.toMap(OrderItem::getId, i -> i, (a, b) -> a, HashMap::new));
    }

    /**
     * 批量加载整单售后的首条明细（用于展示商品名/图）
     */
    private Map<Long, OrderItem> loadFirstOrderItems(List<Refund> records) {
        Set<Long> orderIds = records.stream()
                .filter(r -> r.getOrderItemId() == null)
                .map(Refund::getOrderId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .in(OrderItem::getOrderId, orderIds)
                .orderByAsc(OrderItem::getId));
        Map<Long, OrderItem> map = new HashMap<>();
        for (OrderItem item : items) {
            map.putIfAbsent(item.getOrderId(), item);
        }
        return map;
    }

    private OrderItem resolveItem(Refund refund, Map<Long, OrderItem> itemMap, Map<Long, OrderItem> firstItemMap) {
        if (refund.getOrderItemId() != null) {
            return itemMap.get(refund.getOrderItemId());
        }
        return firstItemMap.get(refund.getOrderId());
    }
}
