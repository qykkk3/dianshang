package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.common.BusinessException;
import com.mall.dto.CategorySaveDTO;
import com.mall.entity.Category;
import com.mall.entity.Product;
import com.mall.mapper.CategoryMapper;
import com.mall.mapper.ProductMapper;
import com.mall.service.CategoryService;
import com.mall.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 商品分类服务实现
 *
 * <p>分类固定两级：parentId=0 为一级分类，其余为二级分类。</p>
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;

    @Override
    public List<CategoryVO> listTree() {
        // 买家端：仅启用分类，商品数只统计上架商品
        return buildTree(loadCategories(true), true);
    }

    @Override
    public List<CategoryVO> listAllTree() {
        // 管理端：含停用分类，商品数统计全部（未删除）商品，与「存在商品不可删除」规则一致
        return buildTree(loadCategories(false), false);
    }

    @Override
    public CategoryVO getById(Long id) {
        if (id == null) {
            throw new BusinessException("分类ID不能为空");
        }
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        Map<Long, Long> counts = loadProductCount(true);
        CategoryVO vo = toVO(category, counts);
        List<Category> children = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, id)
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
        long childCount = 0L;
        for (Category child : children) {
            vo.getChildren().add(toVO(child, counts));
            childCount += counts.getOrDefault(child.getId(), 0L);
        }
        // 一级分类的商品数为「自身 + 子分类」
        vo.setProductCount(counts.getOrDefault(id, 0L) + childCount);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(CategorySaveDTO dto) {
        if (dto == null) {
            throw new BusinessException("分类数据不能为空");
        }
        if (!StringUtils.hasText(dto.getName())) {
            throw new BusinessException("分类名称不能为空");
        }
        long parentId = dto.getParentId() == null ? 0L : dto.getParentId();
        if (parentId != 0L) {
            Category parent = categoryMapper.selectById(parentId);
            if (parent == null) {
                throw new BusinessException("父分类不存在");
            }
            if (parent.getParentId() != null && parent.getParentId() != 0L) {
                throw new BusinessException("仅支持两级分类，父分类必须是一级分类");
            }
        }

        boolean update = dto.getId() != null;
        Category entity = new Category();
        if (update) {
            Category exist = categoryMapper.selectById(dto.getId());
            if (exist == null) {
                throw new BusinessException("分类不存在");
            }
            if (parentId != 0L && Objects.equals(parentId, dto.getId())) {
                throw new BusinessException("父分类不能是自己");
            }
            if (parentId != 0L) {
                Long childCount = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                        .eq(Category::getParentId, dto.getId()));
                if (childCount != null && childCount > 0) {
                    throw new BusinessException("该分类下存在子分类，不能调整为二级分类");
                }
            }
            entity.setId(dto.getId());
        }
        entity.setParentId(parentId);
        entity.setLevel(parentId == 0L ? 1 : 2);
        entity.setName(dto.getName().trim());
        entity.setIcon(dto.getIcon());
        entity.setSort(dto.getSort() == null ? 0 : dto.getSort());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());

        if (update) {
            categoryMapper.updateById(entity);
        } else {
            categoryMapper.insert(entity);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id == null) {
            throw new BusinessException("分类ID不能为空");
        }
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        Long childCount = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException("该分类下存在子分类，请先删除子分类");
        }
        Long productCount = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getCategoryId, id));
        if (productCount != null && productCount > 0) {
            throw new BusinessException("该分类下存在商品，无法删除");
        }
        categoryMapper.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (id == null) {
            throw new BusinessException("分类ID不能为空");
        }
        if (status == null || (!Integer.valueOf(0).equals(status) && !Integer.valueOf(1).equals(status))) {
            throw new BusinessException("分类状态不合法");
        }
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        Category entity = new Category();
        entity.setId(id);
        entity.setStatus(status);
        categoryMapper.updateById(entity);
    }

    // ---------------------------------------------------------------- 内部方法

    private List<Category> loadCategories(boolean onlyEnabled) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId);
        if (onlyEnabled) {
            wrapper.eq(Category::getStatus, 1);
        }
        return categoryMapper.selectList(wrapper);
    }

    /**
     * 构建两级分类树，并填充商品数量。
     *
     * @param categories       分类集合
     * @param onlyOnShelf      商品数量是否只统计上架商品
     */
    private List<CategoryVO> buildTree(List<Category> categories, boolean onlyOnShelf) {
        Map<Long, Long> counts = loadProductCount(onlyOnShelf);
        Map<Long, List<Category>> childrenMap = categories.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() != 0L)
                .collect(Collectors.groupingBy(Category::getParentId));

        List<CategoryVO> roots = new ArrayList<>();
        for (Category category : categories) {
            if (category.getParentId() != null && category.getParentId() != 0L) {
                continue;
            }
            CategoryVO vo = toVO(category, counts);
            long childCount = 0L;
            for (Category child : childrenMap.getOrDefault(category.getId(), List.of())) {
                vo.getChildren().add(toVO(child, counts));
                childCount += counts.getOrDefault(child.getId(), 0L);
            }
            vo.setProductCount(counts.getOrDefault(category.getId(), 0L) + childCount);
            roots.add(vo);
        }
        return roots;
    }

    /**
     * 统计每个分类下的商品数量
     */
    private Map<Long, Long> loadProductCount(boolean onlyOnShelf) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .select(Product::getCategoryId);
        if (onlyOnShelf) {
            wrapper.eq(Product::getStatus, 1);
        }
        List<Product> products = productMapper.selectList(wrapper);
        Map<Long, Long> counts = new HashMap<>();
        for (Product product : products) {
            if (product.getCategoryId() != null) {
                counts.merge(product.getCategoryId(), 1L, Long::sum);
            }
        }
        return counts;
    }

    private CategoryVO toVO(Category category, Map<Long, Long> counts) {
        CategoryVO vo = new CategoryVO();
        vo.setId(category.getId());
        vo.setParentId(category.getParentId());
        vo.setName(category.getName());
        vo.setIcon(category.getIcon());
        vo.setLevel(category.getLevel());
        vo.setSort(category.getSort());
        vo.setStatus(category.getStatus());
        vo.setProductCount(counts.getOrDefault(category.getId(), 0L));
        vo.setChildren(new ArrayList<>());
        return vo;
    }
}
