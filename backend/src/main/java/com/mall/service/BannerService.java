package com.mall.service;

import com.mall.common.PageResult;
import com.mall.dto.BannerSaveDTO;
import com.mall.vo.BannerVO;

import java.util.List;

/**
 * 轮播图服务
 */
public interface BannerService {

    /** 商城轮播图（仅启用，按 sort 升序；position 为空查全部位置） */
    List<BannerVO> listShop(Integer position);

    /** 管理端轮播图分页 */
    PageResult<BannerVO> pageAdmin(Integer page, Integer size, Integer position, Integer status);

    /** 新增 / 修改轮播图 */
    void save(BannerSaveDTO dto);

    /** 删除轮播图（逻辑删除） */
    void delete(Long id);

    /** 启用 / 停用轮播图 */
    void updateStatus(Long id, Integer status);
}
