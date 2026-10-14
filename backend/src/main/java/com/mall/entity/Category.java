package com.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品分类表
 */
@Data
@TableName("category")
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 分类ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 父分类ID,0为顶级 */
    private Long parentId;

    /** 分类名称 */
    private String name;

    /** 分类图标 */
    private String icon;

    /** 层级 1一级 2二级 */
    private Integer level;

    /** 排序值,越小越前 */
    private Integer sort;

    /** 状态 1启用 0停用 */
    private Integer status;

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
