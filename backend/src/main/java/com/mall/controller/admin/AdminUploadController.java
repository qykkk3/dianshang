package com.mall.controller.admin;

import com.mall.common.Result;
import com.mall.common.annotation.OperLog;
import com.mall.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 管理端文件上传接口（仅需管理端登录，具体权限由登录态保证）
 */
@Tag(name = "管理端-文件上传")
@RestController
@RequestMapping("/api/admin/upload")
@RequiredArgsConstructor
public class AdminUploadController {

    private final FileUploadService fileUploadService;

    @Operation(summary = "上传图片，返回 url/name/size")
    @PostMapping
    @OperLog(module = "文件管理", value = "上传文件", saveParams = false)
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                              @RequestParam(value = "type", required = false) String type) {
        return Result.success("上传成功", fileUploadService.upload(file, type));
    }

    @Operation(summary = "删除文件")
    @DeleteMapping
    @OperLog(module = "文件管理", value = "删除文件", saveParams = false)
    public Result<Void> delete(@RequestParam("url") String url) {
        fileUploadService.delete(url);
        return Result.success("删除成功", null);
    }
}
