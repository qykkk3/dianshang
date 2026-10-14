package com.mall.service;

import com.mall.common.PageResult;
import com.mall.dto.NoticeSaveDTO;
import com.mall.vo.NoticeVO;

/**
 * 公告服务
 */
public interface NoticeService {

    /** 商城公告分页（仅已发布） */
    PageResult<NoticeVO> pageShop(Integer type, Integer page, Integer size);

    /** 商城公告详情（仅已发布） */
    NoticeVO getShopDetail(Long id);

    /** 管理端公告分页（含草稿） */
    PageResult<NoticeVO> pageAdmin(Integer page, Integer size, Integer type, Integer status);

    /** 新增 / 修改公告 */
    void save(NoticeSaveDTO dto);

    /** 删除公告（逻辑删除） */
    void delete(Long id);

    /** 发布 / 撤回公告 */
    void publish(Long id, Integer status);
}
