package com.mall.controller.admin;

import com.mall.common.Result;
import com.mall.service.DashboardService;
import com.mall.vo.CategorySalesVO;
import com.mall.vo.OrderStatusCountVO;
import com.mall.vo.SalesTrendVO;
import com.mall.vo.StatsOverviewVO;
import com.mall.vo.TopProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端数据看板接口
 */
@Tag(name = "管理端-数据看板")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "数据总览（今日/累计销售额、订单、会员、待办等）")
    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public Result<StatsOverviewVO> overview() {
        return Result.success(dashboardService.overview());
    }

    @Operation(summary = "销售趋势（连续 days 天，缺失日期补 0）")
    @GetMapping("/sales-trend")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public Result<List<SalesTrendVO>> salesTrend(@RequestParam(value = "days", defaultValue = "7") Integer days) {
        return Result.success(dashboardService.salesTrend(days));
    }

    @Operation(summary = "分类销售统计")
    @GetMapping("/category-sales")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public Result<List<CategorySalesVO>> categorySales() {
        return Result.success(dashboardService.categorySales());
    }

    @Operation(summary = "热销商品排行")
    @GetMapping("/top-products")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public Result<List<TopProductVO>> topProducts(@RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return Result.success(dashboardService.topProducts(limit));
    }

    @Operation(summary = "订单状态分布")
    @GetMapping("/order-status")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public Result<List<OrderStatusCountVO>> orderStatus() {
        return Result.success(dashboardService.orderStatusCount());
    }
}
