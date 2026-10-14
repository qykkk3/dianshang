package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.BusinessException;
import com.mall.common.PageResult;
import com.mall.dto.CouponQueryDTO;
import com.mall.dto.CouponSaveDTO;
import com.mall.entity.Category;
import com.mall.entity.Coupon;
import com.mall.entity.UserCoupon;
import com.mall.mapper.CategoryMapper;
import com.mall.mapper.CouponMapper;
import com.mall.mapper.UserCouponMapper;
import com.mall.security.SecurityUtils;
import com.mall.service.CouponService;
import com.mall.vo.CouponVO;
import com.mall.vo.UserCouponVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 优惠券服务实现
 *
 * <p>商城领券中心只展示「启用且在有效期内」的券；管理端可查看含已停用的全部券。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    /** 表单时间格式 */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 状态：启用 */
    private static final int STATUS_ENABLED = 1;
    /** 状态：停用 */
    private static final int STATUS_DISABLED = 0;
    /** 类型：满减 */
    private static final int TYPE_AMOUNT = 1;
    /** 类型：折扣 */
    private static final int TYPE_DISCOUNT = 2;
    /** 范围：全场 */
    private static final int SCOPE_ALL = 1;
    /** 范围：指定分类 */
    private static final int SCOPE_CATEGORY = 2;
    /** 范围：指定商品 */
    private static final int SCOPE_PRODUCT = 3;

    /** 折扣下限 */
    private static final BigDecimal DISCOUNT_MIN = new BigDecimal("0.1");
    /** 折扣上限 */
    private static final BigDecimal DISCOUNT_MAX = new BigDecimal("9.9");
    /** 折扣券默认折扣率（不打折） */
    private static final BigDecimal DISCOUNT_DEFAULT = new BigDecimal("10.00");

    /** 用户券状态：未使用 */
    private static final int USER_COUPON_UNUSED = 0;
    /** 用户券状态：已使用 */
    private static final int USER_COUPON_USED = 1;
    /** 用户券状态：已过期 */
    private static final int USER_COUPON_EXPIRED = 2;

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final CategoryMapper categoryMapper;

    @Override
    public List<CouponVO> pageShopCoupons() {
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> coupons = couponMapper.selectList(Wrappers.<Coupon>lambdaQuery()
                .eq(Coupon::getStatus, STATUS_ENABLED)
                .le(Coupon::getStartTime, now)
                .ge(Coupon::getEndTime, now)
                .orderByDesc(Coupon::getCreateTime)
                .orderByDesc(Coupon::getId));
        if (coupons.isEmpty()) {
            return new ArrayList<>();
        }
        // 当前登录用户已领取的券（未登录时为空，received 全部为 false）
        Set<Long> receivedCouponIds = listReceivedCouponIds(coupons);
        Map<Long, String> categoryNames = loadCategoryNames(coupons);
        return coupons.stream()
                .map(coupon -> toCouponVO(coupon, categoryNames, receivedCouponIds.contains(coupon.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<CouponVO> pageAdminCoupons(CouponQueryDTO query) {
        CouponQueryDTO condition = query == null ? new CouponQueryDTO() : query;
        Page<Coupon> pageParam = buildPage(condition.getPage(), condition.getSize());
        String keyword = condition.getKeyword() == null ? null : condition.getKeyword().trim();
        IPage<Coupon> result = couponMapper.selectPage(pageParam, Wrappers.<Coupon>lambdaQuery()
                .like(StringUtils.hasText(keyword), Coupon::getName, keyword)
                .eq(condition.getStatus() != null, Coupon::getStatus, condition.getStatus())
                .orderByDesc(Coupon::getId));
        Map<Long, String> categoryNames = loadCategoryNames(result.getRecords());
        return PageResult.of(result, coupon -> toCouponVO(coupon, categoryNames, false));
    }

    @Override
    public void save(CouponSaveDTO dto) {
        BusinessException.throwIf(dto == null, "缺少优惠券信息");
        LocalDateTime startTime = parseDateTime(dto.getStartTime(), "生效时间");
        LocalDateTime endTime = parseDateTime(dto.getEndTime(), "失效时间");
        BusinessException.throwIf(!startTime.isBefore(endTime), "生效时间必须早于失效时间");

        Integer type = dto.getType();
        if (type != null && type == TYPE_AMOUNT) {
            BusinessException.throwIf(dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0,
                    "满减金额必须大于 0");
        } else if (type != null && type == TYPE_DISCOUNT) {
            BigDecimal discount = dto.getDiscount();
            BusinessException.throwIf(discount == null
                            || discount.compareTo(DISCOUNT_MIN) < 0 || discount.compareTo(DISCOUNT_MAX) > 0,
                    "折扣率需在 0.1 ~ 9.9 之间");
        }

        Integer scope = dto.getScope() == null ? SCOPE_ALL : dto.getScope();
        List<Long> scopeIds = dto.getScopeIds() == null ? new ArrayList<>() : dto.getScopeIds().stream()
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        BusinessException.throwIf(scope != SCOPE_ALL && scopeIds.isEmpty(), "请选择优惠券适用范围");

        Coupon coupon = dto.getId() == null ? new Coupon() : requireCoupon(dto.getId());
        if (dto.getId() != null) {
            int receivedCount = coupon.getReceivedCount() == null ? 0 : coupon.getReceivedCount();
            BusinessException.throwIf(dto.getTotalCount() != null && dto.getTotalCount() < receivedCount,
                    "发行总量不能小于已领取数量（" + receivedCount + "）");
        }
        coupon.setName(dto.getName());
        coupon.setType(type);
        coupon.setAmount(dto.getAmount() == null ? BigDecimal.ZERO : dto.getAmount());
        coupon.setDiscount(dto.getDiscount() == null ? DISCOUNT_DEFAULT : dto.getDiscount());
        coupon.setMinAmount(dto.getMinAmount() == null ? BigDecimal.ZERO : dto.getMinAmount());
        coupon.setTotalCount(dto.getTotalCount());
        coupon.setPerLimit(dto.getPerLimit() == null || dto.getPerLimit() < 1 ? 1 : dto.getPerLimit());
        coupon.setScope(scope);
        coupon.setScopeIds(scope == SCOPE_ALL ? null : joinIds(scopeIds));
        coupon.setStartTime(startTime);
        coupon.setEndTime(endTime);
        coupon.setStatus(dto.getStatus() == null ? STATUS_ENABLED : dto.getStatus());
        coupon.setDescription(dto.getDescription());

        if (dto.getId() == null) {
            coupon.setReceivedCount(0);
            coupon.setUsedCount(0);
            couponMapper.insert(coupon);
        } else {
            couponMapper.updateById(coupon);
        }
    }

    @Override
    public void delete(Long id) {
        Coupon coupon = requireCoupon(id);
        int receivedCount = coupon.getReceivedCount() == null ? 0 : coupon.getReceivedCount();
        BusinessException.throwIf(receivedCount > 0, "该优惠券已被领取，不能删除，可改为停用");
        // 逻辑删除（coupon.deleted）
        couponMapper.deleteById(coupon.getId());
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        BusinessException.throwIf(status == null || (status != STATUS_ENABLED && status != STATUS_DISABLED),
                "优惠券状态只能为 1启用 或 0停用");
        Coupon coupon = requireCoupon(id);
        couponMapper.update(null, Wrappers.<Coupon>lambdaUpdate()
                .eq(Coupon::getId, coupon.getId())
                .set(Coupon::getStatus, status)
                .set(Coupon::getUpdateTime, LocalDateTime.now()));
    }

    @Override
    public PageResult<UserCouponVO> pageCouponRecords(Long couponId, Integer page, Integer size) {
        BusinessException.throwIf(couponId == null, "缺少优惠券ID");
        Page<UserCoupon> pageParam = buildPage(page, size);
        IPage<UserCoupon> result = userCouponMapper.selectPage(pageParam, Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getCouponId, couponId)
                .orderByDesc(UserCoupon::getReceiveTime)
                .orderByDesc(UserCoupon::getId));
        Map<Long, Coupon> couponMap = loadCoupons(result.getRecords().stream()
                .map(UserCoupon::getCouponId).collect(Collectors.toSet()));
        LocalDateTime now = LocalDateTime.now();
        return PageResult.of(result, userCoupon -> toUserCouponVO(userCoupon, couponMap, now));
    }

    // ------------------------------------------------------------------ 内部方法

    private Coupon requireCoupon(Long id) {
        BusinessException.throwIf(id == null, "缺少优惠券ID");
        Coupon coupon = couponMapper.selectById(id);
        BusinessException.throwIf(coupon == null, "优惠券不存在或已被删除");
        return coupon;
    }

    /** 分页参数兜底：页码从 1 开始，单页最大 200 条 */
    private <T> Page<T> buildPage(Integer page, Integer size) {
        long current = page == null || page < 1 ? 1L : page;
        long limit = size == null || size < 1 ? 10L : Math.min(size, 200);
        return new Page<>(current, limit);
    }

    private LocalDateTime parseDateTime(String text, String label) {
        BusinessException.throwIf(!StringUtils.hasText(text), "请选择" + label);
        try {
            return LocalDateTime.parse(text.trim(), DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new BusinessException(label + "格式应为 yyyy-MM-dd HH:mm:ss");
        }
    }

    private String joinIds(Collection<Long> ids) {
        return ids.stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.joining(","));
    }

    private List<Long> parseIds(String text) {
        if (!StringUtils.hasText(text)) {
            return new ArrayList<>();
        }
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> {
                    try {
                        return Long.valueOf(value);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 范围名称：scope=2 时拼接分类名称，其余为固定文案
     */
    private String resolveScopeNames(Coupon coupon, Map<Long, String> categoryNames) {
        Integer scope = coupon.getScope();
        if (scope == null) {
            return "";
        }
        if (scope == SCOPE_ALL) {
            return "全场通用";
        }
        if (scope == SCOPE_CATEGORY) {
            String names = parseIds(coupon.getScopeIds()).stream()
                    .map(categoryNames::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(","));
            return names.isEmpty() ? "指定分类" : names;
        }
        if (scope == SCOPE_PRODUCT) {
            return "指定商品";
        }
        return "";
    }

    private String resolveTypeName(Integer type) {
        if (type == null) {
            return "";
        }
        return type == TYPE_AMOUNT ? "满减" : type == TYPE_DISCOUNT ? "折扣" : "";
    }

    /** 当前登录用户已领取的券ID集合（未登录返回空集合） */
    private Set<Long> listReceivedCouponIds(List<Coupon> coupons) {
        Long userId = SecurityUtils.getUserIdOrNull();
        if (userId == null || coupons.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> couponIds = coupons.stream().map(Coupon::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<UserCoupon> records = userCouponMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
                .select(UserCoupon::getCouponId)
                .eq(UserCoupon::getUserId, userId)
                .in(UserCoupon::getCouponId, couponIds));
        Set<Long> received = new HashSet<>();
        records.forEach(record -> received.add(record.getCouponId()));
        return received;
    }

    private Map<Long, String> loadCategoryNames(List<Coupon> coupons) {
        Set<Long> categoryIds = coupons.stream()
                .filter(coupon -> Integer.valueOf(SCOPE_CATEGORY).equals(coupon.getScope()))
                .flatMap(coupon -> parseIds(coupon.getScopeIds()).stream())
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Category> categories = categoryMapper.selectBatchIds(categoryIds);
        Map<Long, String> map = new LinkedHashMap<>();
        if (categories != null) {
            categories.forEach(category -> map.put(category.getId(), category.getName()));
        }
        return map;
    }

    private Map<Long, Coupon> loadCoupons(Collection<Long> couponIds) {
        Set<Long> ids = couponIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Coupon> coupons = couponMapper.selectBatchIds(ids);
        Map<Long, Coupon> map = new LinkedHashMap<>();
        if (coupons != null) {
            coupons.forEach(coupon -> map.put(coupon.getId(), coupon));
        }
        return map;
    }

    private CouponVO toCouponVO(Coupon coupon, Map<Long, String> categoryNames, boolean received) {
        CouponVO vo = new CouponVO();
        vo.setId(coupon.getId());
        vo.setName(coupon.getName());
        vo.setType(coupon.getType());
        vo.setTypeName(resolveTypeName(coupon.getType()));
        vo.setAmount(coupon.getAmount());
        vo.setDiscount(coupon.getDiscount());
        vo.setMinAmount(coupon.getMinAmount());
        vo.setTotalCount(coupon.getTotalCount());
        vo.setReceivedCount(coupon.getReceivedCount());
        vo.setUsedCount(coupon.getUsedCount());
        vo.setPerLimit(coupon.getPerLimit());
        vo.setScope(coupon.getScope());
        vo.setScopeNames(resolveScopeNames(coupon, categoryNames));
        vo.setStartTime(coupon.getStartTime());
        vo.setEndTime(coupon.getEndTime());
        vo.setStatus(coupon.getStatus());
        vo.setDescription(coupon.getDescription());
        vo.setReceived(received);
        return vo;
    }

    /**
     * 用户券 VO：status=0 且已过期时对外展示为 2 已过期
     */
    private UserCouponVO toUserCouponVO(UserCoupon userCoupon, Map<Long, Coupon> couponMap, LocalDateTime now) {
        UserCouponVO vo = new UserCouponVO();
        vo.setId(userCoupon.getId());
        vo.setUserId(userCoupon.getUserId());
        vo.setCouponId(userCoupon.getCouponId());
        vo.setCouponName(userCoupon.getCouponName());
        vo.setOrderId(userCoupon.getOrderId());
        vo.setReceiveTime(userCoupon.getReceiveTime());
        vo.setUseTime(userCoupon.getUseTime());
        vo.setExpireTime(userCoupon.getExpireTime());
        Integer status = userCoupon.getStatus();
        if (status != null && status == USER_COUPON_UNUSED
                && userCoupon.getExpireTime() != null && userCoupon.getExpireTime().isBefore(now)) {
            status = USER_COUPON_EXPIRED;
        }
        vo.setStatus(status);
        if (status == null) {
            vo.setStatusName("未知");
        } else if (status == USER_COUPON_UNUSED) {
            vo.setStatusName("未使用");
        } else if (status == USER_COUPON_USED) {
            vo.setStatusName("已使用");
        } else if (status == USER_COUPON_EXPIRED) {
            vo.setStatusName("已过期");
        } else {
            vo.setStatusName("未知");
        }
        Coupon coupon = couponMap.get(userCoupon.getCouponId());
        if (coupon != null) {
            vo.setType(coupon.getType());
            vo.setAmount(coupon.getAmount());
            vo.setDiscount(coupon.getDiscount());
            vo.setMinAmount(coupon.getMinAmount());
            vo.setDescription(coupon.getDescription());
            if (vo.getCouponName() == null) {
                vo.setCouponName(coupon.getName());
            }
        }
        return vo;
    }
}
