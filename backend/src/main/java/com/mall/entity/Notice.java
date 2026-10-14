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
 * 公告表
 */
@Data
@TableName("notice")
public class Notice implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 公告ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 正文 */
    private String content;

    /** 类型 1平台公告 2促销活动 3帮助中心 */
    private Integer type;

    /** 状态 1已发布 0草稿 */
    private Integer status;

    /** 发布时间 */
    private LocalDateTime publishTime;

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
