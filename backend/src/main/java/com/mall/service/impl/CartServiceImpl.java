package com.mall.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.BusinessException;
import com.mall.dto.CartAddDTO;
import com.mall.dto.CartBatchDTO;
import com.mall.dto.CartCheckedDTO;
import com.mall.dto.CartUpdateDTO;
import com.mall.entity.Cart;
import com.mall.entity.Product;
import com.mall.entity.ProductSku;
import com.mall.entity.ProductSpec;
import com.mall.mapper.CartMapper;
import com.mall.mapper.ProductMapper;
import com.mall.mapper.ProductSkuMapper;
import com.mall.mapper.ProductSpecMapper;
import com.mall.security.SecurityUtils;
import com.mall.service.CartService;
import com.mall.vo.CartVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 购物车服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductSpecMapper productSpecMapper;

    @Override
    public List<CartVO> list() {
        Long userId = SecurityUtils.getUserId();
        List<Cart> carts = cartMapper.selectList(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getUserId, userId)
                .orderByDesc(Cart::getCreateTime));
        if (carts.isEmpty()) {
            return new ArrayList<>();
        }
        // 批量加载商品与 SKU，避免 N+1
        Map<Long, Product> productMap = loadProducts(carts.stream()
                .map(Cart::getProductId).filter(java.util.Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, ProductSku> skuMap = loadSkus(carts.stream()
                .map(Cart::getSkuId).filter(java.util.Objects::nonNull).collect(Collectors.toSet()));

        return carts.stream().map(cart -> buildCartVO(cart,
                productMap.get(cart.getProductId()), skuMap.get(cart.getSkuId()))).collect(Collectors.toList());
    }

    @Override
    public Integer count() {
        Long userId = SecurityUtils.getUserId();
        Long count = cartMapper.selectCount(Wrappers.<Cart>lambdaQuery().eq(Cart::getUserId, userId));
        return count == null ? 0 : count.intValue();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(CartAddDTO dto) {
        Long userId = SecurityUtils.getUserId();
        // 商品必须上架
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException("商品不存在或已下架");
        }
        // SKU 必须属于该商品且启用
        ProductSku sku = productSkuMapper.selectById(dto.getSkuId());
        if (sku == null || !sku.getProductId().equals(product.getId())) {
            throw new BusinessException("商品规格不存在");
        }
        if (sku.getStatus() == null || sku.getStatus() != 1) {
            throw new BusinessException("该规格已停用，请选择其它规格");
        }
        // 同 SKU 已存在则累加数量
        Cart exist = cartMapper.selectOne(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getSkuId, sku.getId()));
        int finalQuantity = dto.getQuantity();
        if (exist != null && exist.getQuantity() != null) {
            finalQuantity += exist.getQuantity();
        }
        // 累加后不能超过库存
        if (sku.getStock() == null || finalQuantity > sku.getStock()) {
            throw new BusinessException("库存不足，当前可购买 " + (sku.getStock() == null ? 0 : sku.getStock()) + " 件");
        }
        if (exist != null) {
            Cart update = new Cart();
            update.setId(exist.getId());
            update.setQuantity(finalQuantity);
            update.setChecked(1);
            cartMapper.updateById(update);
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(product.getId());
            cart.setSkuId(sku.getId());
            cart.setQuantity(finalQuantity);
            cart.setChecked(1);
            cartMapper.insert(cart);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CartUpdateDTO dto) {
        Cart cart = requireOwned(id);
        ProductSku sku = productSkuMapper.selectById(cart.getSkuId());
        if (sku == null) {
            throw new BusinessException("商品规格不存在");
        }
        // 数量不能超过库存
        if (sku.getStock() == null || dto.getQuantity() > sku.getStock()) {
            throw new BusinessException("库存不足，当前可购买 " + (sku.getStock() == null ? 0 : sku.getStock()) + " 件");
        }
        Cart update = new Cart();
        update.setId(cart.getId());
        update.setQuantity(dto.getQuantity());
        if (dto.getChecked() != null) {
            update.setChecked(dto.getChecked() == 1 ? 1 : 0);
        }
        cartMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateChecked(CartCheckedDTO dto) {
        Long userId = SecurityUtils.getUserId();
        int checked = Integer.valueOf(1).equals(dto.getChecked()) ? 1 : 0;
        List<Long> ids = dto.getIds();
        if (ids != null && !ids.isEmpty()) {
            // 指定条目：校验归属后更新
            List<Cart> carts = cartMapper.selectBatchIds(ids);
            if (carts.size() != new HashSet<>(ids).size()
                    || carts.stream().anyMatch(c -> !c.getUserId().equals(userId))) {
                throw new BusinessException("购物车记录不存在或无权操作");
            }
        }
        Cart update = new Cart();
        update.setChecked(checked);
        cartMapper.update(update, Wrappers.<Cart>lambdaUpdate()
                .eq(Cart::getUserId, userId)
                .in(ids != null && !ids.isEmpty(), Cart::getId, ids));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        Cart cart = requireOwned(id);
        cartMapper.deleteById(cart.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(CartBatchDTO dto) {
        Long userId = SecurityUtils.getUserId();
        List<Long> ids = dto.getIds();
        List<Cart> carts = cartMapper.selectBatchIds(ids);
        if (carts.size() != new HashSet<>(ids).size()
                || carts.stream().anyMatch(c -> !c.getUserId().equals(userId))) {
            throw new BusinessException("购物车记录不存在或无权操作");
        }
        cartMapper.delete(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getUserId, userId)
                .in(Cart::getId, ids));
    }

    /* ==================== 内部方法 ==================== */

    /**
     * 查询购物车条目并校验归属
     */
    private Cart requireOwned(Long id) {
        Cart cart = id == null ? null : cartMapper.selectById(id);
        if (cart == null || !cart.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException("购物车记录不存在或无权操作");
        }
        return cart;
    }

    /**
     * 商品/SKU 是否有效：商品已上架、SKU 已启用、库存充足
     */
    private int resolveStatus(Product product, ProductSku sku, Integer quantity) {
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            return 0;
        }
        if (sku == null || sku.getStatus() == null || sku.getStatus() != 1) {
            return 0;
        }
        if (sku.getStock() == null || (quantity != null && quantity > sku.getStock())) {
            return 0;
        }
        return 1;
    }

    /**
     * 组装购物车 VO；价格取 SKU 售价，商品图片优先 SKU 图片
     */
    private CartVO buildCartVO(Cart cart, Product product, ProductSku sku) {
        CartVO vo = new CartVO();
        vo.setId(cart.getId());
        vo.setProductId(cart.getProductId());
        vo.setSkuId(cart.getSkuId());
        vo.setQuantity(cart.getQuantity());
        vo.setChecked(cart.getChecked());
        vo.setStatus(resolveStatus(product, sku, cart.getQuantity()));
        if (product != null) {
            vo.setProductName(product.getName());
            vo.setProductImage(product.getMainImage());
        }
        if (sku != null) {
            if (StringUtils.hasText(sku.getImage())) {
                vo.setProductImage(sku.getImage());
            }
            vo.setSpecInfo(sku.getSpecInfo());
            vo.setPrice(sku.getPrice());
            vo.setStock(sku.getStock());
        }
        if (vo.getStock() == null) {
            vo.setStock(0);
        }
        if (vo.getPrice() == null) {
            vo.setPrice(BigDecimal.ZERO);
        }
        return vo;
    }

    private Map<Long, Product> loadProducts(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Product> list = productMapper.selectBatchIds(new ArrayList<>(ids));
        return list.stream().collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a, HashMap::new));
    }

    private Map<Long, ProductSku> loadSkus(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ProductSku> list = productSkuMapper.selectBatchIds(new ArrayList<>(ids));
        return list.stream().collect(Collectors.toMap(ProductSku::getId, s -> s, (a, b) -> a, HashMap::new));
    }

    /**
     * 判断商品是否定义了多规格（保留给前端提示使用）
     */
    @SuppressWarnings("unused")
    private boolean hasMultiSpec(Long productId) {
        Long count = productSpecMapper.selectCount(Wrappers.<ProductSpec>lambdaQuery()
                .eq(ProductSpec::getProductId, productId));
        return count != null && count > 0;
    }
}
