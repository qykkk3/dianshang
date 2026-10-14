package com.mall.service;

import com.mall.dto.CategorySaveDTO;
import com.mall.vo.CategoryVO;

import java.util.List;

/**
 * 商品分类服务
 */
public interface CategoryService {

    // ==================== 买家端 ====================

    /**
     * 买家端分类树：仅返回启用分类，两级结构，并填充商品数量
     */
    List<CategoryVO> listTree();

    /**
     * 查询单个分类（含启用状态的子分类）
     */
    CategoryVO getById(Long id);

    // ==================== 管理端 ====================

    /**
     * 管理端分类树：包含停用分类
     */
    List<CategoryVO> listAllTree();

    /**
     * 新增/编辑分类，level 由 parentId 推导
     */
    void save(CategorySaveDTO dto);

    /**
     * 删除分类：存在子分类或存在商品时拒绝
     */
    void delete(Long id);

    /**
     * 修改分类状态
     */
    void updateStatus(Long id, Integer status);
}
