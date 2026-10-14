package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 买家个人资料修改参数
 *
 * <p>字段为 null 表示不修改；birthday 为字符串（yyyy-MM-dd），可为空。</p>
 */
@Data
@Schema(description = "个人资料修改参数")
public class ProfileUpdateDTO {

    @Schema(description = "昵称")
    @Size(max = 50, message = "昵称长度不能超过 50 个字符")
    private String nickname;

    @Schema(description = "手机号")
    @Size(max = 20, message = "手机号长度不能超过 20 个字符")
    private String phone;

    @Schema(description = "邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过 100 个字符")
    private String email;

    @Schema(description = "头像地址")
    @Size(max = 255, message = "头像地址过长")
    private String avatar;

    @Schema(description = "性别 0未知 1男 2女")
    @Min(value = 0, message = "性别取值不合法")
    @Max(value = 2, message = "性别取值不合法")
    private Integer gender;

    @Schema(description = "生日 yyyy-MM-dd，可为空")
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "生日格式应为 yyyy-MM-dd")
    private String birthday;
}
