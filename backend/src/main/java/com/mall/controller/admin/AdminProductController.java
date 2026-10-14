package com.mall.controller.admin;

import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.common.annotation.OperLog;
import com.mall.dto.ProductQueryDTO;
import com.mall.dto.ProductSaveDTO;
import com.mall.service.ProductService;
import com.mall.vo.ProductDetailVO;
import com.mall.vo.ProductImportResultVO;
import com.mall.vo.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理端商品接口
 */
@Tag(name = "管理端-商品管理")
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @Operation(summary = "商品分页查询（含下架商品）")
    @GetMapping
    @PreAuthorize("hasAuthority('product:list')")
    public Result<PageResult<ProductVO>> page(@Valid @ModelAttribute ProductQueryDTO query) {
        return Result.success(productService.pageAdminProducts(query));
    }

    @Operation(summary = "商品详情（含停用 SKU）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('product:list')")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        return Result.success(productService.getAdminDetail(id));
    }

    @Operation(summary = "新增商品")
    @PostMapping
    @PreAuthorize("hasAuthority('product:add')")
    @OperLog(module = "商品管理", value = "新增商品")
    public Result<Long> save(@Valid @RequestBody ProductSaveDTO dto) {
        dto.setId(null);
        return Result.success("新增成功", productService.save(dto));
    }

    @Operation(summary = "编辑商品")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:edit')")
    @OperLog(module = "商品管理", value = "编辑商品")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProductSaveDTO dto) {
        dto.setId(id);
        productService.save(dto);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:delete')")
    @OperLog(module = "商品管理", value = "删除商品")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "修改商品上下架状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('product:status')")
    @OperLog(module = "商品管理", value = "修改商品上下架")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam("status") Integer status) {
        productService.updateStatus(id, status);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "修改商品推荐标记")
    @PutMapping("/{id}/recommend")
    @PreAuthorize("hasAuthority('product:edit')")
    @OperLog(module = "商品管理", value = "修改商品推荐")
    public Result<Void> updateRecommend(@PathVariable Long id,
                                        @RequestParam("isRecommend") Integer isRecommend) {
        productService.updateRecommend(id, isRecommend);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "导出商品 Excel")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('product:export')")
    public void export(HttpServletResponse response, @Valid @ModelAttribute ProductQueryDTO query) {
        productService.exportExcel(response, query);
    }

    @Operation(summary = "下载商品导入模板")
    @GetMapping("/template")
    @PreAuthorize("hasAuthority('product:import')")
    public void template(HttpServletResponse response) {
        productService.exportTemplate(response);
    }

    @Operation(summary = "导入商品 Excel")
    @PostMapping("/import")
    @PreAuthorize("hasAuthority('product:import')")
    @OperLog(module = "商品管理", value = "导入商品", saveParams = false)
    public Result<ProductImportResultVO> importExcel(@RequestParam("file") MultipartFile file) {
        return Result.success("导入完成", productService.importExcel(file));
    }
}
