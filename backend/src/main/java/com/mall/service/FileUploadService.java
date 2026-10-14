package com.mall.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件上传服务（本地磁盘存储）
 */
public interface FileUploadService {

    /**
     * 上传文件
     *
     * @param file 上传文件
     * @param type 业务类型：product / banner / avatar，非法时默认 product
     * @return url / name / size
     */
    Map<String, Object> upload(MultipartFile file, String type);

    /**
     * 删除文件：把 /api/files/xxx 形式的地址映射回本地文件并删除
     */
    void delete(String url);
}
