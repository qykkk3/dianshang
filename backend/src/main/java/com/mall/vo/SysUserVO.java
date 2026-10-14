package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员信息
 */
@Data
@Schema(description = "管理员信息")
public class SysUserVO {

    @Schema(description = "管理员ID")
    private Long id;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "姓名")
    private String realName;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色编码 ADMIN/OPERATOR/WAREHOUSE")
    private String roleCode;

    @Schema(description = "状态 1正常 0禁用")
    private Integer status;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "最后登录IP")
    private String lastLoginIp;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
