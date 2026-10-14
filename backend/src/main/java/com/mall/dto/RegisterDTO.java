package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 买家注册请求参数
 */
@Data
@Schema(description = "买家注册请求参数")
public class RegisterDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "登录账号，4-20 个字符", requiredMode = Schema.RequiredMode.REQUIRED, example = "zhangsan")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度必须为 4-20 个字符")
    private String username;

    @Schema(description = "登录密码，6-20 个字符", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须为 6-20 个字符")
    private String password;

    @Schema(description = "昵称，为空时默认取用户名", example = "张三")
    @Size(max = 50, message = "昵称长度不能超过 50 个字符")
    private String nickname;

    @Schema(description = "手机号，可选；填写后必须为 11 位手机号", example = "13800138001")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "邮箱，可选", example = "zhangsan@mall.com")
    @Email(message = "邮箱格式不正确")
    private String email;
}
