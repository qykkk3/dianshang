package com.mall.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.BusinessException;
import com.mall.dto.AddressSaveDTO;
import com.mall.entity.UserAddress;
import com.mall.mapper.UserAddressMapper;
import com.mall.security.SecurityUtils;
import com.mall.service.AddressService;
import com.mall.vo.AddressVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 收货地址服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final UserAddressMapper userAddressMapper;

    @Override
    public List<AddressVO> list() {
        Long userId = SecurityUtils.getUserId();
        List<UserAddress> list = userAddressMapper.selectList(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getCreateTime));
        return list.stream().map(this::toAddressVO).collect(Collectors.toList());
    }

    @Override
    public AddressVO getById(Long id) {
        return toAddressVO(requireOwned(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(AddressSaveDTO dto) {
        Long userId = SecurityUtils.getUserId();
        // 是否首个地址：首个地址自动设为默认
        Long addressCount = userAddressMapper.selectCount(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getUserId, userId));
        boolean first = addressCount == null || addressCount == 0;

        Integer isDefault = dto.getIsDefault();
        boolean wantDefault = Integer.valueOf(1).equals(isDefault);

        if (dto.getId() == null) {
            // 新增
            UserAddress entity = new UserAddress();
            entity.setUserId(userId);
            entity.setReceiver(dto.getReceiver());
            entity.setPhone(dto.getPhone());
            entity.setProvince(dto.getProvince());
            entity.setCity(dto.getCity());
            entity.setDistrict(dto.getDistrict());
            entity.setDetail(dto.getDetail());
            entity.setPostCode(dto.getPostCode());
            entity.setIsDefault(first || wantDefault ? 1 : 0);
            if (first || wantDefault) {
                clearDefault(userId, null);
            }
            userAddressMapper.insert(entity);
        } else {
            // 修改：校验归属
            UserAddress entity = requireOwned(dto.getId());
            entity.setReceiver(dto.getReceiver());
            entity.setPhone(dto.getPhone());
            entity.setProvince(dto.getProvince());
            entity.setCity(dto.getCity());
            entity.setDistrict(dto.getDistrict());
            entity.setDetail(dto.getDetail());
            entity.setPostCode(dto.getPostCode());
            if (wantDefault) {
                // 先把该用户其它地址置为非默认
                clearDefault(userId, entity.getId());
                entity.setIsDefault(1);
            } else if (isDefault != null) {
                // 首个地址不允许取消默认，避免下单时无默认地址
                entity.setIsDefault(first ? 1 : 0);
            }
            userAddressMapper.updateById(entity);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        UserAddress entity = requireOwned(id);
        userAddressMapper.deleteById(entity.getId());
        // 删除的是默认地址时，自动把最新一条地址补为默认，避免下单无默认地址
        if (Integer.valueOf(1).equals(entity.getIsDefault())) {
            List<UserAddress> rest = userAddressMapper.selectList(Wrappers.<UserAddress>lambdaQuery()
                    .eq(UserAddress::getUserId, entity.getUserId())
                    .orderByDesc(UserAddress::getCreateTime)
                    .last("limit 1"));
            if (!rest.isEmpty()) {
                UserAddress first = new UserAddress();
                first.setId(rest.get(0).getId());
                first.setIsDefault(1);
                userAddressMapper.updateById(first);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        Long userId = SecurityUtils.getUserId();
        UserAddress entity = requireOwned(id);
        clearDefault(userId, entity.getId());
        UserAddress update = new UserAddress();
        update.setId(entity.getId());
        update.setIsDefault(1);
        userAddressMapper.updateById(update);
    }

    /**
     * 把该用户其它地址的默认标记置 0
     *
     * @param userId    用户ID
     * @param excludeId 排除的地址ID，可为空
     */
    private void clearDefault(Long userId, Long excludeId) {
        UserAddress update = new UserAddress();
        update.setIsDefault(0);
        userAddressMapper.update(update, Wrappers.<UserAddress>lambdaUpdate()
                .eq(UserAddress::getUserId, userId)
                .ne(excludeId != null, UserAddress::getId, excludeId));
    }

    /**
     * 查询地址并校验归属
     */
    private UserAddress requireOwned(Long id) {
        if (id == null) {
            throw new BusinessException("地址ID不能为空");
        }
        UserAddress entity = userAddressMapper.selectById(id);
        if (entity == null || !entity.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException("收货地址不存在或无权操作");
        }
        return entity;
    }

    /**
     * 实体 → VO，并拼接完整地址
     */
    private AddressVO toAddressVO(UserAddress entity) {
        AddressVO vo = new AddressVO();
        vo.setId(entity.getId());
        vo.setReceiver(entity.getReceiver());
        vo.setPhone(entity.getPhone());
        vo.setProvince(entity.getProvince());
        vo.setCity(entity.getCity());
        vo.setDistrict(entity.getDistrict());
        vo.setDetail(entity.getDetail());
        vo.setPostCode(entity.getPostCode());
        vo.setIsDefault(entity.getIsDefault());
        vo.setFullAddress(buildFullAddress(entity.getProvince(), entity.getCity(),
                entity.getDistrict(), entity.getDetail()));
        return vo;
    }

    /**
     * 拼接完整地址：省 + 市 + 区 + 详细地址
     */
    static String buildFullAddress(String province, String city, String district, String detail) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(province)) {
            sb.append(province);
        }
        if (StringUtils.hasText(city)) {
            sb.append(city);
        }
        if (StringUtils.hasText(district)) {
            sb.append(district);
        }
        if (StringUtils.hasText(detail)) {
            sb.append(detail);
        }
        return sb.toString();
    }
}
