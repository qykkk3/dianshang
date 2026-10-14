package com.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品规格定义表
 */
@Data
@TableName("product_spec")
public class ProductSpec implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 规格ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 商品ID */
    private Long productId;

    /** 规格名 如 颜色 */
    private String specName;

    /** 规格值,逗号分隔 如 曜石黑,月光银 */
    private String specValues;

    /** 排序值 */
    private Integer sort;
}
