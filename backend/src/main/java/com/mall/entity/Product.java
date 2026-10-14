package com.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品SPU表
 */
@Data
@TableName("product")
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 分类ID */
    private Long categoryId;

    /** 商品名称 */
    private String name;

    /** 副标题/卖点 */
    private String subtitle;

    /** 品牌 */
    private String brand;

    /** 单位 */
    private String unit;

    /** 主图 */
    private String mainImage;

    /** 轮播图,逗号分隔 */
    private String images;

    /** 图文详情(HTML) */
    private String detail;

    /** 最低售价(SPU展示价) */
    private BigDecimal price;

    /** 最低原价 */
    private BigDecimal originalPrice;

    /** 成本价 */
    private BigDecimal costPrice;

    /** 总库存(SKU汇总) */
    private Integer stock;

    /** 累计销量 */
    private Integer sales;

    /** 库存预警阈值 */
    private Integer warnStock;

    /** 状态 1上架 0下架 */
    private Integer status;

    /** 是否推荐 */
    private Integer isRecommend;

    /** 是否新品 */
    private Integer isNew;

    /** 是否热销 */
    private Integer isHot;

    /** 排序值 */
    private Integer sort;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除 0未删 1已删 */
    @TableLogic
    private Integer deleted;
}
