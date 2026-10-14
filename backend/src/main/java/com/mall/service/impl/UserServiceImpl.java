package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.BusinessException;
import com.mall.common.PageResult;
import com.mall.dto.PasswordUpdateDTO;
import com.mall.dto.ProfileUpdateDTO;
import com.mall.entity.BrowseHistory;
import com.mall.entity.Coupon;
import com.mall.entity.Favorite;
import com.mall.entity.Product;
import com.mall.entity.User;
import com.mall.entity.UserCoupon;
import com.mall.mapper.BrowseHistoryMapper;
import com.mall.mapper.CouponMapper;
import com.mall.mapper.FavoriteMapper;
import com.mall.mapper.ProductMapper;
import com.mall.mapper.UserCouponMapper;
import com.mall.mapper.UserMapper;
import com.mall.security.SecurityUtils;
import com.mall.service.UserService;
import com.mall.vo.FavoriteVO;
import com.mall.vo.HistoryVO;
import com.mall.vo.UserCouponVO;
import com.mall.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 买家个人中心服务实现（资料、收藏、足迹、我的优惠券）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 用户券状态：未使用 */
    private static final int COUPON_STATUS_UNUSED = 0;
    /** 用户券状态：已使用 */
    private static final int COUPON_STATUS_USED = 1;
    /** 用户券状态：已过期 */
    private static final int COUPON_STATUS_EXPIRED = 2;

    private final UserMapper userMapper;
    private final FavoriteMapper favoriteMapper;
    private final BrowseHistoryMapper browseHistoryMapper;
    private final ProductMapper productMapper;
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserProfileVO getProfile() {
        User user = requireCurrentUser();
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setBirthday(user.getBirthday());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    @Override
    public void updateProfile(ProfileUpdateDTO dto) {
        User user = requireCurrentUser();
        // 仅更新传入的字段（MyBatis-Plus 默认忽略 null）
        User update = new User();
        update.setId(user.getId());
        update.setNickname(dto.getNickname());
        update.setPhone(dto.getPhone());
        update.setEmail(dto.getEmail());
        update.setAvatar(dto.getAvatar());
        update.setGender(dto.getGender());
        update.setBirthday(parseDate(dto.getBirthday()));
        userMapper.updateById(update);
    }

    @Override
    public void updatePassword(PasswordUpdateDTO dto) {
        User user = requireCurrentUser();
        BusinessException.throwIf(!passwordEncoder.matches(dto.getOldPassword(), user.getPassword()), "原密码不正确");
        User update = new User();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(update);
        // 密码修改后保留当前登录态，不强制下线
        log.info("买家 {} 修改密码成功", user.getUsername());
    }

    @Override
    public PageResult<FavoriteVO> pageFavorites(Integer page, Integer size) {
        Long userId = SecurityUtils.getUserId();
        Page<Favorite> pageParam = buildPage(page, size);
        IPage<Favorite> result = favoriteMapper.selectPage(pageParam, Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getCreateTime)
                .orderByDesc(Favorite::getId));
        Map<Long, Product> productMap = loadProducts(result.getRecords().stream()
                .map(Favorite::getProductId).collect(Collectors.toSet()));
        return PageResult.of(result, favorite -> toFavoriteVO(favorite, productMap));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleFavorite(Long productId) {
        BusinessException.throwIf(productId == null, "缺少商品ID");
        Long userId = SecurityUtils.getUserId();
        Favorite exist = favoriteMapper.selectOne(Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId)
                .last("LIMIT 1"));
        if (exist != null) {
            favoriteMapper.deleteById(exist.getId());
            return false;
        }
        Product product = productMapper.selectById(productId);
        BusinessException.throwIf(product == null, "商品不存在或已下架");
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        favoriteMapper.insert(favorite);
        return true;
    }

    @Override
    public void deleteFavorite(Long productId) {
        BusinessException.throwIf(productId == null, "缺少商品ID");
        Long userId = SecurityUtils.getUserId();
        favoriteMapper.delete(Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId));
    }

    @Override
    public PageResult<HistoryVO> pageHistory(Integer page, Integer size) {
        Long userId = SecurityUtils.getUserId();
        Page<BrowseHistory> pageParam = buildPage(page, size);
        IPage<BrowseHistory> result = browseHistoryMapper.selectPage(pageParam, Wrappers.<BrowseHistory>lambdaQuery()
                .eq(BrowseHistory::getUserId, userId)
                .orderByDesc(BrowseHistory::getUpdateTime)
                .orderByDesc(BrowseHistory::getId));
        Map<Long, Product> productMap = loadProducts(result.getRecords().stream()
                .map(BrowseHistory::getProductId).collect(Collectors.toSet()));
        return PageResult.of(result, history -> toHistoryVO(history, productMap));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordHistory(Long productId) {
        BusinessException.throwIf(productId == null, "缺少商品ID");
        Long userId = SecurityUtils.getUserId();
        Long count = browseHistoryMapper.selectCount(Wrappers.<BrowseHistory>lambdaQuery()
                .eq(BrowseHistory::getUserId, userId)
                .eq(BrowseHistory::getProductId, productId));
        if (count != null && count > 0) {
            // 已存在：刷新浏览时间，保证同一用户同一商品只有一条记录
            browseHistoryMapper.update(null, Wrappers.<BrowseHistory>lambdaUpdate()
                    .eq(BrowseHistory::getUserId, userId)
                    .eq(BrowseHistory::getProductId, productId)
                    .set(BrowseHistory::getUpdateTime, LocalDateTime.now()));
            return;
        }
        BrowseHistory history = new BrowseHistory();
        history.setUserId(userId);
        history.setProductId(productId);
        history.setUpdateTime(LocalDateTime.now());
        browseHistoryMapper.insert(history);
    }

    @Override
    public void clearHistory() {
        browseHistoryMapper.delete(Wrappers.<BrowseHistory>lambdaQuery()
                .eq(BrowseHistory::getUserId, SecurityUtils.getUserId()));
    }

    @Override
    public PageResult<UserCouponVO> pageMyCoupons(Integer status, Integer page, Integer size) {
        Long userId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();
        Page<UserCoupon> pageParam = buildPage(page, size);
        IPage<UserCoupon> result = userCouponMapper.selectPage(pageParam, Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .and(status != null, wrapper -> applyStatusFilter(wrapper, status, now))
                .orderByDesc(UserCoupon::getReceiveTime)
                .orderByDesc(UserCoupon::getId));
        Map<Long, Coupon> couponMap = loadCoupons(result.getRecords().stream()
                .map(UserCoupon::getCouponId).collect(Collectors.toSet()));
        return PageResult.of(result, userCoupon -> toUserCouponVO(userCoupon, couponMap, now));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receiveCoupon(Long couponId) {
        BusinessException.throwIf(couponId == null, "缺少优惠券ID");
        Long userId = SecurityUtils.getUserId();
        Coupon coupon = couponMapper.selectById(couponId);
        BusinessException.throwIf(coupon == null, "优惠券不存在");
        BusinessException.throwIf(!Integer.valueOf(1).equals(coupon.getStatus()), "优惠券已停止发放");

        LocalDateTime now = LocalDateTime.now();
        BusinessException.throwIf(coupon.getStartTime() != null && now.isBefore(coupon.getStartTime()), "优惠券尚未开始发放");
        BusinessException.throwIf(coupon.getEndTime() != null && now.isAfter(coupon.getEndTime()), "优惠券已过期");

        int totalCount = coupon.getTotalCount() == null ? 0 : coupon.getTotalCount();
        int receivedCount = coupon.getReceivedCount() == null ? 0 : coupon.getReceivedCount();
        BusinessException.throwIf(receivedCount >= totalCount, "优惠券已被领完");

        int perLimit = coupon.getPerLimit() == null || coupon.getPerLimit() < 1 ? 1 : coupon.getPerLimit();
        Long myCount = userCouponMapper.selectCount(Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, couponId));
        BusinessException.throwIf(myCount != null && myCount >= perLimit, "每人限领 " + perLimit + " 张，您已领取");

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setCouponName(coupon.getName());
        userCoupon.setStatus(COUPON_STATUS_UNUSED);
        // receive_time 无自动填充，必须显式赋值
        userCoupon.setReceiveTime(now);
        userCoupon.setExpireTime(coupon.getEndTime() == null ? now.plusDays(30) : coupon.getEndTime());
        userCouponMapper.insert(userCoupon);

        couponMapper.update(null, Wrappers.<Coupon>lambdaUpdate()
                .eq(Coupon::getId, couponId)
                .set(Coupon::getReceivedCount, receivedCount + 1)
                .set(Coupon::getUpdateTime, now));
    }

    // ------------------------------------------------------------------ 内部方法

    /** 当前登录买家，不存在/被禁用直接抛业务异常 */
    private User requireCurrentUser() {
        User user = userMapper.selectById(SecurityUtils.getUserId());
        BusinessException.throwIf(user == null, "用户不存在");
        BusinessException.throwIf(!Integer.valueOf(1).equals(user.getStatus()), "账号已被禁用，请联系管理员");
        return user;
    }

    private LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new BusinessException("生日格式应为 yyyy-MM-dd");
        }
    }

    /** 分页参数兜底：页码从 1 开始，单页最大 200 条 */
    private <T> Page<T> buildPage(Integer page, Integer size) {
        long current = page == null || page < 1 ? 1L : page;
        long limit = size == null || size < 1 ? 10L : Math.min(size, 200);
        return new Page<>(current, limit);
    }

    /**
     * 我的优惠券状态过滤：0未使用(未过期)、1已使用、2已过期(含过期但未使用的历史数据)
     */
    private void applyStatusFilter(LambdaQueryWrapper<UserCoupon> wrapper, Integer status, LocalDateTime now) {
        if (status == null) {
            return;
        }
        if (status == COUPON_STATUS_UNUSED) {
            wrapper.eq(UserCoupon::getStatus, COUPON_STATUS_UNUSED).ge(UserCoupon::getExpireTime, now);
        } else if (status == COUPON_STATUS_USED) {
            wrapper.eq(UserCoupon::getStatus, COUPON_STATUS_USED);
        } else if (status == COUPON_STATUS_EXPIRED) {
            wrapper.and(w -> w.eq(UserCoupon::getStatus, COUPON_STATUS_EXPIRED)
                    .or(x -> x.eq(UserCoupon::getStatus, COUPON_STATUS_UNUSED).lt(UserCoupon::getExpireTime, now)));
        } else {
            wrapper.eq(UserCoupon::getStatus, status);
        }
    }

    private Map<Long, Product> loadProducts(Collection<Long> productIds) {
        Set<Long> ids = productIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
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

    private FavoriteVO toFavoriteVO(Favorite favorite, Map<Long, Product> productMap) {
        FavoriteVO vo = new FavoriteVO();
        vo.setId(favorite.getId());
        vo.setProductId(favorite.getProductId());
        vo.setCreateTime(favorite.getCreateTime());
        Product product = productMap.get(favorite.getProductId());
        if (product != null) {
            vo.setProductName(product.getName());
            vo.setProductImage(product.getMainImage());
            vo.setPrice(product.getPrice());
            vo.setSales(product.getSales());
            vo.setStatus(product.getStatus());
        }
        return vo;
    }

    private HistoryVO toHistoryVO(BrowseHistory history, Map<Long, Product> productMap) {
        HistoryVO vo = new HistoryVO();
        vo.setId(history.getId());
        vo.setProductId(history.getProductId());
        vo.setBrowseTime(history.getUpdateTime());
        Product product = productMap.get(history.getProductId());
        if (product != null) {
            vo.setProductName(product.getName());
            vo.setProductImage(product.getMainImage());
            vo.setPrice(product.getPrice());
            vo.setSales(product.getSales());
            vo.setStatus(product.getStatus());
        }
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
        if (status != null && status == COUPON_STATUS_UNUSED
                && userCoupon.getExpireTime() != null && userCoupon.getExpireTime().isBefore(now)) {
            status = COUPON_STATUS_EXPIRED;
        }
        vo.setStatus(status);
        vo.setStatusName(couponStatusName(status));
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

    private String couponStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case COUPON_STATUS_UNUSED -> "未使用";
            case COUPON_STATUS_USED -> "已使用";
            case COUPON_STATUS_EXPIRED -> "已过期";
            default -> "未知";
        };
    }

}
