package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 买家修改密码参数
 */
@Data
@Schema(description = "修改密码参数")
public class PasswordUpdateDTO {

    @Schema(description = "原密码")
    @NotBlank(message = "请输入原密码")
    private String oldPassword;

    @Schema(description = "新密码，6-20 位")
    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 20, message = "新密码长度需为 6-20 位")
    private String newPassword;
}
