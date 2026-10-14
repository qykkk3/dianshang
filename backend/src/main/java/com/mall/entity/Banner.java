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
 * 首页轮播图表
 */
@Data
@TableName("banner")
public class Banner implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 轮播图ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 图片地址 */
    private String image;

    /** 跳转链接 */
    private String linkUrl;

    /** 位置 1商城首页 2分类页 */
    private Integer position;

    /** 排序值 */
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
