package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 买家个人资料
 */
@Data
@Schema(description = "买家个人资料")
public class UserProfileVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "性别 0未知 1男 2女")
    private Integer gender;

    @Schema(description = "生日")
    private LocalDate birthday;

    @Schema(description = "注册时间")
    private LocalDateTime createTime;
}
