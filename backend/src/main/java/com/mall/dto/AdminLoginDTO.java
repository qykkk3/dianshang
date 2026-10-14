package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理端登录参数
 */
@Data
@Schema(description = "管理端登录参数")
public class AdminLoginDTO {

    @Schema(description = "登录账号")
    @NotBlank(message = "请输入登录账号")
    private String username;

    @Schema(description = "登录密码")
    @NotBlank(message = "请输入登录密码")
    private String password;
}
