package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 买家登录结果
 */
@Data
@Schema(description = "买家登录结果")
public class BuyerLoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "访问令牌")
    private String token;

    @Schema(description = "用户信息")
    private UserInfoVO userInfo;
}
