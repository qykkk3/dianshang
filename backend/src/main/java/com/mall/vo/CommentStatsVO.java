package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 商品评价统计
 */
@Data
@Schema(description = "商品评价统计")
public class CommentStatsVO {

    @Schema(description = "平均评分，保留 1 位小数")
    private Double avgStar = 0.0;

    @Schema(description = "评价总数")
    private Long total = 0L;

    @Schema(description = "各星级数量，键为 1-5")
    private Map<Integer, Long> starCount = new LinkedHashMap<>();
}
