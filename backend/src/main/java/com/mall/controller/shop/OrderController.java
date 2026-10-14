package com.mall.controller.shop;

import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.dto.OrderPreviewDTO;
import com.mall.dto.OrderQueryDTO;
import com.mall.dto.OrderSubmitDTO;
import com.mall.service.OrderService;
import com.mall.vo.ExpressTrackVO;
import com.mall.vo.OrderCountVO;
import com.mall.vo.OrderDetailVO;
import com.mall.vo.OrderPreviewVO;
import com.mall.vo.OrderSubmitVO;
import com.mall.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 买家订单接口（需买家登录）
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Tag(name = "买家订单", description = "结算预览、下单、支付、取消、收货与订单查询")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/preview")
    @Operation(summary = "结算预览（重算金额与可用优惠券）")
    public Result<OrderPreviewVO> preview(@Valid @RequestBody OrderPreviewDTO dto) {
        return Result.success(orderService.preview(dto));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交订单（扣库存/核销优惠券）")
    public Result<OrderSubmitVO> submit(@Valid @RequestBody OrderSubmitDTO dto) {
        return Result.success("下单成功", orderService.submit(dto));
    }

    @GetMapping("/list")
    @Operation(summary = "我的订单分页")
    public Result<PageResult<OrderVO>> list(@ParameterObject OrderQueryDTO query) {
        return Result.success(orderService.pageBuyerOrders(query));
    }

    @GetMapping("/counts")
    @Operation(summary = "各状态订单数量")
    public Result<OrderCountVO> counts() {
        return Result.success(orderService.countByStatus());
    }

    @GetMapping("/{orderNo}")
    @Operation(summary = "订单详情（含物流轨迹与售后单）")
    public Result<OrderDetailVO> detail(@PathVariable String orderNo) {
        return Result.success(orderService.getBuyerDetail(orderNo));
    }

    @PostMapping("/{orderNo}/pay")
    @Operation(summary = "模拟支付")
    public Result<Void> pay(@PathVariable String orderNo) {
        orderService.pay(orderNo);
        return Result.success("支付成功", null);
    }

    @PostMapping("/{orderNo}/cancel")
    @Operation(summary = "取消订单（回滚库存与优惠券）")
    public Result<Void> cancel(@PathVariable String orderNo) {
        orderService.cancel(orderNo);
        return Result.success("订单已取消", null);
    }

    @PostMapping("/{orderNo}/confirm")
    @Operation(summary = "确认收货")
    public Result<Void> confirm(@PathVariable String orderNo) {
        orderService.confirmReceive(orderNo);
        return Result.success("确认收货成功", null);
    }

    @DeleteMapping("/{orderNo}")
    @Operation(summary = "删除订单（仅已完成/已取消）")
    public Result<Void> delete(@PathVariable String orderNo) {
        orderService.deleteOrder(orderNo);
        return Result.success();
    }

    @GetMapping("/{orderNo}/track")
    @Operation(summary = "订单物流轨迹")
    public Result<List<ExpressTrackVO>> track(@PathVariable String orderNo) {
        return Result.success(orderService.listTracks(orderNo));
    }
}
