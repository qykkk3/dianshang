package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.BusinessException;
import com.mall.common.PageResult;
import com.mall.entity.Product;
import com.mall.entity.ProductSku;
import com.mall.entity.SysConfig;
import com.mall.mapper.ProductMapper;
import com.mall.mapper.ProductSkuMapper;
import com.mall.mapper.StatsMapper;
import com.mall.mapper.SysConfigMapper;
import com.mall.service.StockService;
import com.mall.vo.StockWarningVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 库存服务实现
 *
 * <p>库存预警阈值优先读取 sys_config.stock_warn_threshold，读不到时使用 mall.shop.stock-warn-threshold 配置值。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    /** sys_config 中库存预警阈值的配置键 */
    private static final String CONFIG_KEY = "stock_warn_threshold";
    /** 分页最大条数 */
    private static final int MAX_PAGE_SIZE = 200;

    private final StatsMapper statsMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final SysConfigMapper sysConfigMapper;

    @Value("${mall.shop.stock-warn-threshold:10}")
    private Integer defaultThreshold;

    @Override
    public PageResult<StockWarningVO> pageWarnings(String keyword, Integer page, Integer size) {
        long current = page == null || page < 1 ? 1L : page;
        long pageSize = size == null || size < 1 ? 10L : Math.min(size, MAX_PAGE_SIZE);
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Page<StockWarningVO> pageParam = new Page<>(current, pageSize);
        statsMapper.selectStockWarnings(pageParam, getThreshold(), kw);
        return PageResult.of(pageParam);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSkuStock(Long skuId, Integer stock) {
        if (skuId == null) {
            throw new BusinessException("SKU ID 不能为空");
        }
        if (stock == null || stock < 0) {
            throw new BusinessException("库存数量不能小于 0");
        }
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new BusinessException("SKU 不存在");
        }
        ProductSku update = new ProductSku();
        update.setId(skuId);
        update.setStock(stock);
        productSkuMapper.updateById(update);

        // 重算商品总库存 = 该商品所有 SKU 库存之和
        List<ProductSku> skus = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                .select(ProductSku::getStock)
                .eq(ProductSku::getProductId, sku.getProductId()));
        int total = skus.stream()
                .filter(item -> item.getStock() != null)
                .mapToInt(ProductSku::getStock)
                .sum();
        Product productUpdate = new Product();
        productUpdate.setId(sku.getProductId());
        productUpdate.setStock(total);
        productMapper.updateById(productUpdate);
    }

    @Override
    public Integer getThreshold() {
        SysConfig config = selectThresholdConfig();
        if (config != null && StringUtils.hasText(config.getConfigValue())) {
            try {
                int value = Integer.parseInt(config.getConfigValue().trim());
                if (value >= 0) {
                    return value;
                }
                log.warn("sys_config.{} 配置值不合法：{}，使用默认阈值", CONFIG_KEY, config.getConfigValue());
            } catch (NumberFormatException e) {
                log.warn("sys_config.{} 配置值不是数字：{}，使用默认阈值", CONFIG_KEY, config.getConfigValue());
            }
        }
        return defaultThreshold == null || defaultThreshold < 0 ? 10 : defaultThreshold;
    }

    @Override
    public void updateThreshold(Integer threshold) {
        if (threshold == null || threshold < 0) {
            throw new BusinessException("预警阈值不能小于 0");
        }
        SysConfig config = selectThresholdConfig();
        if (config == null) {
            SysConfig entity = new SysConfig();
            entity.setConfigKey(CONFIG_KEY);
            entity.setConfigName("库存预警阈值");
            entity.setConfigValue(String.valueOf(threshold));
            entity.setRemark("SKU 库存小于等于该值时进入库存预警");
            sysConfigMapper.insert(entity);
        } else {
            SysConfig entity = new SysConfig();
            entity.setId(config.getId());
            entity.setConfigValue(String.valueOf(threshold));
            sysConfigMapper.updateById(entity);
        }
    }

    private SysConfig selectThresholdConfig() {
        return sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, CONFIG_KEY)
                .last("LIMIT 1"));
    }
}
