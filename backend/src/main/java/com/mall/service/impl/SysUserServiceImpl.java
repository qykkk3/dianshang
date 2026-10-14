package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.BusinessException;
import com.mall.common.PageResult;
import com.mall.dto.PasswordResetDTO;
import com.mall.dto.SysUserQueryDTO;
import com.mall.dto.SysUserSaveDTO;
import com.mall.entity.SysRole;
import com.mall.entity.SysUser;
import com.mall.mapper.SysRoleMapper;
import com.mall.mapper.SysUserMapper;
import com.mall.security.LoginUser;
import com.mall.security.SecurityUtils;
import com.mall.security.TokenService;
import com.mall.service.SysRoleService;
import com.mall.service.SysUserService;
import com.mall.vo.SysRoleVO;
import com.mall.vo.SysUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 后台管理员服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    /** 内置超级管理员ID：不允许删除、不允许禁用 */
    private static final Long SUPER_ADMIN_ID = 1L;

    /** 默认状态：正常 */
    private static final int STATUS_ENABLED = 1;

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysRoleService sysRoleService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Override
    public PageResult<SysUserVO> page(SysUserQueryDTO query) {
        SysUserQueryDTO dto = query == null ? new SysUserQueryDTO() : query;
        long current = dto.getPage() == null || dto.getPage() < 1 ? 1L : dto.getPage();
        long size = dto.getSize() == null || dto.getSize() < 1 ? 10L : dto.getSize();
        Page<SysUser> page = new Page<>(current, size);

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dto.getKeyword())) {
            String keyword = dto.getKeyword().trim();
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        wrapper.eq(dto.getStatus() != null, SysUser::getStatus, dto.getStatus());
        wrapper.eq(dto.getRoleId() != null, SysUser::getRoleId, dto.getRoleId());
        wrapper.orderByAsc(SysUser::getId);
        sysUserMapper.selectPage(page, wrapper);

        // 一次性查出角色信息，避免逐条查询角色
        Map<Long, SysRole> roleMap = loadRoleMap(page.getRecords().stream()
                .map(SysUser::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        return PageResult.of(page, user -> toVO(user, roleMap));
    }

    @Override
    public SysUserVO getById(Long id) {
        SysUser user = requireUser(id);
        Map<Long, SysRole> roleMap = loadRoleMap(
                user.getRoleId() == null ? Collections.emptyList() : Collections.singletonList(user.getRoleId()));
        return toVO(user, roleMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(SysUserSaveDTO dto) {
        if (!StringUtils.hasText(dto.getUsername())) {
            throw new BusinessException("登录账号不能为空");
        }
        requireRole(dto.getRoleId());
        if (dto.getId() == null) {
            // ---------------- 新增 ----------------
            if (existsUsername(dto.getUsername(), null)) {
                throw new BusinessException("登录账号已存在：" + dto.getUsername());
            }
            if (!StringUtils.hasText(dto.getPassword())) {
                throw new BusinessException("新增管理员必须设置初始密码");
            }
            SysUser entity = new SysUser();
            entity.setUsername(dto.getUsername().trim());
            entity.setPassword(passwordEncoder.encode(dto.getPassword()));
            entity.setRealName(dto.getRealName());
            entity.setAvatar(dto.getAvatar());
            entity.setPhone(dto.getPhone());
            entity.setEmail(dto.getEmail());
            entity.setRoleId(dto.getRoleId());
            entity.setStatus(dto.getStatus() == null ? STATUS_ENABLED : dto.getStatus());
            sysUserMapper.insert(entity);
            return;
        }
        // ---------------- 编辑 ----------------
        SysUser existing = requireUser(dto.getId());
        if (existsUsername(dto.getUsername(), dto.getId())) {
            throw new BusinessException("登录账号已被其它管理员使用：" + dto.getUsername());
        }
        int targetStatus = dto.getStatus() == null
                ? (existing.getStatus() == null ? STATUS_ENABLED : existing.getStatus())
                : dto.getStatus();
        if (targetStatus == 0) {
            // 保护超级管理员与当前登录账号，禁止通过编辑表单把自己/超管禁掉
            checkNotSuperAdmin(dto.getId(), "超级管理员账号不允许禁用");
            checkNotSelf(dto.getId(), "不能禁用当前登录的管理员账号");
        }
        boolean roleChanged = !Objects.equals(existing.getRoleId(), dto.getRoleId());

        SysUser entity = new SysUser();
        entity.setId(dto.getId());
        entity.setUsername(dto.getUsername().trim());
        entity.setRealName(dto.getRealName());
        entity.setAvatar(dto.getAvatar());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        entity.setRoleId(dto.getRoleId());
        entity.setStatus(targetStatus);
        // 密码留空表示不修改密码
        if (StringUtils.hasText(dto.getPassword())) {
            entity.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        sysUserMapper.updateById(entity);

        // 角色变更或账号被禁用时，强制其重新登录
        if (roleChanged || targetStatus == 0) {
            kickOut(dto.getId(), roleChanged ? "角色变更" : "账号被禁用");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        checkNotSuperAdmin(id, "超级管理员账号不允许删除");
        checkNotSelf(id, "不能删除当前登录的管理员账号");
        requireUser(id);
        // sys_user 含逻辑删除字段，此处由 MyBatis-Plus 转为逻辑删除
        sysUserMapper.deleteById(id);
        kickOut(id, "账号被删除");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态值不合法，只能为 1（正常）或 0（禁用）");
        }
        if (status == 0) {
            checkNotSuperAdmin(id, "超级管理员账号不允许禁用");
            checkNotSelf(id, "不能禁用当前登录的管理员账号");
        }
        requireUser(id);
        SysUser entity = new SysUser();
        entity.setId(id);
        entity.setStatus(status);
        sysUserMapper.updateById(entity);
        if (status == 0) {
            kickOut(id, "账号被禁用");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, PasswordResetDTO dto) {
        requireUser(id);
        SysUser entity = new SysUser();
        entity.setId(id);
        entity.setPassword(passwordEncoder.encode(dto.getPassword()));
        sysUserMapper.updateById(entity);
        kickOut(id, "密码被重置");
    }

    @Override
    public List<SysRoleVO> listAllRoles() {
        // 供管理员表单的角色下拉使用，直接复用角色列表（含 userCount）
        return sysRoleService.list();
    }

    /* ------------------------------------------------------------------ 内部方法 */

    private SysUser requireUser(Long id) {
        if (id == null) {
            throw new BusinessException("管理员ID不能为空");
        }
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("管理员不存在或已被删除");
        }
        return user;
    }

    private void requireRole(Long roleId) {
        if (roleId == null) {
            throw new BusinessException("请选择所属角色");
        }
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("所选角色不存在，请重新选择");
        }
    }

    private boolean existsUsername(String username, Long excludeId) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username.trim())
                .ne(excludeId != null, SysUser::getId, excludeId);
        Long count = sysUserMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    /** 禁止操作内置超级管理员 */
    private void checkNotSuperAdmin(Long id, String message) {
        if (SUPER_ADMIN_ID.equals(id)) {
            throw new BusinessException(message);
        }
    }

    /** 禁止操作当前登录的管理员本人 */
    private void checkNotSelf(Long id, String message) {
        Long currentUserId = SecurityUtils.getUserId();
        if (Objects.equals(currentUserId, id)) {
            throw new BusinessException(message);
        }
    }

    /** 强制管理员下线（Redis 登录态失效，Token 立即不可用） */
    private void kickOut(Long userId, String reason) {
        tokenService.kickOut(LoginUser.TYPE_ADMIN, userId);
        log.info("管理员[{}]{}，已强制下线", userId, reason);
    }

    private Map<Long, SysRole> loadRoleMap(Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
        return roles.stream().collect(Collectors.toMap(SysRole::getId, Function.identity(), (a, b) -> a));
    }

    private SysUserVO toVO(SysUser user, Map<Long, SysRole> roleMap) {
        SysUserVO vo = new SysUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setRoleId(user.getRoleId());
        vo.setStatus(user.getStatus());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setLastLoginIp(user.getLastLoginIp());
        vo.setCreateTime(user.getCreateTime());
        SysRole role = user.getRoleId() == null ? null : roleMap.get(user.getRoleId());
        if (role != null) {
            vo.setRoleName(role.getRoleName());
            vo.setRoleCode(role.getRoleCode());
        }
        return vo;
    }
}
