package com.mall.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.vo.CategorySalesVO;
import com.mall.vo.OrderStatusCountVO;
import com.mall.vo.SalesTrendVO;
import com.mall.vo.StockWarningVO;
import com.mall.vo.TopProductVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 统计与报表 Mapper（数据看板、库存预警）
 *
 * <p>销售额口径：已支付订单（状态 1待发货 / 2待收货 / 3已完成 / 5售后中）。</p>
 */
@Mapper
public interface StatsMapper {

    /**
     * 每日销售趋势
     */
    @Select("""
            SELECT DATE_FORMAT(pay_time, '%Y-%m-%d') AS sale_date,
                   COUNT(*)                          AS order_count,
                   IFNULL(ROUND(SUM(pay_amount), 2), 0) AS total_amount
            FROM orders
            WHERE pay_time IS NOT NULL
              AND status IN (1, 2, 3, 5)
              AND pay_time >= #{startTime}
            GROUP BY DATE_FORMAT(pay_time, '%Y-%m-%d')
            ORDER BY sale_date
            """)
    List<SalesTrendVO> selectSalesTrend(@Param("startTime") LocalDateTime startTime);

    /**
     * 分类销售统计
     */
    @Select("""
            SELECT c.id                                AS category_id,
                   c.name                              AS category_name,
                   IFNULL(SUM(oi.quantity), 0)         AS sale_count,
                   IFNULL(ROUND(SUM(oi.total_amount), 2), 0) AS total_amount
            FROM order_item oi
                     JOIN orders o ON oi.order_id = o.id
                     JOIN product p ON oi.product_id = p.id
                     JOIN category c ON p.category_id = c.id
            WHERE o.status IN (1, 2, 3, 5)
            GROUP BY c.id, c.name
            ORDER BY total_amount DESC
            """)
    List<CategorySalesVO> selectCategorySales();

    /**
     * 热销商品排行
     */
    @Select("""
            SELECT oi.product_id                      AS product_id,
                   oi.product_name                    AS product_name,
                   IFNULL(SUM(oi.quantity), 0)         AS sale_count,
                   IFNULL(ROUND(SUM(oi.total_amount), 2), 0) AS total_amount
            FROM order_item oi
                     JOIN orders o ON oi.order_id = o.id
            WHERE o.status IN (1, 2, 3, 5)
            GROUP BY oi.product_id, oi.product_name
            ORDER BY sale_count DESC
            LIMIT #{limit}
            """)
    List<TopProductVO> selectTopProducts(@Param("limit") Integer limit);

    /**
     * 订单状态分布
     */
    @Select("""
            SELECT status,
                   COUNT(*)                             AS order_count,
                   IFNULL(ROUND(SUM(pay_amount), 2), 0) AS total_amount
            FROM orders
            GROUP BY status
            ORDER BY status
            """)
    List<OrderStatusCountVO> selectOrderStatusCount();

    /**
     * 库存预警分页查询（SKU 维度）
     *
     * <p>预警判定：SKU 库存 &lt;= 商品自身预警阈值 {@code product.warn_stock}；
     * 若商品未单独设置（warn_stock 为 0 或 NULL），则使用传入的全局阈值。</p>
     */
    @Select("""
            <script>
            SELECT s.id            AS sku_id,
                   s.product_id    AS product_id,
                   p.name          AS product_name,
                   s.sku_code      AS sku_code,
                   s.spec_info     AS spec_info,
                   p.main_image    AS main_image,
                   s.stock         AS stock,
                   p.warn_stock    AS warn_stock,
                   p.status        AS product_status
            FROM product_sku s
                     JOIN product p ON s.product_id = p.id
            WHERE p.deleted = 0
              AND s.stock &lt;= (CASE WHEN p.warn_stock IS NULL OR p.warn_stock &lt;= 0 THEN #{threshold} ELSE p.warn_stock END)
              <if test="keyword != null and keyword != ''">
                  AND (p.name LIKE CONCAT('%', #{keyword}, '%') OR s.sku_code LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            ORDER BY s.stock ASC, s.id ASC
            </script>
            """)
    IPage<StockWarningVO> selectStockWarnings(IPage<StockWarningVO> page,
                                             @Param("threshold") Integer threshold,
                                             @Param("keyword") String keyword);

    /**
     * 库存预警数量（判定规则同 {@link #selectStockWarnings}）
     */
    @Select("""
            SELECT COUNT(*)
            FROM product_sku s
                     JOIN product p ON s.product_id = p.id
            WHERE p.deleted = 0
              AND s.stock <= (CASE WHEN p.warn_stock IS NULL OR p.warn_stock <= 0 THEN #{threshold} ELSE p.warn_stock END)
            """)
    Long countStockWarnings(@Param("threshold") Integer threshold);

    /**
     * 指定时间段销售额
     */
    @Select("""
            SELECT IFNULL(ROUND(SUM(pay_amount), 2), 0)
            FROM orders
            WHERE pay_time >= #{start} AND pay_time < #{end} AND status IN (1, 2, 3, 5)
            """)
    BigDecimal sumSalesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 指定时间段订单数（按创建时间）
     */
    @Select("SELECT COUNT(*) FROM orders WHERE create_time >= #{start} AND create_time < #{end}")
    Long countOrdersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
