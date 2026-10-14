package com.mall.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web MVC 配置：把本地磁盘的上传目录映射为静态资源访问路径
 *
 * <p>例如配置 upload.path=./uploads、url-prefix=/api/files，
 * 则 ./uploads/product/a.png 通过 /api/files/product/a.png 访问。</p>
 */
@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${mall.upload.path:./uploads}")
    private String uploadPath;

    @Value("${mall.upload.url-prefix:/api/files}")
    private String urlPrefix;

    /**
     * 上传根目录的绝对路径（不存在时自动创建）
     */
    public static Path resolveUploadRoot(String uploadPath) {
        Path root = Paths.get(uploadPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (Exception e) {
            log.error("创建上传目录失败: {}", root, e);
        }
        return root;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = resolveUploadRoot(uploadPath);
        String location = root.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
        log.info("静态资源映射: {}/** -> {}", urlPrefix, location);

        // Knife4j / Swagger 静态资源
        registry.addResourceHandler("/doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * 供上传服务复用的上传根目录
     */
    public Path uploadRoot() {
        return resolveUploadRoot(uploadPath);
    }

    /**
     * 上传目录（File 形式）
     */
    public File uploadDir() {
        return resolveUploadRoot(uploadPath).toFile();
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }
}
