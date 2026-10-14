package com.mall.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.BusinessException;
import com.mall.common.PageResult;
import com.mall.dto.BannerSaveDTO;
import com.mall.entity.Banner;
import com.mall.mapper.BannerMapper;
import com.mall.service.BannerService;
import com.mall.vo.BannerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 轮播图服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    /** 状态：启用 */
    private static final int STATUS_ENABLED = 1;
    /** 状态：停用 */
    private static final int STATUS_DISABLED = 0;
    /** 位置：商城首页 */
    private static final int POSITION_SHOP = 1;

    private final BannerMapper bannerMapper;

    @Override
    public List<BannerVO> listShop(Integer position) {
        List<Banner> banners = bannerMapper.selectList(Wrappers.<Banner>lambdaQuery()
                .eq(Banner::getStatus, STATUS_ENABLED)
                .eq(position != null, Banner::getPosition, position)
                .orderByAsc(Banner::getSort)
                .orderByAsc(Banner::getId));
        if (banners.isEmpty()) {
            return new ArrayList<>();
        }
        return banners.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public PageResult<BannerVO> pageAdmin(Integer page, Integer size, Integer position, Integer status) {
        Page<Banner> pageParam = buildPage(page, size);
        IPage<Banner> result = bannerMapper.selectPage(pageParam, Wrappers.<Banner>lambdaQuery()
                .eq(position != null, Banner::getPosition, position)
                .eq(status != null, Banner::getStatus, status)
                .orderByAsc(Banner::getSort)
                .orderByDesc(Banner::getId));
        return PageResult.of(result, this::toVO);
    }

    @Override
    public void save(BannerSaveDTO dto) {
        BusinessException.throwIf(dto == null, "缺少轮播图信息");
        Banner banner = dto.getId() == null ? new Banner() : requireBanner(dto.getId());
        banner.setTitle(dto.getTitle());
        banner.setImage(dto.getImage());
        banner.setLinkUrl(dto.getLinkUrl());
        banner.setPosition(dto.getPosition() == null ? POSITION_SHOP : dto.getPosition());
        banner.setSort(dto.getSort() == null ? 0 : dto.getSort());
        banner.setStatus(dto.getStatus() == null ? STATUS_ENABLED : dto.getStatus());
        if (dto.getId() == null) {
            bannerMapper.insert(banner);
        } else {
            bannerMapper.updateById(banner);
        }
    }

    @Override
    public void delete(Long id) {
        Banner banner = requireBanner(id);
        // 逻辑删除（banner.deleted）
        bannerMapper.deleteById(banner.getId());
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        BusinessException.throwIf(status == null || (status != STATUS_ENABLED && status != STATUS_DISABLED),
                "轮播图状态只能为 1启用 或 0停用");
        Banner banner = requireBanner(id);
        bannerMapper.update(null, Wrappers.<Banner>lambdaUpdate()
                .eq(Banner::getId, banner.getId())
                .set(Banner::getStatus, status));
    }

    // ------------------------------------------------------------------ 内部方法

    private Banner requireBanner(Long id) {
        BusinessException.throwIf(id == null, "缺少轮播图ID");
        Banner banner = bannerMapper.selectById(id);
        BusinessException.throwIf(banner == null, "轮播图不存在或已被删除");
        return banner;
    }

    /** 分页参数兜底：页码从 1 开始，单页最大 200 条 */
    private <T> Page<T> buildPage(Integer page, Integer size) {
        long current = page == null || page < 1 ? 1L : page;
        long limit = size == null || size < 1 ? 10L : Math.min(size, 200);
        return new Page<>(current, limit);
    }

    private BannerVO toVO(Banner banner) {
        BannerVO vo = new BannerVO();
        vo.setId(banner.getId());
        vo.setTitle(banner.getTitle());
        vo.setImage(banner.getImage());
        vo.setLinkUrl(banner.getLinkUrl());
        vo.setPosition(banner.getPosition());
        vo.setSort(banner.getSort());
        vo.setStatus(banner.getStatus());
        vo.setCreateTime(banner.getCreateTime());
        return vo;
    }
}
