package com.mall.controller.shop;

import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.dto.RefundApplyDTO;
import com.mall.service.RefundService;
import com.mall.vo.RefundVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 买家售后接口（需买家登录）
 */
@RestController
@RequestMapping("/api/refund")
@RequiredArgsConstructor
@Tag(name = "买家售后", description = "申请售后、售后列表/详情与撤销申请")
public class RefundController {

    private final RefundService refundService;

    @PostMapping("/apply")
    @Operation(summary = "申请售后")
    public Result<Long> apply(@Valid @RequestBody RefundApplyDTO dto) {
        return Result.success("售后申请已提交", refundService.apply(dto));
    }

    @GetMapping("/list")
    @Operation(summary = "我的售后列表")
    public Result<PageResult<RefundVO>> list(@RequestParam(required = false) Integer status,
                                             @RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(refundService.pageBuyerRefunds(status, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "售后详情")
    public Result<RefundVO> detail(@PathVariable Long id) {
        return Result.success(refundService.getBuyerDetail(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "撤销售后申请")
    public Result<Void> cancel(@PathVariable Long id) {
        refundService.cancel(id);
        return Result.success("售后申请已撤销", null);
    }
}
