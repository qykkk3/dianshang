package com.mall.service;

import com.mall.common.PageResult;
import com.mall.dto.SysOperLogQueryDTO;
import com.mall.vo.SysOperLogVO;

import java.util.List;

/**
 * 操作日志服务
 */
public interface SysOperLogService {

    /**
     * 操作日志分页查询（按操作时间倒序）
     */
    PageResult<SysOperLogVO> page(SysOperLogQueryDTO query);

    /**
     * 清理 N 天之前的日志；days <= 0 表示清空全部（物理删除）
     */
    void clean(Integer days);

    /**
     * 按ID批量删除日志（物理删除）
     */
    void deleteByIds(List<Long> ids);
}
