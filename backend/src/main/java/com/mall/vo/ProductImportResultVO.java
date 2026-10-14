package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品 Excel 导入结果
 */
@Data
@Schema(description = "商品Excel导入结果")
public class ProductImportResultVO {

    @Schema(description = "导入成功商品数")
    private Integer successCount = 0;

    @Schema(description = "导入失败商品数")
    private Integer failCount = 0;

    @Schema(description = "错误明细（含出错行号与原因）")
    private List<String> errors = new ArrayList<>();

    /** 成功 +1 */
    public void addSuccess() {
        this.successCount = (this.successCount == null ? 0 : this.successCount) + 1;
    }

    /** 失败 +1 */
    public void addFail() {
        this.failCount = (this.failCount == null ? 0 : this.failCount) + 1;
    }

    /** 追加一条错误说明 */
    public void addError(String message) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(message);
    }
}
