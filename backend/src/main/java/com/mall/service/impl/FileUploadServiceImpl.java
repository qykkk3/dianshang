package com.mall.service.impl;

import com.mall.common.BusinessException;
import com.mall.config.WebMvcConfig;
import com.mall.service.FileUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 文件上传服务实现：本地磁盘存储
 *
 * <p>存储规则：{mall.upload.path}/{type}/yyyyMM/{uuid}.{ext}，
 * 访问地址：{mall.upload.url-prefix}/{type}/yyyyMM/{uuid}.{ext}</p>
 */
@Slf4j
@Service
public class FileUploadServiceImpl implements FileUploadService {

    /** 单文件大小上限：10MB */
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    /** 允许的业务类型 */
    private static final Set<String> ALLOW_TYPES = Set.of("product", "banner", "avatar");
    /** 默认业务类型 */
    private static final String DEFAULT_TYPE = "product";

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    @Value("${mall.upload.path:./uploads}")
    private String uploadPath;

    @Value("${mall.upload.url-prefix:/api/files}")
    private String urlPrefix;

    @Value("${mall.upload.allow-ext:jpg,jpeg,png,gif,bmp,webp,svg}")
    private String allowExt;

    @Override
    public Map<String, Object> upload(MultipartFile file, String type) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过 10MB");
        }
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new BusinessException("上传文件名不能为空");
        }
        String ext = extensionOf(originalFilename);
        if (!StringUtils.hasText(ext)) {
            throw new BusinessException("无法识别文件类型，请上传带扩展名的文件，仅支持 " + allowExt);
        }
        if (!allowedExtensions().contains(ext)) {
            throw new BusinessException("不支持的文件类型：" + ext + "，仅支持 " + allowExt);
        }
        String dirType = resolveType(type);
        String month = LocalDate.now().format(MONTH_FORMATTER);
        String newFileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;

        Path root = WebMvcConfig.resolveUploadRoot(uploadPath);
        Path dir = root.resolve(dirType).resolve(month).normalize();
        // 目录穿越防护
        if (!dir.startsWith(root)) {
            throw new BusinessException("上传目录不合法");
        }
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(newFileName).normalize();
            if (!target.startsWith(root)) {
                throw new BusinessException("上传目录不合法");
            }
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("文件上传失败：{}", originalFilename, e);
            throw new BusinessException("文件上传失败：" + e.getMessage());
        }

        String prefix = normalizePrefix();
        String url = prefix + "/" + dirType + "/" + month + "/" + newFileName;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", url);
        result.put("name", originalFilename);
        result.put("size", file.getSize());
        return result;
    }

    @Override
    public void delete(String url) {
        if (!StringUtils.hasText(url)) {
            throw new BusinessException("文件地址不能为空");
        }
        String relative = url.trim();
        String prefix = normalizePrefix();
        if (relative.startsWith(prefix)) {
            relative = relative.substring(prefix.length());
        }
        relative = relative.replace('\\', '/');
        while (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        if (!StringUtils.hasText(relative)) {
            throw new BusinessException("文件地址不合法");
        }
        Path root = WebMvcConfig.resolveUploadRoot(uploadPath);
        Path target = root.resolve(relative).normalize();
        // 必须位于上传根目录内，防止目录穿越
        if (!target.startsWith(root) || target.equals(root)) {
            throw new BusinessException("文件地址不合法");
        }
        try {
            if (!Files.deleteIfExists(target)) {
                log.warn("待删除的文件不存在：{}", target);
            }
        } catch (IOException e) {
            log.error("文件删除失败：{}", target, e);
            throw new BusinessException("文件删除失败：" + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- 内部方法

    private String resolveType(String type) {
        if (!StringUtils.hasText(type)) {
            return DEFAULT_TYPE;
        }
        String value = type.trim().toLowerCase(Locale.ROOT);
        return ALLOW_TYPES.contains(value) ? value : DEFAULT_TYPE;
    }

    private String normalizePrefix() {
        String prefix = StringUtils.hasText(urlPrefix) ? urlPrefix.trim() : "/api/files";
        while (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }

    private String extensionOf(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index + 1).trim().toLowerCase(Locale.ROOT);
    }

    private Set<String> allowedExtensions() {
        if (!StringUtils.hasText(allowExt)) {
            return Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg");
        }
        return Arrays.stream(allowExt.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(item -> item.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
