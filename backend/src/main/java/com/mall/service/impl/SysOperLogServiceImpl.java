package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.BusinessException;
import com.mall.common.PageResult;
import com.mall.dto.SysOperLogQueryDTO;
import com.mall.entity.SysOperLog;
import com.mall.mapper.SysOperLogMapper;
import com.mall.service.SysOperLogService;
import com.mall.vo.SysOperLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

/**
 * 操作日志服务实现
 *
 * <p>sys_oper_log 无逻辑删除字段，清理与批量删除均为物理删除。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysOperLogServiceImpl implements SysOperLogService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final int DEFAULT_CLEAN_DAYS = 30;

    private final SysOperLogMapper sysOperLogMapper;

    @Override
    public PageResult<SysOperLogVO> page(SysOperLogQueryDTO query) {
        SysOperLogQueryDTO dto = query == null ? new SysOperLogQueryDTO() : query;
        long current = dto.getPage() == null || dto.getPage() < 1 ? 1L : dto.getPage();
        long size = dto.getSize() == null || dto.getSize() < 1 ? 10L : dto.getSize();
        Page<SysOperLog> page = new Page<>(current, size);

        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dto.getKeyword())) {
            String keyword = dto.getKeyword().trim();
            wrapper.and(w -> w.like(SysOperLog::getOperation, keyword)
                    .or().like(SysOperLog::getUsername, keyword)
                    .or().like(SysOperLog::getRequestUri, keyword));
        }
        wrapper.eq(StringUtils.hasText(dto.getModule()), SysOperLog::getModule,
                        StringUtils.hasText(dto.getModule()) ? dto.getModule().trim() : null)
                .eq(dto.getStatus() != null, SysOperLog::getStatus, dto.getStatus());
        // 时间区间按 create_time 过滤；只传日期时，结束时间自动补到当天 23:59:59
        LocalDateTime startTime = parseDateTime(dto.getStartTime(), false);
        LocalDateTime endTime = parseDateTime(dto.getEndTime(), true);
        wrapper.ge(startTime != null, SysOperLog::getCreateTime, startTime)
                .le(endTime != null, SysOperLog::getCreateTime, endTime);
        wrapper.orderByDesc(SysOperLog::getCreateTime).orderByDesc(SysOperLog::getId);
        sysOperLogMapper.selectPage(page, wrapper);
        return PageResult.of(page, this::toVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clean(Integer days) {
        int cleanDays = days == null ? DEFAULT_CLEAN_DAYS : days;
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        if (cleanDays > 0) {
            wrapper.lt(SysOperLog::getCreateTime, LocalDateTime.now().minusDays(cleanDays));
        }
        // days <= 0：不带条件，清空全部日志（物理删除）
        int rows = sysOperLogMapper.delete(wrapper);
        log.info("清理操作日志：{} 天之前，共 {} 条", cleanDays, rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的日志");
        }
        List<Long> distinctIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            throw new BusinessException("请选择要删除的日志");
        }
        sysOperLogMapper.deleteBatchIds(distinctIds);
    }

    /* ------------------------------------------------------------------ 内部方法 */

    /**
     * 宽松解析时间字符串：支持 yyyy-MM-dd、yyyy-MM-dd HH:mm、yyyy-MM-dd HH:mm:ss 以及 ISO 的 T 分隔
     *
     * @param endOfDay 只传日期时是否补到当天最后一秒
     */
    private LocalDateTime parseDateTime(String text, boolean endOfDay) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String value = text.trim();
        try {
            if (value.length() >= 19) {
                return LocalDateTime.parse(value.substring(0, 19).replace('T', ' '), DATE_TIME_FORMATTER);
            }
            if (value.length() == 16) {
                return LocalDateTime.parse(value, DATE_TIME_MINUTE_FORMATTER);
            }
            if (value.length() == 10) {
                LocalDate date = LocalDate.parse(value, DATE_FORMATTER);
                return endOfDay ? LocalDateTime.of(date, LocalTime.of(23, 59, 59)) : date.atStartOfDay();
            }
        } catch (DateTimeParseException e) {
            throw new BusinessException("时间格式不正确，应为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
        }
        throw new BusinessException("时间格式不正确，应为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
    }

    private SysOperLogVO toVO(SysOperLog entity) {
        SysOperLogVO vo = new SysOperLogVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setUsername(entity.getUsername());
        vo.setModule(entity.getModule());
        vo.setOperation(entity.getOperation());
        vo.setMethod(entity.getMethod());
        vo.setRequestUri(entity.getRequestUri());
        vo.setParams(entity.getParams());
        vo.setIp(entity.getIp());
        vo.setCostTime(entity.getCostTime());
        vo.setStatus(entity.getStatus());
        vo.setErrorMsg(entity.getErrorMsg());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
