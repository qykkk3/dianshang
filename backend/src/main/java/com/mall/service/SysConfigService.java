package com.mall.service;

import com.mall.dto.SysConfigSaveDTO;
import com.mall.vo.SysConfigVO;

import java.util.List;

/**
 * 系统配置服务
 */
public interface SysConfigService {

    /**
     * 全部配置项
     */
    List<SysConfigVO> list();

    /**
     * 按键取值，供其它模块复用；不存在时返回 defaultValue
     */
    String getValue(String key, String defaultValue);

    /**
     * 按 configKey 更新配置值，不存在则新增
     */
    void update(SysConfigSaveDTO dto);

    /**
     * 批量更新配置
     */
    void batchUpdate(List<SysConfigSaveDTO> list);
}
