package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.common.BusinessException;
import com.mall.dto.SysConfigSaveDTO;
import com.mall.entity.SysConfig;
import com.mall.mapper.SysConfigMapper;
import com.mall.service.SysConfigService;
import com.mall.vo.SysConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统配置服务实现
 *
 * <p>sys_config 无逻辑删除字段，按 configKey 唯一约束进行更新或新增。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper sysConfigMapper;

    @Override
    public List<SysConfigVO> list() {
        List<SysConfig> configs = sysConfigMapper.selectList(
                new LambdaQueryWrapper<SysConfig>().orderByAsc(SysConfig::getId));
        return configs.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public String getValue(String key, String defaultValue) {
        if (!StringUtils.hasText(key)) {
            return defaultValue;
        }
        SysConfig config = findByKey(key.trim());
        if (config == null || config.getConfigValue() == null) {
            return defaultValue;
        }
        return config.getConfigValue();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysConfigSaveDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getConfigKey())) {
            throw new BusinessException("配置键不能为空");
        }
        String configKey = dto.getConfigKey().trim();
        SysConfig existing = findByKey(configKey);
        if (existing == null) {
            // 不存在则新增（config_name / config_value 在库中为 NOT NULL）
            SysConfig entity = new SysConfig();
            entity.setConfigKey(configKey);
            entity.setConfigName(StringUtils.hasText(dto.getConfigName()) ? dto.getConfigName() : configKey);
            entity.setConfigValue(dto.getConfigValue() == null ? "" : dto.getConfigValue());
            entity.setRemark(dto.getRemark());
            sysConfigMapper.insert(entity);
            log.info("新增系统配置：{}", configKey);
            return;
        }
        SysConfig entity = new SysConfig();
        entity.setId(existing.getId());
        if (dto.getConfigValue() != null) {
            entity.setConfigValue(dto.getConfigValue());
        }
        if (StringUtils.hasText(dto.getConfigName())) {
            entity.setConfigName(dto.getConfigName());
        }
        if (dto.getRemark() != null) {
            entity.setRemark(dto.getRemark());
        }
        sysConfigMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdate(List<SysConfigSaveDTO> list) {
        if (list == null || list.isEmpty()) {
            throw new BusinessException("没有需要保存的配置项");
        }
        for (SysConfigSaveDTO dto : list) {
            update(dto);
        }
    }

    /* ------------------------------------------------------------------ 内部方法 */

    private SysConfig findByKey(String configKey) {
        return sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, configKey)
                .last("LIMIT 1"));
    }

    private SysConfigVO toVO(SysConfig config) {
        SysConfigVO vo = new SysConfigVO();
        vo.setId(config.getId());
        vo.setConfigKey(config.getConfigKey());
        vo.setConfigName(config.getConfigName());
        vo.setConfigValue(config.getConfigValue());
        vo.setRemark(config.getRemark());
        vo.setUpdateTime(config.getUpdateTime());
        return vo;
    }
}
