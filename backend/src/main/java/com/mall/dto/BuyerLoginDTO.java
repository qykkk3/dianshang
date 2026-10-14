package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 买家登录请求参数
 */
@Data
@Schema(description = "买家登录请求参数")
public class BuyerLoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "登录账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "zhangsan")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "登录密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;
}
