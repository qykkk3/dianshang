package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理端登录结果
 */
@Data
@Schema(description = "管理端登录结果")
public class AdminLoginVO {

    @Schema(description = "登录令牌")
    private String token;

    @Schema(description = "登录用户信息")
    private SysUserInfoVO userInfo;

    @Schema(description = "权限标识集合")
    private List<String> permissions = new ArrayList<>();

    @Schema(description = "菜单树")
    private List<MenuVO> menus = new ArrayList<>();
}
