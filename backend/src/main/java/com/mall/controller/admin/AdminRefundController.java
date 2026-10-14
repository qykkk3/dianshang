package com.mall.controller.admin;

import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.common.annotation.OperLog;
import com.mall.dto.RefundAuditDTO;
import com.mall.service.RefundService;
import com.mall.vo.RefundVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端售后接口（需管理端登录）
 */
@RestController
@RequestMapping("/api/admin/refunds")
@RequiredArgsConstructor
@Tag(name = "管理端-售后管理", description = "售后工单查询与审核")
public class AdminRefundController {

    private final RefundService refundService;

    @GetMapping
    @PreAuthorize("hasAuthority('refund:list')")
    @Operation(summary = "售后工单分页列表")
    public Result<PageResult<RefundVO>> list(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer status,
                                             @RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(refundService.pageAdminRefunds(keyword, status, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('refund:list')")
    @Operation(summary = "售后工单详情")
    public Result<RefundVO> detail(@PathVariable Long id) {
        return Result.success(refundService.getAdminDetail(id));
    }

    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('refund:audit')")
    @OperLog(module = "售后管理", value = "售后审核")
    @Operation(summary = "售后审核（1同意 2拒绝 4退款完成）")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody RefundAuditDTO dto) {
        refundService.audit(id, dto);
        return Result.success("审核完成", null);
    }
}
