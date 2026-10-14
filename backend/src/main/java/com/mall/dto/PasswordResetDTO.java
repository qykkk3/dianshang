package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员密码重置参数
 */
@Data
@Schema(description = "管理员密码重置参数")
public class PasswordResetDTO {

    @Schema(description = "新密码 6-20 位")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需为 6-20 位")
    private String password;
}
