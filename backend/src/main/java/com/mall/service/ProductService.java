package com.mall.service;

import com.mall.dto.ProductQueryDTO;
import com.mall.dto.ProductSaveDTO;
import com.mall.common.PageResult;
import com.mall.vo.ProductDetailVO;
import com.mall.vo.ProductImportResultVO;
import com.mall.vo.ProductVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 商品服务（买家端 + 管理端）
 */
public interface ProductService {

    // ==================== 买家端 ====================

    /**
     * 买家端商品分页：仅上架商品，支持关键词/分类/价格区间/排序
     */
    PageResult<ProductVO> pageShopProducts(ProductQueryDTO query);

    /**
     * 买家端商品详情：下架商品抛业务异常；填充规格、SKU、收藏、评价统计
     *
     * @param id          商品ID
     * @param loginUserId 当前登录买家ID，可为 null（未登录）
     */
    ProductDetailVO getShopDetail(Long id, Long loginUserId);

    /**
     * 热销商品：isHot=1 且上架，按销量倒序
     */
    List<ProductVO> listHot(Integer limit);

    /**
     * 新品：上架商品按创建时间倒序
     */
    List<ProductVO> listNew(Integer limit);

    /**
     * 推荐商品：isRecommend=1 且上架，按排序值
     */
    List<ProductVO> listRecommend(Integer limit);

    // ==================== 管理端 ====================

    /**
     * 管理端商品分页：包含下架商品
     */
    PageResult<ProductVO> pageAdminProducts(ProductQueryDTO query);

    /**
     * 管理端商品详情：不过滤下架 SKU，favorited 恒为 false
     */
    ProductDetailVO getAdminDetail(Long id);

    /**
     * 新增/编辑商品，返回商品ID
     */
    Long save(ProductSaveDTO dto);

    /**
     * 逻辑删除商品（同时删除 SKU 与规格），存在未完成订单时拒绝
     */
    void delete(Long id);

    /**
     * 修改商品上下架状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 修改商品推荐标记
     */
    void updateRecommend(Long id, Integer isRecommend);

    /**
     * 按当前筛选条件导出商品 Excel（最多 5000 条）
     */
    void exportExcel(HttpServletResponse response, ProductQueryDTO query);

    /**
     * 导出商品导入模板
     */
    void exportTemplate(HttpServletResponse response);

    /**
     * Excel 导入商品：同一商品名称的多行合并为一个 SPU + 多 SKU
     */
    ProductImportResultVO importExcel(MultipartFile file);
}
