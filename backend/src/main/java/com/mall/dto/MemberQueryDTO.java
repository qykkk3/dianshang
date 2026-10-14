package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 会员列表查询条件
 */
@Data
@Schema(description = "会员列表查询条件")
public class MemberQueryDTO {

    @Schema(description = "关键字：用户名 / 昵称 / 手机号，模糊匹配")
    private String keyword;

    @Schema(description = "状态 1正常 0禁用")
    private Integer status;

    @Schema(description = "页码，从 1 开始", defaultValue = "1")
    private Integer page = 1;

    @Schema(description = "每页条数", defaultValue = "10")
    private Integer size = 10;
}
