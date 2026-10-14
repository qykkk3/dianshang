package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.BusinessException;
import com.mall.common.PageResult;
import com.mall.common.enums.OrderStatus;
import com.mall.dto.CommentAddDTO;
import com.mall.dto.CommentQueryDTO;
import com.mall.entity.OrderItem;
import com.mall.entity.Orders;
import com.mall.entity.Product;
import com.mall.entity.ProductComment;
import com.mall.entity.ProductSku;
import com.mall.entity.User;
import com.mall.mapper.OrderItemMapper;
import com.mall.mapper.OrdersMapper;
import com.mall.mapper.ProductCommentMapper;
import com.mall.mapper.ProductMapper;
import com.mall.mapper.ProductSkuMapper;
import com.mall.mapper.UserMapper;
import com.mall.security.LoginUser;
import com.mall.security.SecurityUtils;
import com.mall.service.CommentService;
import com.mall.vo.CommentStatsVO;
import com.mall.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商品评价服务实现
 *
 * <p>评价列表只展示 status=1 的记录；匿名评价对外统一显示「匿名用户」与默认头像。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    /** 匿名评价展示昵称 */
    private static final String ANONYMOUS_NICKNAME = "匿名用户";
    /** 匿名评价展示头像 */
    private static final String ANONYMOUS_AVATAR = "/api/files/avatar/u0.svg";

    /** 评价状态：显示 */
    private static final int STATUS_SHOW = 1;
    /** 评价状态：隐藏 */
    private static final int STATUS_HIDE = 0;
    /** 未评价 */
    private static final int NOT_COMMENTED = 0;
    /** 已评价 */
    private static final int COMMENTED = 1;

    private final ProductCommentMapper productCommentMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrdersMapper ordersMapper;
    private final UserMapper userMapper;

    @Override
    public PageResult<CommentVO> pageProductComments(Long productId, Integer star, Integer page, Integer size) {
        BusinessException.throwIf(productId == null, "缺少商品ID");
        Page<ProductComment> pageParam = buildPage(page, size);
        IPage<ProductComment> result = productCommentMapper.selectPage(pageParam, Wrappers.<ProductComment>lambdaQuery()
                .eq(ProductComment::getProductId, productId)
                .eq(ProductComment::getStatus, STATUS_SHOW)
                .eq(star != null, ProductComment::getStar, star)
                .orderByDesc(ProductComment::getCreateTime)
                .orderByDesc(ProductComment::getId));
        Map<Long, Product> productMap = loadProducts(result.getRecords());
        Map<Long, ProductSku> skuMap = loadSkus(result.getRecords());
        return PageResult.of(result, comment -> toCommentVO(comment, productMap, skuMap, true));
    }

    @Override
    public CommentStatsVO getProductCommentStats(Long productId) {
        BusinessException.throwIf(productId == null, "缺少商品ID");
        List<ProductComment> comments = productCommentMapper.selectList(Wrappers.<ProductComment>lambdaQuery()
                .select(ProductComment::getStar)
                .eq(ProductComment::getProductId, productId)
                .eq(ProductComment::getStatus, STATUS_SHOW));

        Map<Integer, Long> starCount = new LinkedHashMap<>();
        for (int star = 1; star <= 5; star++) {
            starCount.put(star, 0L);
        }
        long total = 0L;
        long sum = 0L;
        for (ProductComment comment : comments) {
            Integer star = comment.getStar();
            if (star == null || star < 1 || star > 5) {
                continue;
            }
            starCount.merge(star, 1L, Long::sum);
            total++;
            sum += star;
        }
        CommentStatsVO vo = new CommentStatsVO();
        vo.setTotal(total);
        vo.setStarCount(starCount);
        // 平均分保留 1 位小数
        vo.setAvgStar(total == 0 ? 0.0 : Math.round(sum * 10.0 / total) / 10.0);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addComment(CommentAddDTO dto) {
        Long userId = SecurityUtils.getUserId();
        OrderItem orderItem = orderItemMapper.selectById(dto.getOrderItemId());
        BusinessException.throwIf(orderItem == null, "订单明细不存在");
        BusinessException.throwIf(!userId.equals(orderItem.getUserId()), "无权评价该订单商品");

        Orders order = ordersMapper.selectById(orderItem.getOrderId());
        BusinessException.throwIf(order == null, "订单不存在");
        BusinessException.throwIf(!OrderStatus.FINISHED.getCode().equals(order.getStatus()), "订单完成后才能评价");
        BusinessException.throwIf(Integer.valueOf(COMMENTED).equals(orderItem.getIsCommented()), "该商品已评价，请勿重复提交");

        User user = userMapper.selectById(userId);
        ProductComment comment = new ProductComment();
        comment.setProductId(orderItem.getProductId());
        comment.setSkuId(orderItem.getSkuId());
        comment.setOrderId(orderItem.getOrderId());
        comment.setOrderItemId(orderItem.getId());
        comment.setUserId(userId);
        // 昵称 / 头像快照
        comment.setUserNickname(user == null ? null : user.getNickname());
        comment.setUserAvatar(user == null ? null : user.getAvatar());
        comment.setStar(dto.getStar());
        comment.setContent(dto.getContent());
        comment.setImages(joinImages(dto.getImages()));
        comment.setIsAnonymous(dto.getIsAnonymous() == null ? 0 : dto.getIsAnonymous());
        comment.setStatus(STATUS_SHOW);
        productCommentMapper.insert(comment);

        // 标记订单明细已评价
        orderItemMapper.update(null, Wrappers.<OrderItem>lambdaUpdate()
                .eq(OrderItem::getId, orderItem.getId())
                .set(OrderItem::getIsCommented, COMMENTED));
    }

    @Override
    public PageResult<CommentVO> pageMyComments(Integer page, Integer size) {
        Long userId = SecurityUtils.getUserId();
        Page<ProductComment> pageParam = buildPage(page, size);
        IPage<ProductComment> result = productCommentMapper.selectPage(pageParam, Wrappers.<ProductComment>lambdaQuery()
                .eq(ProductComment::getUserId, userId)
                .orderByDesc(ProductComment::getCreateTime)
                .orderByDesc(ProductComment::getId));
        Map<Long, Product> productMap = loadProducts(result.getRecords());
        Map<Long, ProductSku> skuMap = loadSkus(result.getRecords());
        // 自己的评价不做匿名脱敏，便于用户回看
        return PageResult.of(result, comment -> toCommentVO(comment, productMap, skuMap, false));
    }

    @Override
    public PageResult<Map<String, Object>> pagePendingItems(Integer page, Integer size) {
        Long userId = SecurityUtils.getUserId();
        // 已完成订单
        List<Orders> finishedOrders = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                .select(Orders::getId, Orders::getOrderNo, Orders::getFinishTime)
                .eq(Orders::getUserId, userId)
                .eq(Orders::getStatus, OrderStatus.FINISHED.getCode()));
        if (finishedOrders.isEmpty()) {
            return PageResult.empty();
        }
        Map<Long, Orders> orderMap = new LinkedHashMap<>();
        finishedOrders.forEach(order -> orderMap.put(order.getId(), order));

        Page<OrderItem> pageParam = buildPage(page, size);
        IPage<OrderItem> result = orderItemMapper.selectPage(pageParam, Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getUserId, userId)
                .eq(OrderItem::getIsCommented, NOT_COMMENTED)
                .in(OrderItem::getOrderId, orderMap.keySet())
                .orderByDesc(OrderItem::getCreateTime)
                .orderByDesc(OrderItem::getId));

        List<Map<String, Object>> records = new ArrayList<>(result.getRecords().size());
        for (OrderItem item : result.getRecords()) {
            Orders order = orderMap.get(item.getOrderId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orderItemId", item.getId());
            row.put("orderNo", item.getOrderNo());
            row.put("productId", item.getProductId());
            row.put("productName", item.getProductName());
            row.put("productImage", item.getProductImage());
            row.put("specInfo", item.getSpecInfo());
            row.put("price", item.getPrice());
            row.put("quantity", item.getQuantity());
            row.put("orderId", item.getOrderId());
            row.put("finishTime", order == null ? null : order.getFinishTime());
            records.add(row);
        }
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public PageResult<CommentVO> pageAdminComments(CommentQueryDTO query) {
        CommentQueryDTO condition = query == null ? new CommentQueryDTO() : query;
        Page<ProductComment> pageParam = buildPage(condition.getPage(), condition.getSize());
        LambdaQueryWrapper<ProductComment> wrapper = Wrappers.<ProductComment>lambdaQuery()
                .eq(condition.getProductId() != null, ProductComment::getProductId, condition.getProductId())
                .eq(condition.getUserId() != null, ProductComment::getUserId, condition.getUserId())
                .eq(condition.getStar() != null, ProductComment::getStar, condition.getStar())
                .eq(condition.getStatus() != null, ProductComment::getStatus, condition.getStatus());

        String keyword = condition.getKeyword() == null ? null : condition.getKeyword().trim();
        if (keyword != null && !keyword.isEmpty()) {
            // 关键字同时匹配评价内容与商品名称
            List<Long> productIds = listProductIdsByName(keyword);
            wrapper.and(w -> {
                w.like(ProductComment::getContent, keyword);
                if (!productIds.isEmpty()) {
                    w.or().in(ProductComment::getProductId, productIds);
                }
            });
        }
        wrapper.orderByDesc(ProductComment::getCreateTime).orderByDesc(ProductComment::getId);

        IPage<ProductComment> result = productCommentMapper.selectPage(pageParam, wrapper);
        Map<Long, Product> productMap = loadProducts(result.getRecords());
        Map<Long, ProductSku> skuMap = loadSkus(result.getRecords());
        return PageResult.of(result, comment -> toCommentVO(comment, productMap, skuMap, false));
    }

    @Override
    public void reply(Long id, String replyContent) {
        ProductComment comment = requireComment(id);
        LoginUser loginUser = SecurityUtils.getLoginUser();
        String replyUser = loginUser.getNickname() == null || loginUser.getNickname().isBlank()
                ? loginUser.getUsername() : loginUser.getNickname();
        productCommentMapper.update(null, Wrappers.<ProductComment>lambdaUpdate()
                .eq(ProductComment::getId, comment.getId())
                .set(ProductComment::getReplyContent, replyContent)
                .set(ProductComment::getReplyUser, replyUser)
                .set(ProductComment::getReplyTime, LocalDateTime.now()));
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        BusinessException.throwIf(status == null
                || (status != STATUS_SHOW && status != STATUS_HIDE), "评价状态只能为 1显示 或 0隐藏");
        ProductComment comment = requireComment(id);
        productCommentMapper.update(null, Wrappers.<ProductComment>lambdaUpdate()
                .eq(ProductComment::getId, comment.getId())
                .set(ProductComment::getStatus, status));
    }

    @Override
    public void delete(Long id) {
        ProductComment comment = requireComment(id);
        // product_comment 无 deleted 字段，此处为物理删除
        productCommentMapper.deleteById(comment.getId());
    }

    // ------------------------------------------------------------------ 内部方法

    private ProductComment requireComment(Long id) {
        BusinessException.throwIf(id == null, "缺少评价ID");
        ProductComment comment = productCommentMapper.selectById(id);
        BusinessException.throwIf(comment == null, "评价不存在或已被删除");
        return comment;
    }

    /** 分页参数兜底：页码从 1 开始，单页最大 200 条 */
    private <T> Page<T> buildPage(Integer page, Integer size) {
        long current = page == null || page < 1 ? 1L : page;
        long limit = size == null || size < 1 ? 10L : Math.min(size, 200);
        return new Page<>(current, limit);
    }

    private List<Long> listProductIdsByName(String keyword) {
        List<Product> products = productMapper.selectList(Wrappers.<Product>lambdaQuery()
                .select(Product::getId)
                .like(Product::getName, keyword));
        return products.stream().map(Product::getId).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private Map<Long, Product> loadProducts(Collection<ProductComment> comments) {
        Set<Long> ids = comments.stream().map(ProductComment::getProductId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Product> products = productMapper.selectBatchIds(ids);
        Map<Long, Product> map = new LinkedHashMap<>();
        if (products != null) {
            products.forEach(product -> map.put(product.getId(), product));
        }
        return map;
    }

    private Map<Long, ProductSku> loadSkus(Collection<ProductComment> comments) {
        Set<Long> ids = comments.stream().map(ProductComment::getSkuId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ProductSku> skus = productSkuMapper.selectBatchIds(ids);
        Map<Long, ProductSku> map = new LinkedHashMap<>();
        if (skus != null) {
            skus.forEach(sku -> map.put(sku.getId(), sku));
        }
        return map;
    }

    private CommentVO toCommentVO(ProductComment comment, Map<Long, Product> productMap,
                                  Map<Long, ProductSku> skuMap, boolean maskAnonymous) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setProductId(comment.getProductId());
        vo.setSkuId(comment.getSkuId());
        vo.setOrderId(comment.getOrderId());
        vo.setUserId(comment.getUserId());
        vo.setStar(comment.getStar());
        vo.setContent(comment.getContent());
        vo.setImages(splitImages(comment.getImages()));
        vo.setIsAnonymous(comment.getIsAnonymous());
        vo.setReplyContent(comment.getReplyContent());
        vo.setReplyUser(comment.getReplyUser());
        vo.setReplyTime(comment.getReplyTime());
        vo.setStatus(comment.getStatus());
        vo.setCreateTime(comment.getCreateTime());

        boolean anonymous = Integer.valueOf(1).equals(comment.getIsAnonymous());
        if (maskAnonymous && anonymous) {
            vo.setUserNickname(ANONYMOUS_NICKNAME);
            vo.setUserAvatar(ANONYMOUS_AVATAR);
        } else {
            vo.setUserNickname(comment.getUserNickname());
            vo.setUserAvatar(comment.getUserAvatar());
        }

        Product product = productMap.get(comment.getProductId());
        if (product != null) {
            vo.setProductName(product.getName());
            vo.setProductImage(product.getMainImage());
        }
        if (comment.getSkuId() != null) {
            ProductSku sku = skuMap.get(comment.getSkuId());
            if (sku != null) {
                vo.setSpecInfo(sku.getSpecInfo());
            }
        }
        return vo;
    }

    /** 晒图字符串转集合 */
    private List<String> splitImages(String images) {
        if (images == null || images.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(images.split(","))
                .map(String::trim)
                .filter(image -> !image.isEmpty())
                .collect(Collectors.toList());
    }

    /** 晒图集合转字符串 */
    private String joinImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        String joined = images.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(image -> !image.isEmpty())
                .collect(Collectors.joining(","));
        return joined.isEmpty() ? null : joined;
    }
}
