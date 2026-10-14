package com.mall.service;

import com.mall.common.PageResult;
import com.mall.dto.CommentAddDTO;
import com.mall.dto.CommentQueryDTO;
import com.mall.vo.CommentStatsVO;
import com.mall.vo.CommentVO;

import java.util.List;
import java.util.Map;

/**
 * 商品评价服务（买家端 + 管理端）
 */
public interface CommentService {

    /** 商品评价列表（公开，仅显示状态） */
    PageResult<CommentVO> pageProductComments(Long productId, Integer star, Integer page, Integer size);

    /** 商品评价统计 */
    CommentStatsVO getProductCommentStats(Long productId);

    /** 买家发表评价 */
    void addComment(CommentAddDTO dto);

    /** 我的评价 */
    PageResult<CommentVO> pageMyComments(Integer page, Integer size);

    /** 待评价的订单明细（已完成且未评价） */
    PageResult<Map<String, Object>> pagePendingItems(Integer page, Integer size);

    /** 管理端评价列表 */
    PageResult<CommentVO> pageAdminComments(CommentQueryDTO query);

    /** 管理端回复评价 */
    void reply(Long id, String replyContent);

    /** 管理端修改评价显示状态 1显示 0隐藏 */
    void updateStatus(Long id, Integer status);

    /** 管理端删除评价（物理删除） */
    void delete(Long id);
}
