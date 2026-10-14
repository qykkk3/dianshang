package com.mall.service;

import com.mall.dto.BuyerLoginDTO;
import com.mall.dto.RegisterDTO;
import com.mall.vo.BuyerLoginVO;
import com.mall.vo.UserInfoVO;

/**
 * 买家认证服务
 */
public interface AuthService {

    /**
     * 买家注册
     *
     * @param dto 注册参数
     */
    void register(RegisterDTO dto);

    /**
     * 买家登录
     *
     * @param dto 登录参数
     * @return Token 与用户信息
     */
    BuyerLoginVO login(BuyerLoginDTO dto);

    /**
     * 退出登录
     */
    void logout();

    /**
     * 获取当前登录买家信息
     *
     * @return 用户信息
     */
    UserInfoVO getCurrentUserInfo();
}
