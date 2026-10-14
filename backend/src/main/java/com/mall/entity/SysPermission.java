package com.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 权限菜单表
 */
@Data
@TableName("sys_permission")
public class SysPermission implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 权限ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 父级ID,0为顶级 */
    private Long parentId;

    /** 名称 */
    private String name;

    /** 权限标识 如 product:list */
    private String perms;

    /** 类型 1目录 2菜单 3按钮 */
    private Integer type;

    /** 前端路由路径 */
    private String path;

    /** 前端组件路径 */
    private String component;

    /** 图标 */
    private String icon;

    /** 排序值 */
    private Integer sort;

    /** 状态 1启用 0停用 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
