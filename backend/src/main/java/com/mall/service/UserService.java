package com.mall.service;

import com.mall.common.PageResult;
import com.mall.dto.PasswordUpdateDTO;
import com.mall.dto.ProfileUpdateDTO;
import com.mall.vo.FavoriteVO;
import com.mall.vo.HistoryVO;
import com.mall.vo.UserCouponVO;
import com.mall.vo.UserProfileVO;

/**
 * 买家个人中心服务
 */
public interface UserService {

    /** 个人资料 */
    UserProfileVO getProfile();

    /** 修改个人资料 */
    void updateProfile(ProfileUpdateDTO dto);

    /** 修改密码（校验原密码） */
    void updatePassword(PasswordUpdateDTO dto);

    /** 我的收藏（分页） */
    PageResult<FavoriteVO> pageFavorites(Integer page, Integer size);

    /**
     * 切换收藏状态
     *
     * @return true=已收藏 false=已取消收藏
     */
    boolean toggleFavorite(Long productId);

    /** 取消收藏 */
    void deleteFavorite(Long productId);

    /** 我的浏览记录（分页） */
    PageResult<HistoryVO> pageHistory(Integer page, Integer size);

    /** 记录商品浏览足迹（存在则刷新时间） */
    void recordHistory(Long productId);

    /** 清空浏览记录 */
    void clearHistory();

    /** 我的优惠券（分页，status 为空查全部） */
    PageResult<UserCouponVO> pageMyCoupons(Integer status, Integer page, Integer size);

    /** 领取优惠券 */
    void receiveCoupon(Long couponId);
}
