package com.mall.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.BusinessException;
import com.mall.common.util.CommonUtils;
import com.mall.dto.AdminLoginDTO;
import com.mall.entity.SysPermission;
import com.mall.entity.SysRole;
import com.mall.entity.SysRolePermission;
import com.mall.entity.SysUser;
import com.mall.mapper.SysPermissionMapper;
import com.mall.mapper.SysRoleMapper;
import com.mall.mapper.SysRolePermissionMapper;
import com.mall.mapper.SysUserMapper;
import com.mall.security.LoginUser;
import com.mall.security.SecurityUtils;
import com.mall.security.TokenService;
import com.mall.service.AdminAuthService;
import com.mall.vo.AdminLoginVO;
import com.mall.vo.MenuVO;
import com.mall.vo.SysUserInfoVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端登录鉴权服务实现
 *
 * <p>登录成功后由 {@link TokenService} 写入 Redis，权限标识来自「角色-权限」关联表，
 * 菜单树只取该角色拥有的目录(type=1)与菜单(type=2)。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    /** 启用状态 */
    private static final int STATUS_ENABLED = 1;
    /** 权限类型：目录 */
    private static final int PERM_TYPE_DIR = 1;
    /** 权限类型：菜单 */
    private static final int PERM_TYPE_MENU = 2;

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AdminLoginVO login(AdminLoginDTO dto, HttpServletRequest request) {
        // 1. 查管理员（@TableLogic 自动过滤已删除）
        SysUser user = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, dto.getUsername())
                .last("LIMIT 1"));
        BusinessException.throwIf(user == null, "账号或密码错误");
        BusinessException.throwIf(!Integer.valueOf(STATUS_ENABLED).equals(user.getStatus()), "账号已被禁用，请联系管理员");
        BusinessException.throwIf(!passwordEncoder.matches(dto.getPassword(), user.getPassword()), "账号或密码错误");

        // 2. 角色
        SysRole role = sysRoleMapper.selectById(user.getRoleId());
        BusinessException.throwIf(role == null, "账号角色不存在，请联系管理员");
        BusinessException.throwIf(!Integer.valueOf(STATUS_ENABLED).equals(role.getStatus()), "账号角色已停用，请联系管理员");

        // 3. 权限标识 + 菜单树
        List<SysPermission> rolePermissions = listRolePermissions(role.getId());
        List<String> permissions = extractPermissions(rolePermissions);
        List<MenuVO> menus = buildMenuTree(rolePermissions);

        // 4. 构建登录用户并写入 Redis
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setNickname(user.getRealName());
        loginUser.setAvatar(user.getAvatar());
        loginUser.setUserType(LoginUser.TYPE_ADMIN);
        loginUser.setRoleId(role.getId());
        loginUser.setRoleCode(role.getRoleCode());
        loginUser.setRoleName(role.getRoleName());
        loginUser.setPermissions(permissions);
        String token = tokenService.createToken(loginUser);

        // 5. 记录登录时间与 IP
        LocalDateTime now = LocalDateTime.now();
        SysUser loginRecord = new SysUser();
        loginRecord.setId(user.getId());
        loginRecord.setLastLoginTime(now);
        loginRecord.setLastLoginIp(CommonUtils.getIp(request));
        sysUserMapper.updateById(loginRecord);

        AdminLoginVO vo = new AdminLoginVO();
        vo.setToken(token);
        vo.setUserInfo(buildUserInfo(user, role));
        vo.getUserInfo().setLastLoginTime(now);
        vo.setPermissions(permissions);
        vo.setMenus(menus);
        log.info("管理端登录成功：{}（角色 {}）", user.getUsername(), role.getRoleCode());
        return vo;
    }

    @Override
    public void logout() {
        tokenService.deleteToken(SecurityUtils.getToken());
    }

    @Override
    public AdminLoginVO getCurrentInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        // 重新查库：保证角色调整、权限变更、账号禁用即时生效
        SysUser user = sysUserMapper.selectById(loginUser.getUserId());
        BusinessException.throwIf(user == null, "登录账号不存在或已被删除");
        BusinessException.throwIf(!Integer.valueOf(STATUS_ENABLED).equals(user.getStatus()), "账号已被禁用，请联系管理员");
        SysRole role = sysRoleMapper.selectById(user.getRoleId());
        BusinessException.throwIf(role == null, "账号角色不存在，请联系管理员");

        List<SysPermission> rolePermissions = listRolePermissions(role.getId());
        AdminLoginVO vo = new AdminLoginVO();
        vo.setToken(loginUser.getToken());
        vo.setUserInfo(buildUserInfo(user, role));
        vo.setPermissions(extractPermissions(rolePermissions));
        vo.setMenus(buildMenuTree(rolePermissions));
        return vo;
    }

    @Override
    public List<MenuVO> listMenus() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = sysUserMapper.selectById(loginUser.getUserId());
        Long roleId = user == null ? loginUser.getRoleId() : user.getRoleId();
        return buildMenuTree(listRolePermissions(roleId));
    }

    // ------------------------------------------------------------------ 内部方法

    /**
     * 查询角色拥有的权限记录（sys_role_permission JOIN sys_permission 的等价实现）
     */
    private List<SysPermission> listRolePermissions(Long roleId) {
        if (roleId == null) {
            return new ArrayList<>();
        }
        List<SysRolePermission> relations = sysRolePermissionMapper.selectList(
                Wrappers.<SysRolePermission>lambdaQuery().eq(SysRolePermission::getRoleId, roleId));
        Set<Long> permissionIds = relations.stream()
                .map(SysRolePermission::getPermissionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (permissionIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<SysPermission> permissions = sysPermissionMapper.selectBatchIds(permissionIds);
        return permissions == null ? new ArrayList<>() : permissions;
    }

    /**
     * 提取权限标识，过滤 null / 空串
     */
    private List<String> extractPermissions(List<SysPermission> permissions) {
        return permissions.stream()
                .map(SysPermission::getPerms)
                .filter(perms -> perms != null && !perms.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 由权限记录构建菜单树：仅取启用的目录与菜单，按 sort 升序，父节点缺失时按顶级处理
     */
    private List<MenuVO> buildMenuTree(List<SysPermission> permissions) {
        List<SysPermission> menus = permissions.stream()
                .filter(p -> p.getType() != null
                        && (p.getType() == PERM_TYPE_DIR || p.getType() == PERM_TYPE_MENU)
                        && Integer.valueOf(STATUS_ENABLED).equals(p.getStatus()))
                .collect(Collectors.toList());
        Map<Long, MenuVO> nodeMap = new LinkedHashMap<>();
        for (SysPermission permission : menus) {
            nodeMap.put(permission.getId(), toMenuVO(permission));
        }
        List<MenuVO> roots = new ArrayList<>();
        for (SysPermission permission : menus) {
            MenuVO node = nodeMap.get(permission.getId());
            Long parentId = permission.getParentId();
            MenuVO parent = (parentId == null || parentId == 0L) ? null : nodeMap.get(parentId);
            if (parent == null || parent == node) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        sortMenus(roots, new HashSet<>());
        return roots;
    }

    /**
     * 按 sort、id 升序递归排序（visited 用于兜底脏数据形成的环）
     */
    private void sortMenus(List<MenuVO> nodes, Set<Long> visited) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        nodes.sort(Comparator.comparing(MenuVO::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(MenuVO::getId, Comparator.nullsLast(Long::compareTo)));
        for (MenuVO node : nodes) {
            if (node.getId() != null && !visited.add(node.getId())) {
                node.getChildren().clear();
                continue;
            }
            sortMenus(node.getChildren(), visited);
        }
    }

    private MenuVO toMenuVO(SysPermission permission) {
        MenuVO vo = new MenuVO();
        vo.setId(permission.getId());
        vo.setParentId(permission.getParentId());
        vo.setName(permission.getName());
        vo.setPath(permission.getPath());
        vo.setComponent(permission.getComponent());
        vo.setIcon(permission.getIcon());
        vo.setPerms(permission.getPerms());
        vo.setType(permission.getType());
        vo.setSort(permission.getSort());
        return vo;
    }

    private SysUserInfoVO buildUserInfo(SysUser user, SysRole role) {
        SysUserInfoVO info = new SysUserInfoVO();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setRealName(user.getRealName());
        info.setAvatar(user.getAvatar());
        info.setPhone(user.getPhone());
        info.setEmail(user.getEmail());
        info.setRoleId(user.getRoleId());
        info.setLastLoginTime(user.getLastLoginTime());
        if (role != null) {
            info.setRoleCode(role.getRoleCode());
            info.setRoleName(role.getRoleName());
        }
        return info;
    }
}
