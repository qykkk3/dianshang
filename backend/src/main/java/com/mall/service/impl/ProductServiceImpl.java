package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.BusinessException;
import com.mall.common.PageResult;
import com.mall.common.util.ExcelUtils;
import com.mall.dto.ProductQueryDTO;
import com.mall.dto.ProductSaveDTO;
import com.mall.dto.SkuSaveDTO;
import com.mall.dto.SpecSaveDTO;
import com.mall.entity.Category;
import com.mall.entity.Favorite;
import com.mall.entity.OrderItem;
import com.mall.entity.Orders;
import com.mall.entity.Product;
import com.mall.entity.ProductComment;
import com.mall.entity.ProductSku;
import com.mall.entity.ProductSpec;
import com.mall.mapper.CategoryMapper;
import com.mall.mapper.FavoriteMapper;
import com.mall.mapper.OrderItemMapper;
import com.mall.mapper.OrdersMapper;
import com.mall.mapper.ProductCommentMapper;
import com.mall.mapper.ProductMapper;
import com.mall.mapper.ProductSkuMapper;
import com.mall.mapper.ProductSpecMapper;
import com.mall.service.ProductService;
import com.mall.vo.ProductDetailVO;
import com.mall.vo.ProductImportResultVO;
import com.mall.vo.ProductVO;
import com.mall.vo.SkuVO;
import com.mall.vo.SpecVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商品服务实现
 *
 * <p>核心规则：</p>
 * <ul>
 *   <li>SPU 的 price / originalPrice 取所有 SKU 的最低价，stock 取所有 SKU 库存之和；</li>
 *   <li>SKU 的 specInfo 优先使用前端传入值，为空时由 specData 的 value 以 " / " 拼接；</li>
 *   <li>更新商品时：带 id 的 SKU 执行更新，不在列表中的旧 SKU 直接删除，无 id 的 SKU 执行新增；</li>
 *   <li>规格采用「先删该商品全部规格再插入」的简单可靠策略。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    /** 导出最大条数 */
    private static final int EXPORT_MAX = 5000;
    /** 导入最大行数（含表头） */
    private static final int MAX_IMPORT_ROWS = 5000;
    /** 分页最大条数 */
    private static final int MAX_PAGE_SIZE = 200;
    /** 轮播图列表最大长度（product.images 为 VARCHAR(2000)） */
    private static final int MAX_IMAGES_LENGTH = 2000;
    /** 规格 JSON 最大长度（product_sku.spec_data 为 VARCHAR(500)） */
    private static final int MAX_SPEC_DATA_LENGTH = 500;
    /** 规格展示文本最大长度（product_sku.spec_info 为 VARCHAR(200)） */
    private static final int MAX_SPEC_INFO_LENGTH = 200;
    /** 未完成订单状态：待付款 / 待发货 / 待收货 / 售后中 */
    private static final List<Integer> UNFINISHED_ORDER_STATUS = List.of(0, 1, 2, 5);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 导出表头 */
    private static final String[] EXPORT_HEADERS = {
            "商品ID", "商品名称", "分类", "品牌", "价格", "原价", "总库存", "销量", "状态", "是否推荐", "创建时间"
    };
    /** 导入模板表头 */
    private static final String[] TEMPLATE_HEADERS = {
            "商品名称", "副标题", "品牌", "分类名称", "单位",
            "SKU规格(如 颜色:红色;尺码:M)", "SKU价格", "SKU原价", "SKU库存", "SKU编码"
    };

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductSpecMapper productSpecMapper;
    private final CategoryMapper categoryMapper;
    private final FavoriteMapper favoriteMapper;
    private final ProductCommentMapper productCommentMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrdersMapper ordersMapper;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Value("${mall.shop.stock-warn-threshold:10}")
    private Integer defaultWarnStock;

    // ================================================================ 买家端

    @Override
    public PageResult<ProductVO> pageShopProducts(ProductQueryDTO query) {
        ProductQueryDTO condition = query == null ? new ProductQueryDTO() : query;
        Page<Product> page = new Page<>(normalizePage(condition.getPage()), normalizeSize(condition.getSize()));
        productMapper.selectPage(page, buildWrapper(condition, true));
        return toProductPage(page, true);
    }

    @Override
    public ProductDetailVO getShopDetail(Long id, Long loginUserId) {
        Product product = selectProduct(id);
        if (!Integer.valueOf(1).equals(product.getStatus())) {
            throw new BusinessException("商品已下架");
        }
        return buildDetail(product, true, loginUserId);
    }

    @Override
    public List<ProductVO> listHot(Integer limit) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .eq(Product::getIsHot, 1)
                .orderByDesc(Product::getSales)
                .orderByDesc(Product::getId)
                .last("LIMIT " + normalizeLimit(limit));
        return toProductList(productMapper.selectList(wrapper), true);
    }

    @Override
    public List<ProductVO> listNew(Integer limit) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .orderByDesc(Product::getCreateTime)
                .orderByDesc(Product::getId)
                .last("LIMIT " + normalizeLimit(limit));
        return toProductList(productMapper.selectList(wrapper), true);
    }

    @Override
    public List<ProductVO> listRecommend(Integer limit) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .eq(Product::getIsRecommend, 1)
                .orderByAsc(Product::getSort)
                .orderByDesc(Product::getId)
                .last("LIMIT " + normalizeLimit(limit));
        return toProductList(productMapper.selectList(wrapper), true);
    }

    // ================================================================ 管理端

    @Override
    public PageResult<ProductVO> pageAdminProducts(ProductQueryDTO query) {
        ProductQueryDTO condition = query == null ? new ProductQueryDTO() : query;
        Page<Product> page = new Page<>(normalizePage(condition.getPage()), normalizeSize(condition.getSize()));
        productMapper.selectPage(page, buildWrapper(condition, false));
        return toProductPage(page, false);
    }

    @Override
    public ProductDetailVO getAdminDetail(Long id) {
        Product product = selectProduct(id);
        return buildDetail(product, false, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(ProductSaveDTO dto) {
        if (dto == null) {
            throw new BusinessException("商品数据不能为空");
        }
        if (dto.getCategoryId() == null) {
            throw new BusinessException("请选择商品分类");
        }
        if (!StringUtils.hasText(dto.getName())) {
            throw new BusinessException("商品名称不能为空");
        }
        Category category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null) {
            throw new BusinessException("商品分类不存在");
        }
        if (dto.getSkus() == null || dto.getSkus().isEmpty()) {
            throw new BusinessException("请至少添加一个 SKU");
        }

        boolean update = dto.getId() != null;
        Product product = new Product();
        if (update) {
            Product exist = productMapper.selectById(dto.getId());
            if (exist == null) {
                throw new BusinessException("商品不存在");
            }
            product.setId(exist.getId());
        }

        // ---------- 组装 SKU，同时计算 SPU 的价格与库存 ----------
        List<ProductSku> skuEntities = new ArrayList<>();
        BigDecimal minPrice = null;
        BigDecimal minOriginalPrice = null;
        int totalStock = 0;
        for (SkuSaveDTO skuDTO : dto.getSkus()) {
            if (skuDTO == null) {
                continue;
            }
            BigDecimal price = skuDTO.getPrice() == null ? BigDecimal.ZERO : skuDTO.getPrice();
            BigDecimal originalPrice = skuDTO.getOriginalPrice() == null ? price : skuDTO.getOriginalPrice();
            int stock = skuDTO.getStock() == null ? 0 : Math.max(skuDTO.getStock(), 0);
            String specData = normalizeSpecData(skuDTO.getSpecData());
            String specInfo = resolveSpecInfo(skuDTO.getSpecInfo(), specData);

            ProductSku entity = new ProductSku();
            entity.setId(skuDTO.getId());
            entity.setSkuCode(skuDTO.getSkuCode());
            entity.setSpecData(specData);
            entity.setSpecInfo(specInfo);
            entity.setPrice(price);
            entity.setOriginalPrice(originalPrice);
            entity.setStock(stock);
            entity.setImage(skuDTO.getImage());
            entity.setStatus(skuDTO.getStatus() == null ? 1 : resolveFlag(skuDTO.getStatus(), 1, "SKU 状态"));

            skuEntities.add(entity);
            totalStock += stock;
            minPrice = min(minPrice, price);
            minOriginalPrice = min(minOriginalPrice, originalPrice);
        }
        if (skuEntities.isEmpty()) {
            throw new BusinessException("请至少添加一个 SKU");
        }

        // ---------- SPU 字段 ----------
        product.setCategoryId(dto.getCategoryId());
        product.setName(dto.getName().trim());
        product.setPrice(minPrice == null ? BigDecimal.ZERO : minPrice);
        product.setOriginalPrice(minOriginalPrice == null ? BigDecimal.ZERO : minOriginalPrice);
        product.setStock(totalStock);
        product.setSubtitle(dto.getSubtitle());
        product.setBrand(dto.getBrand());
        product.setMainImage(dto.getMainImage());
        product.setDetail(dto.getDetail());
        if (dto.getImages() != null) {
            product.setImages(joinImages(dto.getImages()));
        }
        if (update) {
            if (StringUtils.hasText(dto.getUnit())) {
                product.setUnit(dto.getUnit().trim());
            }
            if (dto.getStatus() != null) {
                product.setStatus(resolveFlag(dto.getStatus(), 1, "商品状态"));
            }
            if (dto.getIsRecommend() != null) {
                product.setIsRecommend(resolveFlag(dto.getIsRecommend(), 0, "推荐标记"));
            }
            if (dto.getIsNew() != null) {
                product.setIsNew(resolveFlag(dto.getIsNew(), 0, "新品标记"));
            }
            if (dto.getIsHot() != null) {
                product.setIsHot(resolveFlag(dto.getIsHot(), 0, "热销标记"));
            }
            if (dto.getSort() != null) {
                product.setSort(dto.getSort());
            }
            if (dto.getWarnStock() != null) {
                product.setWarnStock(dto.getWarnStock());
            }
            if (dto.getCostPrice() != null) {
                product.setCostPrice(dto.getCostPrice());
            }
            productMapper.updateById(product);
        } else {
            product.setUnit(StringUtils.hasText(dto.getUnit()) ? dto.getUnit().trim() : "件");
            product.setStatus(resolveFlag(dto.getStatus(), 1, "商品状态"));
            product.setIsRecommend(resolveFlag(dto.getIsRecommend(), 0, "推荐标记"));
            product.setIsNew(resolveFlag(dto.getIsNew(), 0, "新品标记"));
            product.setIsHot(resolveFlag(dto.getIsHot(), 0, "热销标记"));
            product.setSort(dto.getSort() == null ? 0 : dto.getSort());
            product.setWarnStock(dto.getWarnStock() == null ? defaultWarnStockValue() : dto.getWarnStock());
            product.setCostPrice(dto.getCostPrice() == null ? BigDecimal.ZERO : dto.getCostPrice());
            product.setSales(0);
            productMapper.insert(product);
        }
        Long productId = product.getId();

        // ---------- 规格：先删后插 ----------
        productSpecMapper.delete(new LambdaQueryWrapper<ProductSpec>()
                .eq(ProductSpec::getProductId, productId));
        if (dto.getSpecs() != null) {
            int sort = 1;
            for (SpecSaveDTO specDTO : dto.getSpecs()) {
                if (specDTO == null || !StringUtils.hasText(specDTO.getSpecName())) {
                    continue;
                }
                ProductSpec spec = new ProductSpec();
                spec.setProductId(productId);
                spec.setSpecName(specDTO.getSpecName().trim());
                spec.setSpecValues(joinSpecValues(specDTO.getSpecValues()));
                spec.setSort(sort++);
                productSpecMapper.insert(spec);
            }
        }

        // ---------- SKU：新增 / 更新 / 删除 ----------
        List<ProductSku> existing = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                .select(ProductSku::getId)
                .eq(ProductSku::getProductId, productId));
        Set<Long> existingIds = existing.stream()
                .map(ProductSku::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        Set<Long> keepIds = new HashSet<>();
        for (ProductSku entity : skuEntities) {
            if (entity.getId() == null) {
                entity.setProductId(productId);
                productSkuMapper.insert(entity);
                if (entity.getId() != null) {
                    keepIds.add(entity.getId());
                }
                continue;
            }
            // 防止把其它商品的 SKU 挂到当前商品上
            if (!existingIds.contains(entity.getId())) {
                throw new BusinessException("SKU 不存在或不属于当前商品：" + entity.getId());
            }
            entity.setProductId(productId);
            productSkuMapper.updateById(entity);
            keepIds.add(entity.getId());
        }
        for (Long oldId : existingIds) {
            if (!keepIds.contains(oldId)) {
                productSkuMapper.deleteById(oldId);
            }
        }
        return productId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Product product = selectProduct(id);
        // 存在未完成订单（0待付款 1待发货 2待收货 5售后中）时拒绝删除
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .select(OrderItem::getOrderId)
                .eq(OrderItem::getProductId, product.getId()));
        List<Long> orderIds = items.stream()
                .map(OrderItem::getOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (!orderIds.isEmpty()) {
            Long count = ordersMapper.selectCount(new LambdaQueryWrapper<Orders>()
                    .in(Orders::getId, orderIds)
                    .in(Orders::getStatus, UNFINISHED_ORDER_STATUS));
            if (count != null && count > 0) {
                throw new BusinessException("该商品存在未完成订单，无法删除");
            }
        }
        productSkuMapper.delete(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, product.getId()));
        productSpecMapper.delete(new LambdaQueryWrapper<ProductSpec>().eq(ProductSpec::getProductId, product.getId()));
        // 逻辑删除商品
        productMapper.deleteById(product.getId());
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Product product = selectProduct(id);
        Product update = new Product();
        update.setId(product.getId());
        update.setStatus(resolveFlag(status, null, "商品状态"));
        productMapper.updateById(update);
    }

    @Override
    public void updateRecommend(Long id, Integer isRecommend) {
        Product product = selectProduct(id);
        Product update = new Product();
        update.setId(product.getId());
        update.setIsRecommend(resolveFlag(isRecommend, null, "推荐标记"));
        productMapper.updateById(update);
    }

    @Override
    public void exportExcel(HttpServletResponse response, ProductQueryDTO query) {
        ProductQueryDTO condition = query == null ? new ProductQueryDTO() : query;
        LambdaQueryWrapper<Product> wrapper = buildWrapper(condition, false);
        wrapper.orderByAsc(Product::getId);
        wrapper.last("LIMIT " + EXPORT_MAX);
        List<Product> products = productMapper.selectList(wrapper);
        Map<Long, String> categoryNames = loadCategoryNames(products.stream()
                .map(Product::getCategoryId)
                .collect(Collectors.toList()));

        List<Object[]> rows = new ArrayList<>(products.size());
        for (Product product : products) {
            rows.add(new Object[]{
                    product.getId(),
                    product.getName(),
                    categoryNames.getOrDefault(product.getCategoryId(), ""),
                    product.getBrand(),
                    product.getPrice(),
                    product.getOriginalPrice(),
                    product.getStock(),
                    product.getSales(),
                    Integer.valueOf(1).equals(product.getStatus()) ? "上架" : "下架",
                    Integer.valueOf(1).equals(product.getIsRecommend()) ? "是" : "否",
                    product.getCreateTime() == null ? "" : DATE_TIME_FORMATTER.format(product.getCreateTime())
            });
        }
        String fileName = "商品列表_" + LocalDateTime.now().format(FILE_NAME_FORMATTER);
        ExcelUtils.export(response, fileName, "商品列表", EXPORT_HEADERS, rows);
    }

    @Override
    public void exportTemplate(HttpServletResponse response) {
        String[] tips = {
                "商品导入模板填写说明：",
                "1、同一商品名称填写多行，导入后自动合并为一个商品（SPU），每一行对应一个 SKU；",
                "2、商品名称为必填项，不能为空；",
                "3、分类名称必须与系统中已存在的商品分类名称完全一致，否则该商品导入失败；",
                "4、「SKU规格」格式为 规格名:规格值，多个规格之间使用英文分号(;)分隔，例如：颜色:红色;尺码:M；无规格商品可留空；",
                "5、SKU价格、SKU原价、SKU库存必须填写数字，价格最多两位小数，库存为不小于 0 的整数；",
                "6、SKU编码可留空；",
                "7、导入成功的商品默认为上架状态，单位为「件」（单位列留空时），不参与推荐、非新品、非热销；",
                "8、单行填写有误只影响该商品，其它商品仍会正常导入，错误明细随导入结果一起返回。"
        };
        ExcelUtils.exportTemplate(response, "商品导入模板", "商品", TEMPLATE_HEADERS, tips);
    }

    @Override
    public ProductImportResultVO importExcel(MultipartFile file) {
        ProductImportResultVO result = new ProductImportResultVO();
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要导入的 Excel 文件");
        }
        String filename = file.getOriginalFilename();
        String lowerName = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".xlsx") && !lowerName.endsWith(".xls")) {
            throw new BusinessException("仅支持 .xlsx / .xls 格式的 Excel 文件");
        }

        // 商品名称 -> 该商品的所有 SKU 行（保持模板中的先后顺序）
        Map<String, List<ImportRow>> groups = new LinkedHashMap<>();
        try (InputStream in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BusinessException("Excel 文件中没有工作表");
            }
            int lastRowNum = sheet.getLastRowNum();
            int maxRow = Math.min(lastRowNum, MAX_IMPORT_ROWS);
            for (int i = 1; i <= maxRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                int rowNo = i + 1;
                ImportRow importRow = parseRow(row, rowNo);
                if (!StringUtils.hasText(importRow.getProductName())) {
                    // 商品名称为空的行无法归组，单独计为一条失败
                    result.addFail();
                    result.addError("第 " + rowNo + " 行：商品名称不能为空");
                    continue;
                }
                groups.computeIfAbsent(importRow.getProductName().trim(), key -> new ArrayList<>()).add(importRow);
            }
            if (lastRowNum > MAX_IMPORT_ROWS) {
                result.addError("单次最多导入 " + MAX_IMPORT_ROWS + " 行，超出的行已忽略");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析商品导入 Excel 失败", e);
            throw new BusinessException("Excel 解析失败：" + e.getMessage());
        }

        if (groups.isEmpty() && result.getFailCount() == 0) {
            throw new BusinessException("Excel 中没有可导入的数据，请使用「下载模板」获取标准模板");
        }

        Map<String, Long> categoryIdByName = loadCategoryIdByName();
        for (Map.Entry<String, List<ImportRow>> entry : groups.entrySet()) {
            String productName = entry.getKey();
            List<ImportRow> rows = entry.getValue();
            List<String> rowErrors = rows.stream()
                    .map(ImportRow::getError)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
            if (!rowErrors.isEmpty()) {
                result.addFail();
                rowErrors.forEach(result::addError);
                continue;
            }
            try {
                ProductSaveDTO dto = buildImportProduct(productName, rows, categoryIdByName);
                // 逐个商品独立事务，单条失败不影响其它商品
                transactionTemplate.executeWithoutResult(status -> save(dto));
                result.addSuccess();
            } catch (BusinessException e) {
                result.addFail();
                result.addError("商品「" + productName + "」（第 " + rows.get(0).getRowNo() + " 行起）导入失败：" + e.getMessage());
            } catch (Exception e) {
                log.error("导入商品失败：{}", productName, e);
                result.addFail();
                result.addError("商品「" + productName + "」（第 " + rows.get(0).getRowNo() + " 行起）导入失败：" + e.getMessage());
            }
        }
        return result;
    }

    // ================================================================ 查询条件

    /**
     * 构建商品查询条件
     *
     * @param query    查询条件
     * @param onlyOnShelf 是否仅查询上架商品（买家端为 true）
     */
    private LambdaQueryWrapper<Product> buildWrapper(ProductQueryDTO query, boolean onlyOnShelf) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (onlyOnShelf) {
            wrapper.eq(Product::getStatus, 1);
        } else if (query.getStatus() != null) {
            wrapper.eq(Product::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(Product::getName, keyword)
                    .or().like(Product::getSubtitle, keyword)
                    .or().like(Product::getBrand, keyword));
        }
        if (query.getCategoryId() != null) {
            List<Long> categoryIds = collectCategoryIds(query.getCategoryId());
            if (categoryIds.isEmpty()) {
                // 分类不存在，直接返回空结果
                wrapper.eq(Product::getId, -1L);
            } else {
                wrapper.in(Product::getCategoryId, categoryIds);
            }
        }
        if (query.getIsRecommend() != null) {
            wrapper.eq(Product::getIsRecommend, query.getIsRecommend());
        }
        if (query.getMinPrice() != null) {
            wrapper.ge(Product::getPrice, query.getMinPrice());
        }
        if (query.getMaxPrice() != null) {
            wrapper.le(Product::getPrice, query.getMaxPrice());
        }
        applySort(wrapper, query.getSort());
        return wrapper;
    }

    /**
     * 排序：default(推荐+排序值) / sales / price_asc / price_desc / new
     */
    private void applySort(LambdaQueryWrapper<Product> wrapper, String sort) {
        String value = StringUtils.hasText(sort) ? sort.trim().toLowerCase(Locale.ROOT) : "default";
        switch (value) {
            case "sales" -> wrapper.orderByDesc(Product::getSales);
            case "price_asc" -> wrapper.orderByAsc(Product::getPrice);
            case "price_desc" -> wrapper.orderByDesc(Product::getPrice);
            case "new" -> wrapper.orderByDesc(Product::getCreateTime);
            default -> wrapper.orderByDesc(Product::getIsRecommend).orderByAsc(Product::getSort);
        }
        // 稳定分页的兜底排序
        wrapper.orderByDesc(Product::getId);
    }

    /**
     * 分类筛选：一级分类自动包含其子分类
     */
    private List<Long> collectCategoryIds(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            return new ArrayList<>();
        }
        List<Long> ids = new ArrayList<>();
        ids.add(category.getId());
        List<Category> children = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .select(Category::getId)
                .eq(Category::getParentId, categoryId));
        for (Category child : children) {
            ids.add(child.getId());
        }
        return ids;
    }

    // ================================================================ 结果组装

    private PageResult<ProductVO> toProductPage(Page<Product> page, boolean onlyEnabledSku) {
        List<ProductVO> list = toProductList(page.getRecords(), onlyEnabledSku);
        return new PageResult<>(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private List<ProductVO> toProductList(List<Product> products, boolean onlyEnabledSku) {
        List<ProductVO> list = new ArrayList<>();
        if (products == null || products.isEmpty()) {
            return list;
        }
        Map<Long, String> categoryNames = loadCategoryNames(products.stream()
                .map(Product::getCategoryId)
                .collect(Collectors.toList()));
        Map<Long, Integer> skuCounts = loadSkuCounts(products.stream()
                .map(Product::getId)
                .collect(Collectors.toList()), onlyEnabledSku);
        for (Product product : products) {
            ProductVO vo = new ProductVO();
            fillProductBase(product, vo);
            vo.setCategoryName(categoryNames.get(product.getCategoryId()));
            vo.setSkuCount(skuCounts.getOrDefault(product.getId(), 0));
            list.add(vo);
        }
        return list;
    }

    /**
     * 组装商品详情
     *
     * @param product     商品
     * @param shop        是否买家端（买家端需要校验状态、只返回启用 SKU、填充收藏与评价）
     * @param loginUserId 当前登录买家ID，可为空
     */
    private ProductDetailVO buildDetail(Product product, boolean shop, Long loginUserId) {
        ProductDetailVO vo = new ProductDetailVO();
        fillProductBase(product, vo);
        Category category = product.getCategoryId() == null ? null : categoryMapper.selectById(product.getCategoryId());
        vo.setCategoryName(category == null ? null : category.getName());
        vo.setImages(product.getImages());
        vo.setDetail(product.getDetail());
        vo.setCostPrice(product.getCostPrice());
        vo.setUpdateTime(product.getUpdateTime());
        vo.setSpecs(loadSpecVOs(product.getId()));
        vo.setSkus(loadSkuVOs(product.getId(), shop));
        vo.setSkuCount(vo.getSkus().size());

        // favorited：管理端恒为 false
        boolean favorited = false;
        if (shop && loginUserId != null) {
            Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getUserId, loginUserId)
                    .eq(Favorite::getProductId, product.getId()));
            favorited = count != null && count > 0;
        }
        vo.setFavorited(favorited);

        // 评价数量与平均分（仅统计显示中的评价）
        Long commentCount = productCommentMapper.selectCount(new LambdaQueryWrapper<ProductComment>()
                .eq(ProductComment::getProductId, product.getId())
                .eq(ProductComment::getStatus, 1));
        vo.setCommentCount(commentCount == null ? 0L : commentCount);
        vo.setAvgStar(calcAvgStar(product.getId(), vo.getCommentCount()));
        return vo;
    }

    private void fillProductBase(Product product, ProductVO vo) {
        vo.setId(product.getId());
        vo.setCategoryId(product.getCategoryId());
        vo.setName(product.getName());
        vo.setSubtitle(product.getSubtitle());
        vo.setBrand(product.getBrand());
        vo.setUnit(product.getUnit());
        vo.setMainImage(product.getMainImage());
        vo.setPrice(product.getPrice());
        vo.setOriginalPrice(product.getOriginalPrice());
        vo.setStock(product.getStock());
        vo.setSales(product.getSales());
        vo.setWarnStock(product.getWarnStock());
        vo.setStatus(product.getStatus());
        vo.setIsRecommend(product.getIsRecommend());
        vo.setIsNew(product.getIsNew());
        vo.setIsHot(product.getIsHot());
        vo.setSort(product.getSort());
        vo.setCreateTime(product.getCreateTime());
    }

    private List<SpecVO> loadSpecVOs(Long productId) {
        List<ProductSpec> specs = productSpecMapper.selectList(new LambdaQueryWrapper<ProductSpec>()
                .eq(ProductSpec::getProductId, productId)
                .orderByAsc(ProductSpec::getSort)
                .orderByAsc(ProductSpec::getId));
        List<SpecVO> list = new ArrayList<>(specs.size());
        for (ProductSpec spec : specs) {
            SpecVO vo = new SpecVO();
            vo.setId(spec.getId());
            vo.setSpecName(spec.getSpecName());
            vo.setSpecValues(splitSpecValues(spec.getSpecValues()));
            list.add(vo);
        }
        return list;
    }

    private List<SkuVO> loadSkuVOs(Long productId, boolean onlyEnabled) {
        LambdaQueryWrapper<ProductSku> wrapper = new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getProductId, productId)
                .orderByAsc(ProductSku::getId);
        if (onlyEnabled) {
            wrapper.eq(ProductSku::getStatus, 1);
        }
        List<SkuVO> list = new ArrayList<>();
        for (ProductSku sku : productSkuMapper.selectList(wrapper)) {
            SkuVO vo = new SkuVO();
            vo.setId(sku.getId());
            vo.setProductId(sku.getProductId());
            vo.setSkuCode(sku.getSkuCode());
            vo.setSpecData(sku.getSpecData());
            vo.setSpecInfo(sku.getSpecInfo());
            vo.setPrice(sku.getPrice());
            vo.setOriginalPrice(sku.getOriginalPrice());
            vo.setStock(sku.getStock());
            vo.setSales(sku.getSales());
            vo.setImage(sku.getImage());
            vo.setStatus(sku.getStatus());
            list.add(vo);
        }
        return list;
    }

    private Map<Long, String> loadCategoryNames(Collection<Long> categoryIds) {
        Map<Long, String> names = new HashMap<>();
        if (categoryIds == null) {
            return names;
        }
        List<Long> ids = categoryIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return names;
        }
        for (Category category : categoryMapper.selectBatchIds(ids)) {
            names.put(category.getId(), category.getName());
        }
        return names;
    }

    private Map<Long, Integer> loadSkuCounts(Collection<Long> productIds, boolean onlyEnabled) {
        Map<Long, Integer> counts = new HashMap<>();
        if (productIds == null) {
            return counts;
        }
        List<Long> ids = productIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return counts;
        }
        LambdaQueryWrapper<ProductSku> wrapper = new LambdaQueryWrapper<ProductSku>()
                .select(ProductSku::getProductId)
                .in(ProductSku::getProductId, ids);
        if (onlyEnabled) {
            wrapper.eq(ProductSku::getStatus, 1);
        }
        for (ProductSku sku : productSkuMapper.selectList(wrapper)) {
            if (sku.getProductId() != null) {
                counts.merge(sku.getProductId(), 1, Integer::sum);
            }
        }
        return counts;
    }

    /**
     * 平均评分（保留一位小数），无评价时返回 null
     */
    private Double calcAvgStar(Long productId, Long commentCount) {
        if (commentCount == null || commentCount <= 0) {
            return null;
        }
        QueryWrapper<ProductComment> wrapper = new QueryWrapper<ProductComment>()
                .select("IFNULL(AVG(star), 0)")
                .eq("product_id", productId)
                .eq("status", 1);
        List<Object> values = productCommentMapper.selectObjs(wrapper);
        if (values == null || values.isEmpty() || values.get(0) == null) {
            return null;
        }
        Object value = values.get(0);
        double avg;
        if (value instanceof Number number) {
            avg = number.doubleValue();
        } else {
            try {
                avg = Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    // ================================================================ 参数处理

    private Product selectProduct(Long id) {
        if (id == null) {
            throw new BusinessException("商品ID不能为空");
        }
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        return product;
    }

    /**
     * 校验规格 JSON 是否为合法 JSON 对象，并返回规范化后的字符串
     */
    private String normalizeSpecData(String specData) {
        if (!StringUtils.hasText(specData)) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(specData);
            if (node == null || !node.isObject()) {
                throw new BusinessException("SKU 规格数据必须是 JSON 对象，如 {\"颜色\":\"曜石黑\"}");
            }
            String value = node.toString();
            if (value.length() > MAX_SPEC_DATA_LENGTH) {
                throw new BusinessException("SKU 规格数据长度不能超过 " + MAX_SPEC_DATA_LENGTH + " 个字符");
            }
            return value;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("SKU 规格数据不是合法的 JSON 字符串：" + specData);
        }
    }

    /**
     * 规格展示文本：优先使用传入值，为空时由 specData 的所有 value 以 " / " 拼接
     */
    private String resolveSpecInfo(String specInfo, String specData) {
        String value = StringUtils.hasText(specInfo) ? specInfo.trim() : null;
        if (value == null && StringUtils.hasText(specData)) {
            try {
                JsonNode node = objectMapper.readTree(specData);
                List<String> values = new ArrayList<>();
                node.fields().forEachRemaining(item -> {
                    String text = item.getValue() == null ? "" : item.getValue().asText("");
                    if (StringUtils.hasText(text)) {
                        values.add(text);
                    }
                });
                if (!values.isEmpty()) {
                    value = String.join(" / ", values);
                }
            } catch (Exception e) {
                log.warn("解析 SKU 规格数据失败：{}", e.getMessage());
            }
        }
        if (value != null && value.length() > MAX_SPEC_INFO_LENGTH) {
            value = value.substring(0, MAX_SPEC_INFO_LENGTH);
        }
        return value;
    }

    /**
     * 校验 0/1 开关值
     */
    private Integer resolveFlag(Integer value, Integer defaultValue, String fieldName) {
        if (value == null) {
            if (defaultValue == null) {
                throw new BusinessException(fieldName + "不能为空");
            }
            return defaultValue;
        }
        if (!Integer.valueOf(0).equals(value) && !Integer.valueOf(1).equals(value)) {
            throw new BusinessException(fieldName + "只能是 0 或 1");
        }
        return value;
    }

    private String joinImages(List<String> images) {
        String joined = images.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining(","));
        if (joined.length() > MAX_IMAGES_LENGTH) {
            throw new BusinessException("轮播图数量过多，地址总长度不能超过 " + MAX_IMAGES_LENGTH + " 个字符");
        }
        return StringUtils.hasText(joined) ? joined : null;
    }

    private List<String> splitSpecValues(String specValues) {
        if (!StringUtils.hasText(specValues)) {
            return new ArrayList<>();
        }
        return Arrays.stream(specValues.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private String joinSpecValues(List<String> specValues) {
        if (specValues == null || specValues.isEmpty()) {
            return "";
        }
        return specValues.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(","));
    }

    private BigDecimal min(BigDecimal current, BigDecimal candidate) {
        if (current == null) {
            return candidate;
        }
        if (candidate == null) {
            return current;
        }
        return current.min(candidate);
    }

    private int defaultWarnStockValue() {
        return defaultWarnStock == null || defaultWarnStock < 0 ? 10 : defaultWarnStock;
    }

    private long normalizePage(Integer page) {
        return page == null || page < 1 ? 1L : page;
    }

    private long normalizeSize(Integer size) {
        return size == null || size < 1 ? 10L : Math.min(size, MAX_PAGE_SIZE);
    }

    private int normalizeLimit(Integer limit) {
        return limit == null || limit < 1 ? 8 : Math.min(limit, 50);
    }

    // ================================================================ Excel 导入

    /**
     * 判断整行为空
     */
    private boolean isBlankRow(Row row) {
        for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
            if (StringUtils.hasText(ExcelUtils.readCell(row.getCell(i)))) {
                return false;
            }
        }
        return true;
    }

    private ImportRow parseRow(Row row, int rowNo) {
        ImportRow importRow = new ImportRow(rowNo);
        importRow.setProductName(ExcelUtils.readCell(row.getCell(0)));
        importRow.setSubtitle(ExcelUtils.readCell(row.getCell(1)));
        importRow.setBrand(ExcelUtils.readCell(row.getCell(2)));
        importRow.setCategoryName(ExcelUtils.readCell(row.getCell(3)));
        importRow.setUnit(ExcelUtils.readCell(row.getCell(4)));
        String specText = ExcelUtils.readCell(row.getCell(5));
        String priceText = ExcelUtils.readCell(row.getCell(6));
        String originalPriceText = ExcelUtils.readCell(row.getCell(7));
        String stockText = ExcelUtils.readCell(row.getCell(8));
        importRow.setSkuCode(ExcelUtils.readCell(row.getCell(9)));

        List<String> errors = new ArrayList<>();
        BigDecimal price = parseDecimal(priceText);
        if (price == null) {
            errors.add("SKU价格「" + nullToEmpty(priceText) + "」不是合法的数字");
        } else {
            importRow.setPrice(price);
        }
        BigDecimal originalPrice = parseDecimal(originalPriceText);
        if (StringUtils.hasText(originalPriceText) && originalPrice == null) {
            errors.add("SKU原价「" + originalPriceText + "」不是合法的数字");
        } else {
            importRow.setOriginalPrice(originalPrice == null ? price : originalPrice);
        }
        Integer stock = parseInteger(stockText);
        if (stock == null || stock < 0) {
            errors.add("SKU库存「" + nullToEmpty(stockText) + "」不是合法的非负整数");
        } else {
            importRow.setStock(stock);
        }
        if (StringUtils.hasText(specText)) {
            try {
                importRow.setSpecMap(parseSpecText(specText));
            } catch (BusinessException e) {
                errors.add(e.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            importRow.setError("第 " + rowNo + " 行：" + String.join("；", errors));
        }
        return importRow;
    }

    /**
     * 解析「颜色:红色;尺码:M」形式的规格文本
     */
    private LinkedHashMap<String, String> parseSpecText(String specText) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (String part : specText.split("[;；]")) {
            String item = part.trim();
            if (!StringUtils.hasText(item)) {
                continue;
            }
            int index = indexOfSeparator(item);
            if (index <= 0 || index == item.length() - 1) {
                throw new BusinessException("SKU规格「" + item + "」格式错误，应为 规格名:规格值");
            }
            String name = item.substring(0, index).trim();
            String value = item.substring(index + 1).trim();
            if (!StringUtils.hasText(name) || !StringUtils.hasText(value)) {
                throw new BusinessException("SKU规格「" + item + "」格式错误，应为 规格名:规格值");
            }
            map.put(name, value);
        }
        if (map.isEmpty()) {
            throw new BusinessException("SKU规格不能为空");
        }
        return map;
    }

    /**
     * 兼容半角/全角冒号
     */
    private int indexOfSeparator(String text) {
        int half = text.indexOf(':');
        int full = text.indexOf('：');
        if (half < 0) {
            return full;
        }
        if (full < 0) {
            return half;
        }
        return Math.min(half, full);
    }

    private ProductSaveDTO buildImportProduct(String productName, List<ImportRow> rows,
                                              Map<String, Long> categoryIdByName) {
        String categoryName = rows.stream()
                .map(ImportRow::getCategoryName)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .findFirst()
                .orElse(null);
        if (!StringUtils.hasText(categoryName)) {
            throw new BusinessException("分类名称不能为空");
        }
        Long categoryId = categoryIdByName.get(categoryName);
        if (categoryId == null) {
            throw new BusinessException("分类「" + categoryName + "」不存在");
        }

        // 规格定义：按出现顺序合并各 SKU 的规格值
        Map<String, LinkedHashSet<String>> specValues = new LinkedHashMap<>();
        for (ImportRow row : rows) {
            row.getSpecMap().forEach((name, value) ->
                    specValues.computeIfAbsent(name, key -> new LinkedHashSet<>()).add(value));
        }
        List<SpecSaveDTO> specs = new ArrayList<>();
        specValues.forEach((name, values) -> {
            SpecSaveDTO spec = new SpecSaveDTO();
            spec.setSpecName(name);
            spec.setSpecValues(new ArrayList<>(values));
            specs.add(spec);
        });

        List<SkuSaveDTO> skus = new ArrayList<>();
        for (ImportRow row : rows) {
            SkuSaveDTO sku = new SkuSaveDTO();
            sku.setSkuCode(row.getSkuCode());
            sku.setSpecData(row.getSpecMap().isEmpty() ? null : toJson(row.getSpecMap()));
            sku.setPrice(row.getPrice());
            sku.setOriginalPrice(row.getOriginalPrice());
            sku.setStock(row.getStock());
            sku.setStatus(1);
            skus.add(sku);
        }

        ProductSaveDTO dto = new ProductSaveDTO();
        dto.setCategoryId(categoryId);
        dto.setName(productName);
        dto.setSubtitle(firstText(rows, ImportRow::getSubtitle));
        dto.setBrand(firstText(rows, ImportRow::getBrand));
        dto.setUnit(firstText(rows, ImportRow::getUnit));
        dto.setStatus(1);
        dto.setIsRecommend(0);
        dto.setIsNew(0);
        dto.setIsHot(0);
        dto.setSort(0);
        dto.setSpecs(specs);
        dto.setSkus(skus);
        return dto;
    }

    private String firstText(List<ImportRow> rows, java.util.function.Function<ImportRow, String> getter) {
        return rows.stream()
                .map(getter)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .findFirst()
                .orElse(null);
    }

    private Map<String, Long> loadCategoryIdByName() {
        List<Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getId));
        Map<String, Long> map = new LinkedHashMap<>();
        for (Category category : categories) {
            if (StringUtils.hasText(category.getName())) {
                map.putIfAbsent(category.getName().trim(), category.getId());
            }
        }
        return map;
    }

    private String toJson(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new BusinessException("SKU 规格数据序列化失败：" + e.getMessage());
        }
    }

    private BigDecimal parseDecimal(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInteger(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return new BigDecimal(text.trim()).intValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            return null;
        }
    }

    private String nullToEmpty(String text) {
        return text == null ? "" : text;
    }

    /**
     * 导入时的一行 SKU 数据
     */
    private static class ImportRow {

        private final int rowNo;
        private String productName;
        private String subtitle;
        private String brand;
        private String categoryName;
        private String unit;
        private String skuCode;
        private BigDecimal price;
        private BigDecimal originalPrice;
        private Integer stock;
        private LinkedHashMap<String, String> specMap = new LinkedHashMap<>();
        private String error;

        ImportRow(int rowNo) {
            this.rowNo = rowNo;
        }

        int getRowNo() {
            return rowNo;
        }

        String getProductName() {
            return productName;
        }

        void setProductName(String productName) {
            this.productName = productName;
        }

        String getSubtitle() {
            return subtitle;
        }

        void setSubtitle(String subtitle) {
            this.subtitle = subtitle;
        }

        String getBrand() {
            return brand;
        }

        void setBrand(String brand) {
            this.brand = brand;
        }

        String getCategoryName() {
            return categoryName;
        }

        void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        String getUnit() {
            return unit;
        }

        void setUnit(String unit) {
            this.unit = unit;
        }

        String getSkuCode() {
            return skuCode;
        }

        void setSkuCode(String skuCode) {
            this.skuCode = skuCode;
        }

        BigDecimal getPrice() {
            return price;
        }

        void setPrice(BigDecimal price) {
            this.price = price;
        }

        BigDecimal getOriginalPrice() {
            return originalPrice;
        }

        void setOriginalPrice(BigDecimal originalPrice) {
            this.originalPrice = originalPrice;
        }

        Integer getStock() {
            return stock;
        }

        void setStock(Integer stock) {
            this.stock = stock;
        }

        LinkedHashMap<String, String> getSpecMap() {
            return specMap;
        }

        void setSpecMap(LinkedHashMap<String, String> specMap) {
            this.specMap = specMap == null ? new LinkedHashMap<>() : specMap;
        }

        String getError() {
            return error;
        }

        void setError(String error) {
            this.error = error;
        }
    }
}
