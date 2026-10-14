package com.mall.controller.shop;

import com.mall.common.PageResult;
import com.mall.common.Result;
import com.mall.service.NoticeService;
import com.mall.vo.NoticeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商城公告接口（公开，仅已发布）
 */
@Tag(name = "商城-公告")
@RestController
@RequestMapping("/api/shop/notices")
@RequiredArgsConstructor
public class ShopNoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "公告列表")
    @GetMapping
    public Result<PageResult<NoticeVO>> list(@RequestParam(required = false) Integer type,
                                             @RequestParam(required = false, defaultValue = "1") Integer page,
                                             @RequestParam(required = false, defaultValue = "10") Integer size) {
        return Result.success(noticeService.pageShop(type, page, size));
    }

    @Operation(summary = "公告详情")
    @GetMapping("/{id}")
    public Result<NoticeVO> detail(@PathVariable Long id) {
        return Result.success(noticeService.getShopDetail(id));
    }
}
