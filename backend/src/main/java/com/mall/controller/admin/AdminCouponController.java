package com.mall.controller.admin;

import com.mall.common.BusinessException;
import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.common.annotation.OperLog;
import com.mall.dto.CouponQueryDTO;
import com.mall.dto.CouponSaveDTO;
import com.mall.service.CouponService;
import com.mall.vo.CouponVO;
import com.mall.vo.UserCouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端优惠券接口
 */
@Tag(name = "管理端-优惠券管理")
@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService couponService;

    @Operation(summary = "优惠券列表")
    @PreAuthorize("hasAuthority('coupon:list')")
    @GetMapping
    public Result<PageResult<CouponVO>> list(CouponQueryDTO query) {
        return Result.success(couponService.pageAdminCoupons(query));
    }

    @Operation(summary = "新增优惠券")
    @PreAuthorize("hasAuthority('coupon:add')")
    @OperLog(module = "营销管理", value = "新增优惠券")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody CouponSaveDTO dto) {
        dto.setId(null);
        couponService.save(dto);
        return Result.success();
    }

    @Operation(summary = "编辑优惠券")
    @PreAuthorize("hasAuthority('coupon:edit')")
    @OperLog(module = "营销管理", value = "编辑优惠券")
    @PutMapping("/{id}")
    public Result<Void> edit(@PathVariable Long id, @Valid @RequestBody CouponSaveDTO dto) {
        dto.setId(id);
        couponService.save(dto);
        return Result.success();
    }

    @Operation(summary = "删除优惠券")
    @PreAuthorize("hasAuthority('coupon:delete')")
    @OperLog(module = "营销管理", value = "删除优惠券")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        couponService.delete(id);
        return Result.success();
    }

    @Operation(summary = "启用 / 停用优惠券")
    @PreAuthorize("hasAuthority('coupon:status')")
    @OperLog(module = "营销管理", value = "修改优惠券状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestParam(value = "status", required = false) Integer status,
                                     @RequestBody(required = false) Map<String, Object> body) {
        couponService.updateStatus(id, resolveStatus(body, status));
        return Result.success();
    }

    @Operation(summary = "优惠券领取记录")
    @PreAuthorize("hasAuthority('coupon:list')")
    @GetMapping("/{id}/records")
    public Result<PageResult<UserCouponVO>> records(@PathVariable Long id,
                                                    @RequestParam(required = false, defaultValue = "1") Integer page,
                                                    @RequestParam(required = false, defaultValue = "10") Integer size) {
        return Result.success(couponService.pageCouponRecords(id, page, size));
    }

    /**
     * 兼容两种传参：?status=1 或 JSON 请求体 {"status":1}
     */
    private Integer resolveStatus(Map<String, Object> body, Integer status) {
        if (status != null) {
            return status;
        }
        Object value = body == null ? null : body.get("status");
        if (value == null) {
            throw new BusinessException("缺少状态参数");
        }
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("状态参数不合法");
        }
    }
}
