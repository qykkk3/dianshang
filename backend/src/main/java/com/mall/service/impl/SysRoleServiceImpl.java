package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.common.BusinessException;
import com.mall.dto.RolePermissionDTO;
import com.mall.dto.SysRoleSaveDTO;
import com.mall.entity.SysPermission;
import com.mall.entity.SysRole;
import com.mall.entity.SysRolePermission;
import com.mall.entity.SysUser;
import com.mall.mapper.SysPermissionMapper;
import com.mall.mapper.SysRoleMapper;
import com.mall.mapper.SysRolePermissionMapper;
import com.mall.mapper.SysUserMapper;
import com.mall.security.LoginUser;
import com.mall.security.TokenService;
import com.mall.service.SysRoleService;
import com.mall.vo.SysPermissionVO;
import com.mall.vo.SysRoleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色与权限服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    /** 内置角色编码：不允许删除，且不允许修改角色编码 */
    public static final Set<String> BUILT_IN_ROLE_CODES = Set.of("ADMIN", "OPERATOR", "WAREHOUSE");

    private final SysRoleMapper sysRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysUserMapper sysUserMapper;
    private final TokenService tokenService;

    @Override
    public List<SysRoleVO> list() {
        List<SysRole> roles = sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
        if (roles == null || roles.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> roleIds = roles.stream().map(SysRole::getId).collect(Collectors.toList());
        // 一次性查出全部角色权限与管理员数量，避免逐条查询
        Map<Long, List<Long>> permissionMap = loadPermissionIds(roleIds);
        Map<Long, Long> userCountMap = loadUserCount(roleIds);
        return roles.stream()
                .map(role -> toVO(role,
                        permissionMap.getOrDefault(role.getId(), new ArrayList<>()),
                        userCountMap.getOrDefault(role.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    public SysRoleVO getById(Long id) {
        SysRole role = requireRole(id);
        List<Long> permissionIds = loadPermissionIds(Collections.singletonList(id))
                .getOrDefault(id, new ArrayList<>());
        Long userCount = loadUserCount(Collections.singletonList(id)).getOrDefault(id, 0L);
        return toVO(role, permissionIds, userCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(SysRoleSaveDTO dto) {
        // 统一转大写，避免前端传入小写导致唯一校验失效
        String roleCode = dto.getRoleCode() == null ? null : dto.getRoleCode().trim().toUpperCase();
        if (dto.getId() == null) {
            // 新增：roleCode 唯一校验
            if (existsRoleCode(roleCode, null)) {
                throw new BusinessException("角色编码已存在：" + roleCode);
            }
            SysRole entity = new SysRole();
            entity.setRoleCode(roleCode);
            entity.setRoleName(dto.getRoleName());
            entity.setDescription(dto.getDescription());
            entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
            sysRoleMapper.insert(entity);
            return;
        }
        // 编辑
        SysRole existing = requireRole(dto.getId());
        if (BUILT_IN_ROLE_CODES.contains(existing.getRoleCode())
                && !Objects.equals(existing.getRoleCode(), roleCode)) {
            throw new BusinessException("内置角色不允许修改角色编码");
        }
        if (existsRoleCode(roleCode, dto.getId())) {
            throw new BusinessException("角色编码已存在：" + roleCode);
        }
        SysRole entity = new SysRole();
        entity.setId(dto.getId());
        entity.setRoleCode(roleCode);
        entity.setRoleName(dto.getRoleName());
        entity.setDescription(dto.getDescription());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        sysRoleMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRole role = requireRole(id);
        if (BUILT_IN_ROLE_CODES.contains(role.getRoleCode())) {
            throw new BusinessException("内置角色不允许删除");
        }
        Long userCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, id));
        if (userCount != null && userCount > 0) {
            throw new BusinessException("该角色下仍有 " + userCount + " 个管理员，请先调整其角色后再删除");
        }
        // sys_role / sys_role_permission 均无逻辑删除字段，此处为物理删除
        sysRolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
        sysRoleMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态值不合法，只能为 1（启用）或 0（停用）");
        }
        requireRole(id);
        SysRole entity = new SysRole();
        entity.setId(id);
        entity.setStatus(status);
        sysRoleMapper.updateById(entity);
        // 停用角色时强制该角色下的管理员下线，避免其凭旧 Token 继续操作
        if (status == 0) {
            kickOutRoleUsers(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long id, RolePermissionDTO dto) {
        requireRole(id);
        // 先删除该角色的全部权限关联，再批量插入（全量覆盖）
        sysRolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
        List<Long> permissionIds = dto == null ? null : dto.getPermissionIds();
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<Long> distinctIds = permissionIds.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            for (Long permissionId : distinctIds) {
                SysRolePermission relation = new SysRolePermission();
                relation.setRoleId(id);
                relation.setPermissionId(permissionId);
                sysRolePermissionMapper.insert(relation);
            }
        }
        // 权限变更后，该角色下的管理员全部强制下线，重新登录以加载最新权限
        kickOutRoleUsers(id);
    }

    @Override
    public List<SysPermissionVO> listAllPermissions() {
        // 先按 sort 升序取全部权限，再在内存中组装树（同级顺序即 sort 顺序）
        List<SysPermission> permissions = sysPermissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                        .orderByAsc(SysPermission::getSort)
                        .orderByAsc(SysPermission::getId));
        Map<Long, SysPermissionVO> voMap = new LinkedHashMap<>();
        for (SysPermission permission : permissions) {
            voMap.put(permission.getId(), toVO(permission));
        }
        List<SysPermissionVO> tree = new ArrayList<>();
        for (SysPermission permission : permissions) {
            SysPermissionVO vo = voMap.get(permission.getId());
            SysPermissionVO parent = permission.getParentId() == null ? null : voMap.get(permission.getParentId());
            if (parent == null) {
                // parentId = 0 或父节点缺失时作为顶级节点
                tree.add(vo);
            } else {
                parent.getChildren().add(vo);
            }
        }
        return tree;
    }

    /* ------------------------------------------------------------------ 内部方法 */

    private SysRole requireRole(Long id) {
        if (id == null) {
            throw new BusinessException("角色ID不能为空");
        }
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在或已被删除");
        }
        return role;
    }

    private boolean existsRoleCode(String roleCode, Long excludeId) {
        if (!StringUtils.hasText(roleCode)) {
            return false;
        }
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode)
                .ne(excludeId != null, SysRole::getId, excludeId);
        Long count = sysRoleMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    /** 查询多个角色的权限ID集合 */
    private Map<Long, List<Long>> loadPermissionIds(Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysRolePermission> relations = sysRolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>().in(SysRolePermission::getRoleId, roleIds));
        return relations.stream().collect(Collectors.groupingBy(SysRolePermission::getRoleId,
                Collectors.mapping(SysRolePermission::getPermissionId, Collectors.toList())));
    }

    /** 统计每个角色下的管理员数量（sys_user 逻辑删除条件由 MyBatis-Plus 自动追加） */
    private Map<Long, Long> loadUserCount(Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.select("role_id", "COUNT(*) AS cnt")
                .in("role_id", roleIds)
                .groupBy("role_id");
        List<Map<String, Object>> rows = sysUserMapper.selectMaps(wrapper);
        Map<Long, Long> result = new HashMap<>();
        if (rows == null) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            Long roleId = toLong(pick(row, "role_id", "roleId"));
            Long count = toLong(pick(row, "cnt", "count"));
            if (roleId != null) {
                result.put(roleId, count == null ? 0L : count);
            }
        }
        return result;
    }

    /** 该角色下的管理员全部强制下线 */
    private void kickOutRoleUsers(Long roleId) {
        List<SysUser> users = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, roleId));
        for (SysUser user : users) {
            tokenService.kickOut(LoginUser.TYPE_ADMIN, user.getId());
            log.info("角色[{}]权限变更，管理员[{}]已被强制下线", roleId, user.getUsername());
        }
    }

    private SysRoleVO toVO(SysRole role, List<Long> permissionIds, Long userCount) {
        SysRoleVO vo = new SysRoleVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setDescription(role.getDescription());
        vo.setStatus(role.getStatus());
        vo.setCreateTime(role.getCreateTime());
        vo.setPermissionIds(permissionIds == null ? new ArrayList<>() : permissionIds);
        vo.setUserCount(userCount == null ? 0L : userCount);
        return vo;
    }

    private SysPermissionVO toVO(SysPermission permission) {
        SysPermissionVO vo = new SysPermissionVO();
        vo.setId(permission.getId());
        vo.setParentId(permission.getParentId());
        vo.setName(permission.getName());
        vo.setPerms(permission.getPerms());
        vo.setType(permission.getType());
        vo.setPath(permission.getPath());
        vo.setComponent(permission.getComponent());
        vo.setIcon(permission.getIcon());
        vo.setSort(permission.getSort());
        vo.setStatus(permission.getStatus());
        return vo;
    }

    /** 从 selectMaps 结果中按候选键（忽略大小写）取值 */
    private static Object pick(Map<String, Object> row, String... keys) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        for (String key : keys) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
