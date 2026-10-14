package com.mall.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.BusinessException;
import com.mall.dto.BuyerLoginDTO;
import com.mall.dto.RegisterDTO;
import com.mall.entity.User;
import com.mall.mapper.UserMapper;
import com.mall.security.LoginUser;
import com.mall.security.SecurityUtils;
import com.mall.security.TokenService;
import com.mall.service.AuthService;
import com.mall.vo.BuyerLoginVO;
import com.mall.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 买家认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO dto) {
        String username = dto.getUsername() == null ? null : dto.getUsername().trim();
        // 1. 用户名唯一校验
        Long exist = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (exist != null && exist > 0) {
            throw new BusinessException("用户名已存在，请更换后重试");
        }
        // 2. 组装用户，密码 BCrypt 加密
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        // 默认昵称 = 用户名
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname().trim() : username);
        user.setPhone(StringUtils.hasText(dto.getPhone()) ? dto.getPhone().trim() : null);
        user.setEmail(StringUtils.hasText(dto.getEmail()) ? dto.getEmail().trim() : null);
        user.setGender(0);
        user.setStatus(1);
        userMapper.insert(user);
        log.info("买家注册成功：username={}, userId={}", username, user.getId());
    }

    @Override
    public BuyerLoginVO login(BuyerLoginDTO dto) {
        // 1. 用户存在
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, dto.getUsername() == null ? null : dto.getUsername().trim()));
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        // 2. 密码正确
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        // 3. 账号可用
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用，请联系客服");
        }
        // 4. 构建登录态（买家权限由 SecurityConfig 依据 TYPE_BUYER 控制，无需具体权限标识）
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        loginUser.setAvatar(user.getAvatar());
        loginUser.setUserType(LoginUser.TYPE_BUYER);
        String token = tokenService.createToken(loginUser);

        // 5. 更新最后登录时间
        User update = new User();
        update.setId(user.getId());
        update.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(update);

        BuyerLoginVO vo = new BuyerLoginVO();
        vo.setToken(token);
        vo.setUserInfo(toUserInfoVO(user));
        return vo;
    }

    @Override
    public void logout() {
        String token = SecurityUtils.getToken();
        if (StringUtils.hasText(token)) {
            tokenService.deleteToken(token);
        }
    }

    @Override
    public UserInfoVO getCurrentUserInfo() {
        Long userId = SecurityUtils.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在或已注销");
        }
        return toUserInfoVO(user);
    }

    /**
     * 实体 → 用户信息 VO
     */
    private UserInfoVO toUserInfoVO(User user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setBirthday(user.getBirthday());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
