package com.mall.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.BusinessException;
import com.mall.common.PageResult;
import com.mall.dto.NoticeSaveDTO;
import com.mall.entity.Notice;
import com.mall.mapper.NoticeMapper;
import com.mall.service.NoticeService;
import com.mall.vo.NoticeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 公告服务实现
 *
 * <p>商城端只展示已发布公告；首次发布时写入 publish_time。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    /** 状态：已发布 */
    private static final int STATUS_PUBLISHED = 1;
    /** 状态：草稿 */
    private static final int STATUS_DRAFT = 0;
    /** 类型：平台公告 */
    private static final int TYPE_PLATFORM = 1;
    /** 类型：促销活动 */
    private static final int TYPE_PROMOTION = 2;
    /** 类型：帮助中心 */
    private static final int TYPE_HELP = 3;

    private final NoticeMapper noticeMapper;

    @Override
    public PageResult<NoticeVO> pageShop(Integer type, Integer page, Integer size) {
        Page<Notice> pageParam = buildPage(page, size);
        IPage<Notice> result = noticeMapper.selectPage(pageParam, Wrappers.<Notice>lambdaQuery()
                .eq(Notice::getStatus, STATUS_PUBLISHED)
                .eq(type != null, Notice::getType, type)
                .orderByDesc(Notice::getPublishTime)
                .orderByDesc(Notice::getId));
        return PageResult.of(result, this::toVO);
    }

    @Override
    public NoticeVO getShopDetail(Long id) {
        BusinessException.throwIf(id == null, "缺少公告ID");
        Notice notice = noticeMapper.selectById(id);
        BusinessException.throwIf(notice == null || !Integer.valueOf(STATUS_PUBLISHED).equals(notice.getStatus()),
                "公告不存在或尚未发布");
        return toVO(notice);
    }

    @Override
    public PageResult<NoticeVO> pageAdmin(Integer page, Integer size, Integer type, Integer status) {
        Page<Notice> pageParam = buildPage(page, size);
        IPage<Notice> result = noticeMapper.selectPage(pageParam, Wrappers.<Notice>lambdaQuery()
                .eq(type != null, Notice::getType, type)
                .eq(status != null, Notice::getStatus, status)
                .orderByDesc(Notice::getCreateTime)
                .orderByDesc(Notice::getId));
        return PageResult.of(result, this::toVO);
    }

    @Override
    public void save(NoticeSaveDTO dto) {
        BusinessException.throwIf(dto == null, "缺少公告信息");
        Notice notice = dto.getId() == null ? new Notice() : requireNotice(dto.getId());
        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setType(dto.getType() == null ? TYPE_PLATFORM : dto.getType());
        notice.setStatus(dto.getStatus() == null ? STATUS_DRAFT : dto.getStatus());
        // 首次发布时补发布时间
        if (Integer.valueOf(STATUS_PUBLISHED).equals(notice.getStatus()) && notice.getPublishTime() == null) {
            notice.setPublishTime(LocalDateTime.now());
        }
        if (dto.getId() == null) {
            noticeMapper.insert(notice);
        } else {
            noticeMapper.updateById(notice);
        }
    }

    @Override
    public void delete(Long id) {
        Notice notice = requireNotice(id);
        // 逻辑删除（notice.deleted）
        noticeMapper.deleteById(notice.getId());
    }

    @Override
    public void publish(Long id, Integer status) {
        BusinessException.throwIf(status == null
                || (status != STATUS_PUBLISHED && status != STATUS_DRAFT), "公告状态只能为 1已发布 或 0草稿");
        Notice notice = requireNotice(id);
        LocalDateTime publishTime = notice.getPublishTime();
        if (status == STATUS_PUBLISHED && publishTime == null) {
            publishTime = LocalDateTime.now();
        }
        noticeMapper.update(null, Wrappers.<Notice>lambdaUpdate()
                .eq(Notice::getId, notice.getId())
                .set(Notice::getStatus, status)
                .set(Notice::getPublishTime, publishTime)
                .set(Notice::getUpdateTime, LocalDateTime.now()));
    }

    // ------------------------------------------------------------------ 内部方法

    private Notice requireNotice(Long id) {
        BusinessException.throwIf(id == null, "缺少公告ID");
        Notice notice = noticeMapper.selectById(id);
        BusinessException.throwIf(notice == null, "公告不存在或已被删除");
        return notice;
    }

    /** 分页参数兜底：页码从 1 开始，单页最大 200 条 */
    private <T> Page<T> buildPage(Integer page, Integer size) {
        long current = page == null || page < 1 ? 1L : page;
        long limit = size == null || size < 1 ? 10L : Math.min(size, 200);
        return new Page<>(current, limit);
    }

    private NoticeVO toVO(Notice notice) {
        NoticeVO vo = new NoticeVO();
        vo.setId(notice.getId());
        vo.setTitle(notice.getTitle());
        vo.setContent(notice.getContent());
        vo.setType(notice.getType());
        vo.setTypeName(resolveTypeName(notice.getType()));
        vo.setStatus(notice.getStatus());
        vo.setPublishTime(notice.getPublishTime());
        vo.setCreateTime(notice.getCreateTime());
        return vo;
    }

    private String resolveTypeName(Integer type) {
        if (type == null) {
            return "";
        }
        if (type == TYPE_PLATFORM) {
            return "平台公告";
        }
        if (type == TYPE_PROMOTION) {
            return "促销活动";
        }
        if (type == TYPE_HELP) {
            return "帮助中心";
        }
        return "";
    }
}
