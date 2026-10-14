package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员信息（列表）
 */
@Data
@Schema(description = "会员信息")
public class MemberVO {

    @Schema(description = "会员ID")
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

    @Schema(description = "状态 1正常 0禁用")
    private Integer status;

    @Schema(description = "有效订单数（待发货/待收货/已完成/售后中）")
    private Long orderCount = 0L;

    @Schema(description = "累计消费金额")
    private BigDecimal totalConsumption = BigDecimal.ZERO;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "注册时间")
    private LocalDateTime createTime;
}
