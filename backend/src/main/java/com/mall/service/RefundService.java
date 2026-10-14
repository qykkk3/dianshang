package com.mall.service;

import com.mall.common.PageResult;
import com.mall.dto.RefundApplyDTO;
import com.mall.dto.RefundAuditDTO;
import com.mall.entity.Refund;
import com.mall.vo.RefundVO;

/**
 * 售后服务
 */
public interface RefundService {

    /**
     * 买家申请售后
     *
     * @param dto 申请参数
     * @return 售后单ID
     */
    Long apply(RefundApplyDTO dto);

    /**
     * 买家售后分页
     *
     * @param status 状态，为空查全部
     * @param page   页码
     * @param size   每页条数
     * @return 分页结果
     */
    PageResult<RefundVO> pageBuyerRefunds(Integer status, Integer page, Integer size);

    /**
     * 买家售后详情
     *
     * @param id 售后单ID
     * @return 售后单
     */
    RefundVO getBuyerDetail(Long id);

    /**
     * 买家撤销售后（仅待审核）
     *
     * @param id 售后单ID
     */
    void cancel(Long id);

    /* ==================== 以下方法供管理端复用 ==================== */

    /**
     * 管理端售后分页（关键词匹配售后单号/订单号）
     *
     * @param keyword 关键词
     * @param status  状态
     * @param page    页码
     * @param size    每页条数
     * @return 分页结果
     */
    PageResult<RefundVO> pageAdminRefunds(String keyword, Integer status, Integer page, Integer size);

    /**
     * 管理端售后详情
     *
     * @param id 售后单ID
     * @return 售后单
     */
    RefundVO getAdminDetail(Long id);

    /**
     * 管理端售后审核
     *
     * @param id  售后单ID
     * @param dto 审核参数
     */
    void audit(Long id, RefundAuditDTO dto);

    /**
     * 售后单实体 → VO（含买家昵称、商品名/图、类型与状态名称），供订单详情复用
     *
     * @param refund 售后单实体
     * @return 售后单 VO
     */
    RefundVO toVO(Refund refund);
}
