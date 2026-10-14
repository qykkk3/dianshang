package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 商品规格保存参数
 */
@Data
@Schema(description = "商品规格保存参数")
public class SpecSaveDTO {

    @Schema(description = "规格ID，新增时为空")
    private Long id;

    @Schema(description = "规格名，如 颜色")
    private String specName;

    @Schema(description = "规格值列表，如 [\"曜石黑\",\"月光银\"]")
    private List<String> specValues;
}
