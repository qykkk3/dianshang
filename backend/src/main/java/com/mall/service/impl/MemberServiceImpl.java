package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.BusinessException;
import com.mall.common.PageResult;
import com.mall.common.enums.OrderStatus;
import com.mall.dto.MemberQueryDTO;
import com.mall.entity.Favorite;
import com.mall.entity.Orders;
import com.mall.entity.ProductComment;
import com.mall.entity.User;
import com.mall.entity.UserAddress;
import com.mall.mapper.FavoriteMapper;
import com.mall.mapper.OrdersMapper;
import com.mall.mapper.ProductCommentMapper;
import com.mall.mapper.UserAddressMapper;
import com.mall.mapper.UserMapper;
import com.mall.security.LoginUser;
import com.mall.security.TokenService;
import com.mall.service.MemberService;
import com.mall.vo.MemberDetailVO;
import com.mall.vo.MemberVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 会员（买家）管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    /**
     * 计入消费统计的订单状态：待发货(1) / 待收货(2) / 已完成(3) / 售后中(5)
     * 与 {@link OrderStatus#isPaid(Integer)} 口径一致，不含待付款(0)与已取消(4)
     */
    private static final List<Integer> PAID_ORDER_STATUS = List.of(1, 2, 3, 5);

    /** 详情页最近订单条数 */
    private static final int RECENT_ORDER_LIMIT = 10;

    private final UserMapper userMapper;
    private final OrdersMapper ordersMapper;
    private final UserAddressMapper userAddressMapper;
    private final FavoriteMapper favoriteMapper;
    private final ProductCommentMapper productCommentMapper;
    private final TokenService tokenService;

    @Override
    public PageResult<MemberVO> page(MemberQueryDTO query) {
        MemberQueryDTO dto = query == null ? new MemberQueryDTO() : query;
        long current = dto.getPage() == null || dto.getPage() < 1 ? 1L : dto.getPage();
        long size = dto.getSize() == null || dto.getSize() < 1 ? 10L : dto.getSize();
        Page<User> page = new Page<>(current, size);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dto.getKeyword())) {
            String keyword = dto.getKeyword().trim();
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getPhone, keyword));
        }
        wrapper.eq(dto.getStatus() != null, User::getStatus, dto.getStatus());
        wrapper.orderByDesc(User::getCreateTime).orderByDesc(User::getId);
        // user 表含逻辑删除字段，MyBatis-Plus 自动追加 deleted = 0
        userMapper.selectPage(page, wrapper);

        // 一次性统计当前页所有会员的订单数与消费金额，避免逐条查询（N+1）
        Map<Long, OrderStat> statMap = loadOrderStats(page.getRecords().stream()
                .map(User::getId)
                .collect(Collectors.toList()));
        return PageResult.of(page, user -> toVO(user, statMap.get(user.getId())));
    }

    @Override
    public MemberDetailVO getDetail(Long id) {
        User user = requireUser(id);
        MemberDetailVO vo = new MemberDetailVO();
        // 复制同名属性（含 birthday），User 的 password 不会出现在 VO 中
        BeanUtils.copyProperties(user, vo);

        OrderStat stat = loadOrderStats(Collections.singletonList(id)).get(id);
        vo.setOrderCount(stat == null || stat.count() == null ? 0L : stat.count());
        vo.setTotalConsumption(stat == null || stat.amount() == null ? BigDecimal.ZERO : stat.amount());

        vo.setAddressCount(userAddressMapper.selectCount(
                new LambdaQueryWrapper<UserAddress>().eq(UserAddress::getUserId, id)));
        vo.setFavoriteCount(favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, id)));
        vo.setCommentCount(productCommentMapper.selectCount(
                new LambdaQueryWrapper<ProductComment>().eq(ProductComment::getUserId, id)));
        vo.setRecentOrders(loadRecentOrders(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态值不合法，只能为 1（正常）或 0（禁用）");
        }
        requireUser(id);
        User entity = new User();
        entity.setId(id);
        entity.setStatus(status);
        userMapper.updateById(entity);
        if (status == 0) {
            // 禁用会员后强制下线，Token 立即失效
            tokenService.kickOut(LoginUser.TYPE_BUYER, id);
            log.info("会员[{}]已被禁用，已强制下线", id);
        }
    }

    /* ------------------------------------------------------------------ 内部方法 */

    private User requireUser(Long id) {
        if (id == null) {
            throw new BusinessException("会员ID不能为空");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("会员不存在或已被删除");
        }
        return user;
    }

    /**
     * 批量统计会员订单数与累计消费金额（一次 SQL 完成，避免 N+1）
     *
     * <p>生成的 SQL 形如：<br>
     * SELECT user_id, COUNT(*) AS cnt, COALESCE(SUM(pay_amount), 0) AS amount<br>
     * FROM orders WHERE deleted = 0 AND user_id IN (...) AND status IN (1,2,3,5) GROUP BY user_id</p>
     */
    private Map<Long, OrderStat> loadOrderStats(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<Orders> wrapper = new QueryWrapper<>();
        wrapper.select("user_id", "COUNT(*) AS cnt", "COALESCE(SUM(pay_amount), 0) AS amount")
                .in("user_id", userIds)
                .in("status", PAID_ORDER_STATUS)
                .groupBy("user_id");
        List<Map<String, Object>> rows = ordersMapper.selectMaps(wrapper);
        Map<Long, OrderStat> result = new HashMap<>();
        if (rows == null) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            Long userId = toLong(pick(row, "user_id", "userId"));
            Long count = toLong(pick(row, "cnt", "count"));
            BigDecimal amount = toDecimal(pick(row, "amount", "sum(pay_amount)"));
            if (userId != null) {
                result.put(userId, new OrderStat(count == null ? 0L : count,
                        amount == null ? BigDecimal.ZERO : amount));
            }
        }
        return result;
    }

    /** 最近 10 条订单（不查询总数，只统计消费口径外的展示信息） */
    private List<Map<String, Object>> loadRecentOrders(Long userId) {
        Page<Orders> page = new Page<>(1, RECENT_ORDER_LIMIT, false);
        ordersMapper.selectPage(page, new LambdaQueryWrapper<Orders>()
                .eq(Orders::getUserId, userId)
                .orderByDesc(Orders::getCreateTime)
                .orderByDesc(Orders::getId));
        List<Map<String, Object>> recentOrders = new ArrayList<>();
        for (Orders order : page.getRecords()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("orderNo", order.getOrderNo());
            item.put("payAmount", order.getPayAmount());
            item.put("status", order.getStatus());
            item.put("statusName", OrderStatus.descOf(order.getStatus()));
            item.put("createTime", order.getCreateTime());
            recentOrders.add(item);
        }
        return recentOrders;
    }

    private MemberVO toVO(User user, OrderStat stat) {
        MemberVO vo = new MemberVO();
        BeanUtils.copyProperties(user, vo);
        vo.setOrderCount(stat == null || stat.count() == null ? 0L : stat.count());
        vo.setTotalConsumption(stat == null || stat.amount() == null ? BigDecimal.ZERO : stat.amount());
        return vo;
    }

    /** 从 selectMaps 结果中按候选键（忽略大小写）取值 */
    private static Object pick(Map<String, Object> row, String... keys) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        for (String key : keys) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal toDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        try {
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 会员订单统计值 */
    private record OrderStat(Long count, BigDecimal amount) {
    }
}
