package com.mall.controller.admin;

import com.mall.common.BusinessException;
import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.common.annotation.OperLog;
import com.mall.dto.MemberQueryDTO;
import com.mall.service.MemberService;
import com.mall.vo.MemberDetailVO;
import com.mall.vo.MemberVO;
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

import java.util.Map;

/**
 * 管理端 - 会员管理
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "管理端-会员管理", description = "买家会员列表、详情与启用禁用")
public class AdminMemberController {

    private final MemberService memberService;

    @GetMapping({"", "/"})
    @PreAuthorize("hasAuthority('member:list')")
    @Operation(summary = "会员分页列表")
    public Result<PageResult<MemberVO>> page(@Valid MemberQueryDTO query) {
        return Result.success(memberService.page(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('member:detail')")
    @Operation(summary = "会员详情")
    public Result<MemberDetailVO> getDetail(@PathVariable Long id) {
        return Result.success(memberService.getDetail(id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('member:status')")
    @OperLog(module = "会员管理", value = "启用/禁用会员")
    @Operation(summary = "启用/禁用会员")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestParam(value = "status", required = false) Integer status,
                                     @RequestBody(required = false) Map<String, Object> body) {
        memberService.updateStatus(id, resolveStatus(status, body));
        return Result.success();
    }

    /**
     * 兼容 ?status=1 与请求体 {"status":1} 两种传参方式
     */
    private static Integer resolveStatus(Integer status, Map<String, Object> body) {
        if (status != null) {
            return status;
        }
        Object value = body == null ? null : body.get("status");
        if (value == null) {
            throw new BusinessException("缺少参数 status");
        }
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("status 参数不合法");
        }
    }
}
