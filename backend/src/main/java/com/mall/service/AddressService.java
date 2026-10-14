package com.mall.service;

import com.mall.dto.AddressSaveDTO;
import com.mall.vo.AddressVO;

import java.util.List;

/**
 * 收货地址服务
 */
public interface AddressService {

    /**
     * 当前用户地址列表（默认地址优先，其次按创建时间倒序）
     *
     * @return 地址列表
     */
    List<AddressVO> list();

    /**
     * 查询地址详情（校验归属）
     *
     * @param id 地址ID
     * @return 地址
     */
    AddressVO getById(Long id);

    /**
     * 新增/修改地址
     *
     * @param dto 参数
     */
    void save(AddressSaveDTO dto);

    /**
     * 删除地址（校验归属）
     *
     * @param id 地址ID
     */
    void deleteById(Long id);

    /**
     * 设为默认地址
     *
     * @param id 地址ID
     */
    void setDefault(Long id);
}
