package com.mall.controller.admin;

import com.mall.common.Result;
import com.mall.common.annotation.OperLog;
import com.mall.dto.CategorySaveDTO;
import com.mall.service.CategoryService;
import com.mall.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端商品分类接口
 */
@Tag(name = "管理端-商品分类")
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "分类树（含停用分类）")
    @GetMapping
    @PreAuthorize("hasAuthority('category:list')")
    public Result<List<CategoryVO>> listAllTree() {
        return Result.success(categoryService.listAllTree());
    }

    @Operation(summary = "新增分类")
    @PostMapping
    @PreAuthorize("hasAuthority('category:add')")
    @OperLog(module = "商品管理", value = "新增分类")
    public Result<Void> save(@Valid @RequestBody CategorySaveDTO dto) {
        dto.setId(null);
        categoryService.save(dto);
        return Result.success("新增成功", null);
    }

    @Operation(summary = "编辑分类")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('category:edit')")
    @OperLog(module = "商品管理", value = "编辑分类")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CategorySaveDTO dto) {
        dto.setId(id);
        categoryService.save(dto);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('category:delete')")
    @OperLog(module = "商品管理", value = "删除分类")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "修改分类状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('category:edit')")
    @OperLog(module = "商品管理", value = "修改分类状态")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam("status") Integer status) {
        categoryService.updateStatus(id, status);
        return Result.success("修改成功", null);
    }
}
