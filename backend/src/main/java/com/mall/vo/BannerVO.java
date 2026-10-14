package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 首页轮播图
 */
@Data
@Schema(description = "轮播图")
public class BannerVO {

    @Schema(description = "轮播图ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "图片地址")
    private String image;

    @Schema(description = "跳转链接")
    private String linkUrl;

    @Schema(description = "位置 1商城首页 2分类页")
    private Integer position;

    @Schema(description = "排序值")
    private Integer sort;

    @Schema(description = "状态 1启用 0停用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
