package com.mall.service;

import com.mall.common.PageResult;
import com.mall.dto.MemberQueryDTO;
import com.mall.vo.MemberDetailVO;
import com.mall.vo.MemberVO;

/**
 * 会员（买家）管理服务
 */
public interface MemberService {

    /**
     * 会员分页查询，含有效订单数与累计消费金额
     */
    PageResult<MemberVO> page(MemberQueryDTO query);

    /**
     * 会员详情：含地址数、收藏数、评价数与最近订单
     */
    MemberDetailVO getDetail(Long id);

    /**
     * 启用 / 禁用会员（禁用时强制下线）
     */
    void updateStatus(Long id, Integer status);
}
