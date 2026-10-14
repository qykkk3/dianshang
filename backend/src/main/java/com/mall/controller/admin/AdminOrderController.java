package com.mall.controller.admin;

import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.common.annotation.OperLog;
import com.mall.dto.ExpressTrackAddDTO;
import com.mall.dto.OrderPriceDTO;
import com.mall.dto.OrderQueryDTO;
import com.mall.dto.OrderRemarkDTO;
import com.mall.dto.ShipDTO;
import com.mall.service.OrderService;
import com.mall.vo.OrderDetailVO;
import com.mall.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端订单接口（需管理端登录）
 */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Tag(name = "管理端-订单管理", description = "订单查询、发货、物流、备注、改价、关闭、导出与统计")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasAuthority('order:list')")
    @Operation(summary = "订单分页列表")
    public Result<PageResult<OrderVO>> list(@ParameterObject OrderQueryDTO query) {
        return Result.success(orderService.pageAdminOrders(query));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('order:list')")
    @Operation(summary = "各状态订单数量统计")
    public Result<Map<String, Object>> statistics() {
        return Result.success(orderService.adminStatistics());
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('order:export')")
    @OperLog(module = "订单管理", value = "导出订单")
    @Operation(summary = "导出订单 Excel（最多 5000 条）")
    public void export(@ParameterObject OrderQueryDTO query, HttpServletResponse response) {
        orderService.exportAdminOrders(query, response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('order:detail')")
    @Operation(summary = "订单详情")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        return Result.success(orderService.getAdminDetail(id));
    }

    @PutMapping("/{id}/ship")
    @PreAuthorize("hasAnyAuthority('order:doShip', 'order:ship')")
    @OperLog(module = "订单管理", value = "订单发货")
    @Operation(summary = "订单发货（自动生成揽收/运输中轨迹）")
    public Result<Void> ship(@PathVariable Long id, @Valid @RequestBody ShipDTO dto) {
        orderService.ship(id, dto.getExpressCompany(), dto.getExpressNo());
        return Result.success("发货成功", null);
    }

    @PostMapping("/{id}/track")
    @PreAuthorize("hasAuthority('order:track')")
    @OperLog(module = "订单管理", value = "追加物流轨迹")
    @Operation(summary = "追加物流轨迹")
    public Result<Void> addTrack(@PathVariable Long id, @RequestBody ExpressTrackAddDTO dto) {
        orderService.addTrack(id, dto.getStatus(), dto.getContext(), dto.getLocation());
        return Result.success("轨迹已追加", null);
    }

    @PutMapping("/{id}/remark")
    @PreAuthorize("hasAuthority('order:remark')")
    @OperLog(module = "订单管理", value = "订单备注")
    @Operation(summary = "修改商家备注")
    public Result<Void> remark(@PathVariable Long id, @RequestBody OrderRemarkDTO dto) {
        orderService.updateAdminRemark(id, dto.getAdminRemark());
        return Result.success("备注已保存", null);
    }

    @PutMapping("/{id}/price")
    @PreAuthorize("hasAuthority('order:price')")
    @OperLog(module = "订单管理", value = "订单改价")
    @Operation(summary = "订单改价（仅待付款）")
    public Result<Void> price(@PathVariable Long id, @Valid @RequestBody OrderPriceDTO dto) {
        orderService.updatePrice(id, dto.getPayAmount());
        return Result.success("改价成功", null);
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasAuthority('order:close')")
    @OperLog(module = "订单管理", value = "关闭订单")
    @Operation(summary = "关闭订单（仅待付款，回滚库存与优惠券）")
    public Result<Void> close(@PathVariable Long id) {
        orderService.close(id);
        return Result.success("订单已关闭", null);
    }
}
