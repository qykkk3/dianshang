package com.mall.service;

import com.mall.common.PageResult;
import com.mall.vo.StockWarningVO;

/**
 * 库存服务
 */
public interface StockService {

    /**
     * 库存预警分页（SKU 维度），阈值取 sys_config.stock_warn_threshold
     */
    PageResult<StockWarningVO> pageWarnings(String keyword, Integer page, Integer size);

    /**
     * 调整 SKU 库存，并重算所属商品的总库存
     */
    void updateSkuStock(Long skuId, Integer stock);

    /**
     * 读取库存预警阈值：优先 sys_config，读不到用配置默认值
     */
    Integer getThreshold();

    /**
     * 修改库存预警阈值（写 sys_config，不存在则插入）
     */
    void updateThreshold(Integer threshold);
}
