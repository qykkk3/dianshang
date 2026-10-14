package com.mall.service;

import com.mall.common.PageResult;
import com.mall.dto.CouponQueryDTO;
import com.mall.dto.CouponSaveDTO;
import com.mall.vo.CouponVO;
import com.mall.vo.UserCouponVO;

import java.util.List;

/**
 * 优惠券服务（商城领券 + 管理端维护）
 */
public interface CouponService {

    /** 商城可领取的优惠券（公开） */
    List<CouponVO> pageShopCoupons();

    /** 管理端优惠券列表（含已停用） */
    PageResult<CouponVO> pageAdminCoupons(CouponQueryDTO query);

    /** 新增 / 修改优惠券 */
    void save(CouponSaveDTO dto);

    /** 删除优惠券（逻辑删除，已被领取的券拒绝删除） */
    void delete(Long id);

    /** 启用 / 停用优惠券 */
    void updateStatus(Long id, Integer status);

    /** 某张券的领取记录 */
    PageResult<UserCouponVO> pageCouponRecords(Long couponId, Integer page, Integer size);
}
