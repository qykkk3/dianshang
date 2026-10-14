package com.mall.service;

import com.mall.dto.CartAddDTO;
import com.mall.dto.CartBatchDTO;
import com.mall.dto.CartCheckedDTO;
import com.mall.dto.CartUpdateDTO;
import com.mall.vo.CartVO;

import java.util.List;

/**
 * 购物车服务
 */
public interface CartService {

    /**
     * 当前用户购物车列表（按下单时间倒序）
     *
     * @return 购物车条目
     */
    List<CartVO> list();

    /**
     * 当前用户购物车条目数（用于角标）
     *
     * @return 条目数
     */
    Integer count();

    /**
     * 加入购物车（同 SKU 累加数量）
     *
     * @param dto 参数
     */
    void add(CartAddDTO dto);

    /**
     * 修改条目数量/勾选状态
     *
     * @param id  条目ID
     * @param dto 参数
     */
    void update(Long id, CartUpdateDTO dto);

    /**
     * 批量勾选/取消勾选；ids 为空时对当前用户全部条目生效
     *
     * @param dto 参数
     */
    void updateChecked(CartCheckedDTO dto);

    /**
     * 删除单条
     *
     * @param id 条目ID
     */
    void deleteById(Long id);

    /**
     * 批量删除
     *
     * @param dto 参数
     */
    void deleteBatch(CartBatchDTO dto);
}
