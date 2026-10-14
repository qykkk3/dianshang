package com.mall.controller.admin;

import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.common.annotation.OperLog;
import com.mall.dto.StockConfigDTO;
import com.mall.dto.StockUpdateDTO;
import com.mall.service.StockService;
import com.mall.vo.StockWarningVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端库存接口
 */
@Tag(name = "管理端-库存管理")
@RestController
@RequestMapping("/api/admin/stock")
@RequiredArgsConstructor
public class AdminStockController {

    private final StockService stockService;

    @Operation(summary = "库存预警分页查询")
    @GetMapping("/warnings")
    @PreAuthorize("hasAuthority('stock:list')")
    public Result<PageResult<StockWarningVO>> warnings(@RequestParam(value = "keyword", required = false) String keyword,
                                                      @RequestParam(value = "page", defaultValue = "1") Integer page,
                                                      @RequestParam(value = "size", defaultValue = "10") Integer size) {
        return Result.success(stockService.pageWarnings(keyword, page, size));
    }

    @Operation(summary = "调整 SKU 库存")
    @PutMapping("/{skuId}")
    @PreAuthorize("hasAuthority('stock:edit')")
    @OperLog(module = "库存管理", value = "调整库存")
    public Result<Void> updateStock(@PathVariable Long skuId, @Valid @RequestBody StockUpdateDTO dto) {
        stockService.updateSkuStock(skuId, dto.getStock());
        return Result.success("库存调整成功", null);
    }

    @Operation(summary = "查询库存预警阈值")
    @GetMapping("/config")
    @PreAuthorize("hasAuthority('stock:list')")
    public Result<StockConfigDTO> config() {
        StockConfigDTO dto = new StockConfigDTO();
        dto.setThreshold(stockService.getThreshold());
        return Result.success(dto);
    }

    @Operation(summary = "修改库存预警阈值")
    @PutMapping("/config")
    @PreAuthorize("hasAuthority('stock:edit')")
    @OperLog(module = "库存管理", value = "修改库存预警阈值")
    public Result<Void> updateConfig(@Valid @RequestBody StockConfigDTO dto) {
        stockService.updateThreshold(dto.getThreshold());
        return Result.success("修改成功", null);
    }
}
