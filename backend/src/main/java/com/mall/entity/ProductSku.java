package com.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品SKU表
 */
@Data
@TableName("product_sku")
public class ProductSku implements Serializable {

    private static final long serialVersionUID = 1L;

    /** SKU ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 商品ID */
    private Long productId;

    /** SKU编码 */
    private String skuCode;

    /** 规格键值对JSON 如 {"颜色":"曜石黑"} */
    private String specData;

    /** 规格展示文本 如 曜石黑 / 256G */
    private String specInfo;

    /** 售价 */
    private BigDecimal price;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 库存 */
    private Integer stock;

    /** 销量 */
    private Integer sales;

    /** SKU图片 */
    private String image;

    /** 状态 1启用 0停用 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
