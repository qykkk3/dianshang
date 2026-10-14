package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员新增 / 编辑参数
 */
@Data
@Schema(description = "管理员新增/编辑参数")
public class SysUserSaveDTO {

    @Schema(description = "管理员ID，新增时为空，编辑时必传")
    private Long id;

    @Schema(description = "登录账号 4-20 位，全局唯一")
    @NotBlank(message = "登录账号不能为空")
    @Size(min = 4, max = 20, message = "登录账号长度需为 4-20 位")
    private String username;

    @Schema(description = "密码：新增时必填(6-20 位)；编辑时留空表示不修改密码")
    // 允许为空串：编辑时前端常回传空密码，表示「不修改密码」，具体必填校验在 Service 中完成
    @Pattern(regexp = "^$|^.{6,20}$", message = "密码长度需为 6-20 位")
    private String password;

    @Schema(description = "姓名")
    @Size(max = 50, message = "姓名长度不能超过 50 位")
    private String realName;

    @Schema(description = "头像地址")
    @Size(max = 255, message = "头像地址过长")
    private String avatar;

    @Schema(description = "手机号")
    @Size(max = 20, message = "手机号长度不能超过 20 位")
    private String phone;

    @Schema(description = "邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过 100 位")
    private String email;

    @Schema(description = "角色ID")
    @NotNull(message = "请选择所属角色")
    private Long roleId;

    @Schema(description = "状态 1正常 0禁用，新增时缺省为 1")
    private Integer status;
}
