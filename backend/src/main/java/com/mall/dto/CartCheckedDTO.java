package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 购物车勾选状态变更请求参数
 */
@Data
@Schema(description = "购物车勾选状态变更请求参数")
public class CartCheckedDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "购物车条目ID集合；为空表示对当前用户全部条目生效")
    private List<Long> ids;

    @Schema(description = "是否选中 1是 0否", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer checked;
}
