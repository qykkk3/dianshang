package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品评价
 */
@Data
@Schema(description = "商品评价")
public class CommentVO {

    @Schema(description = "评价ID")
    private Long id;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品主图")
    private String productImage;

    @Schema(description = "SKU ID")
    private Long skuId;

    @Schema(description = "规格展示文本")
    private String specInfo;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "评价用户ID")
    private Long userId;

    @Schema(description = "评价人昵称（匿名评价显示「匿名用户」）")
    private String userNickname;

    @Schema(description = "评价人头像")
    private String userAvatar;

    @Schema(description = "评分 1-5")
    private Integer star;

    @Schema(description = "评价内容")
    private String content;

    @Schema(description = "晒图地址集合")
    private List<String> images = new ArrayList<>();

    @Schema(description = "是否匿名 0否 1是")
    private Integer isAnonymous;

    @Schema(description = "商家回复")
    private String replyContent;

    @Schema(description = "回复人")
    private String replyUser;

    @Schema(description = "回复时间")
    private LocalDateTime replyTime;

    @Schema(description = "状态 1显示 0隐藏")
    private Integer status;

    @Schema(description = "评价时间")
    private LocalDateTime createTime;
}
