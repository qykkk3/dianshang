package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品规格
 */
@Data
@Schema(description = "商品规格")
public class SpecVO {

    @Schema(description = "规格ID")
    private Long id;

    @Schema(description = "规格名，如 颜色")
    private String specName;

    @Schema(description = "规格值列表")
    private List<String> specValues = new ArrayList<>();
}
