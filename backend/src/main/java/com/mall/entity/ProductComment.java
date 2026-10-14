package com.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品评价表
 */
@Data
@TableName("product_comment")
public class ProductComment implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 评价ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 商品ID */
    private Long productId;

    /** SKU ID */
    private Long skuId;

    /** 订单ID */
    private Long orderId;

    /** 订单项ID */
    private Long orderItemId;

    /** 评价用户ID */
    private Long userId;

    /** 昵称(快照) */
    private String userNickname;

    /** 头像(快照) */
    private String userAvatar;

    /** 评分1-5 */
    private Integer star;

    /** 评价内容 */
    private String content;

    /** 晒图,逗号分隔 */
    private String images;

    /** 是否匿名 */
    private Integer isAnonymous;

    /** 商家回复 */
    private String replyContent;

    /** 回复人 */
    private String replyUser;

    /** 回复时间 */
    private LocalDateTime replyTime;

    /** 状态 1显示 0隐藏 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
