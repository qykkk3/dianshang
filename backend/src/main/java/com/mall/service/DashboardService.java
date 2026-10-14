package com.mall.service;

import com.mall.vo.CategorySalesVO;
import com.mall.vo.OrderStatusCountVO;
import com.mall.vo.SalesTrendVO;
import com.mall.vo.StatsOverviewVO;
import com.mall.vo.TopProductVO;

import java.util.List;

/**
 * 数据看板服务
 */
public interface DashboardService {

    /** 首页数据总览 */
    StatsOverviewVO overview();

    /** 销售趋势（连续 days 天，缺失日期补 0） */
    List<SalesTrendVO> salesTrend(Integer days);

    /** 分类销售统计 */
    List<CategorySalesVO> categorySales();

    /** 热销商品排行 */
    List<TopProductVO> topProducts(Integer limit);

    /** 订单状态分布 */
    List<OrderStatusCountVO> orderStatusCount();
}
