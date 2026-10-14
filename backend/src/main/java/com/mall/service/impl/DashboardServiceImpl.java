package com.mall.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.enums.OrderStatus;
import com.mall.common.enums.RefundStatus;
import com.mall.entity.Orders;
import com.mall.entity.Product;
import com.mall.entity.Refund;
import com.mall.entity.SysConfig;
import com.mall.entity.User;
import com.mall.mapper.OrdersMapper;
import com.mall.mapper.ProductCommentMapper;
import com.mall.mapper.ProductMapper;
import com.mall.mapper.RefundMapper;
import com.mall.mapper.StatsMapper;
import com.mall.mapper.SysConfigMapper;
import com.mall.mapper.UserMapper;
import com.mall.security.TokenService;
import com.mall.service.DashboardService;
import com.mall.vo.CategorySalesVO;
import com.mall.vo.OrderStatusCountVO;
import com.mall.vo.SalesTrendVO;
import com.mall.vo.StatsOverviewVO;
import com.mall.vo.TopProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据看板服务实现
 *
 * <p>统计口径：</p>
 * <ul>
 *   <li>销售额 / 订单数按 {@link StatsMapper} 定义，销售额只统计已支付订单（1待发货/2待收货/3已完成/5售后中）</li>
 *   <li>今日 = 今天 00:00:00 ~ 明天 00:00:00；累计 = 2000-01-01 ~ 明天 00:00:00</li>
 *   <li>库存预警阈值优先取 sys_config.stock_warn_threshold，缺省 10</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    /** 库存预警阈值配置项 */
    private static final String STOCK_WARN_THRESHOLD_KEY = "stock_warn_threshold";
    /** 库存预警默认阈值 */
    private static final int DEFAULT_STOCK_WARN_THRESHOLD = 10;
    /** 销售趋势默认天数 */
    private static final int DEFAULT_TREND_DAYS = 7;
    /** 销售趋势最大天数 */
    private static final int MAX_TREND_DAYS = 90;
    /** 热销排行默认条数 */
    private static final int DEFAULT_TOP_LIMIT = 10;
    /** 热销排行最大条数 */
    private static final int MAX_TOP_LIMIT = 100;
    /** 累计统计起始时间 */
    private static final LocalDateTime TOTAL_STAT_START = LocalDateTime.of(2000, 1, 1, 0, 0, 0);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final StatsMapper statsMapper;
    private final UserMapper userMapper;
    private final OrdersMapper ordersMapper;
    private final ProductMapper productMapper;
    private final RefundMapper refundMapper;
    private final ProductCommentMapper productCommentMapper;
    private final SysConfigMapper sysConfigMapper;
    private final TokenService tokenService;

    @Override
    public StatsOverviewVO overview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);

        StatsOverviewVO vo = new StatsOverviewVO();
        // 今日
        vo.setTodaySales(nullToZero(statsMapper.sumSalesBetween(todayStart, tomorrowStart)));
        vo.setTodayOrders(nullToZero(statsMapper.countOrdersBetween(todayStart, tomorrowStart)));
        vo.setTodayNewUsers(nullToZero(userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .ge(User::getCreateTime, todayStart))));
        // 累计
        vo.setTotalSales(nullToZero(statsMapper.sumSalesBetween(TOTAL_STAT_START, tomorrowStart)));
        vo.setTotalOrders(nullToZero(ordersMapper.selectCount(null)));
        vo.setTotalUsers(nullToZero(userMapper.selectCount(null)));
        vo.setTotalProducts(nullToZero(productMapper.selectCount(Wrappers.<Product>lambdaQuery()
                .eq(Product::getStatus, 1))));
        // 待处理
        vo.setPendingShipOrders(nullToZero(ordersMapper.selectCount(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getStatus, OrderStatus.PAID.getCode()))));
        vo.setPendingRefunds(nullToZero(refundMapper.selectCount(Wrappers.<Refund>lambdaQuery()
                .eq(Refund::getStatus, RefundStatus.PENDING.getCode()))));
        vo.setStockWarnings(nullToZero(statsMapper.countStockWarnings(resolveStockWarnThreshold())));
        vo.setOnlineUsers(nullToZero(tokenService.countOnline()));
        vo.setTotalComments(nullToZero(productCommentMapper.selectCount(null)));
        return vo;
    }

    @Override
    public List<SalesTrendVO> salesTrend(Integer days) {
        int dayCount = days == null || days < 1 ? DEFAULT_TREND_DAYS : Math.min(days, MAX_TREND_DAYS);
        LocalDate startDate = LocalDate.now().minusDays(dayCount - 1L);
        List<SalesTrendVO> rows = statsMapper.selectSalesTrend(startDate.atStartOfDay());
        Map<String, SalesTrendVO> rowMap = new LinkedHashMap<>();
        if (rows != null) {
            for (SalesTrendVO row : rows) {
                if (row != null && row.getSaleDate() != null) {
                    rowMap.put(row.getSaleDate(), row);
                }
            }
        }
        // 补齐缺失日期，返回连续 dayCount 天
        List<SalesTrendVO> result = new ArrayList<>(dayCount);
        for (int i = 0; i < dayCount; i++) {
            String saleDate = startDate.plusDays(i).format(DATE_FORMATTER);
            SalesTrendVO row = rowMap.get(saleDate);
            if (row == null) {
                row = new SalesTrendVO();
                row.setSaleDate(saleDate);
                row.setOrderCount(0L);
                row.setTotalAmount(BigDecimal.ZERO);
            } else {
                if (row.getOrderCount() == null) {
                    row.setOrderCount(0L);
                }
                if (row.getTotalAmount() == null) {
                    row.setTotalAmount(BigDecimal.ZERO);
                }
            }
            result.add(row);
        }
        return result;
    }

    @Override
    public List<CategorySalesVO> categorySales() {
        List<CategorySalesVO> list = statsMapper.selectCategorySales();
        if (list == null) {
            return new ArrayList<>();
        }
        list.forEach(item -> {
            if (item.getSaleCount() == null) {
                item.setSaleCount(0L);
            }
            if (item.getTotalAmount() == null) {
                item.setTotalAmount(BigDecimal.ZERO);
            }
        });
        return list;
    }

    @Override
    public List<TopProductVO> topProducts(Integer limit) {
        int topLimit = limit == null || limit < 1 ? DEFAULT_TOP_LIMIT : Math.min(limit, MAX_TOP_LIMIT);
        List<TopProductVO> list = statsMapper.selectTopProducts(topLimit);
        if (list == null) {
            return new ArrayList<>();
        }
        list.forEach(item -> {
            if (item.getSaleCount() == null) {
                item.setSaleCount(0L);
            }
            if (item.getTotalAmount() == null) {
                item.setTotalAmount(BigDecimal.ZERO);
            }
        });
        return list;
    }

    @Override
    public List<OrderStatusCountVO> orderStatusCount() {
        List<OrderStatusCountVO> list = statsMapper.selectOrderStatusCount();
        if (list == null) {
            return new ArrayList<>();
        }
        list.forEach(item -> {
            if (item.getOrderCount() == null) {
                item.setOrderCount(0L);
            }
            if (item.getTotalAmount() == null) {
                item.setTotalAmount(BigDecimal.ZERO);
            }
            item.setStatusName(OrderStatus.descOf(item.getStatus()));
        });
        return list;
    }

    // ------------------------------------------------------------------ 内部方法

    /** 读取库存预警阈值，缺省 10 */
    private Integer resolveStockWarnThreshold() {
        SysConfig config = sysConfigMapper.selectOne(Wrappers.<SysConfig>lambdaQuery()
                .eq(SysConfig::getConfigKey, STOCK_WARN_THRESHOLD_KEY)
                .last("LIMIT 1"));
        if (config == null || config.getConfigValue() == null) {
            return DEFAULT_STOCK_WARN_THRESHOLD;
        }
        try {
            int value = Integer.parseInt(config.getConfigValue().trim());
            return value < 0 ? DEFAULT_STOCK_WARN_THRESHOLD : value;
        } catch (NumberFormatException e) {
            log.warn("系统配置 {} 值非法：{}，使用默认值 {}", STOCK_WARN_THRESHOLD_KEY, config.getConfigValue(),
                    DEFAULT_STOCK_WARN_THRESHOLD);
            return DEFAULT_STOCK_WARN_THRESHOLD;
        }
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long nullToZero(Long value) {
        return value == null ? 0L : value;
    }
}
