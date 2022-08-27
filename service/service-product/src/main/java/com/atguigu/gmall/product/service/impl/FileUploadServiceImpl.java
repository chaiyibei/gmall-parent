package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.product.config.MinioAutoConfiguration;
import com.atguigu.gmall.product.config.MinioProperties;
import com.atguigu.gmall.product.service.FileUploadService;
import io.minio.MinioClient;
import io.minio.PutObjectOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.Date;
import java.util.UUID;

@Service
public class FileUploadServiceImpl implements FileUploadService {
    @Autowired
    MinioClient minioClient;
    @Autowired
    MinioProperties minioProperties;

    @Override
    public String upload(MultipartFile file) throws Exception {

        //给桶里面上传文件
        String filename = file.getOriginalFilename();
        filename = UUID.randomUUID().toString().replace("-", "") + "_" + filename;
        String objectName = DateUtil.formatDate(new Date()) + "/" + filename;

        //文件上传参数
        PutObjectOptions options = new PutObjectOptions(file.getSize(),-1l);
        options.setContentType(file.getContentType());
        // 使用putObject上传一个文件到存储桶中。
        minioClient.putObject(minioProperties.getBucketName(),objectName, file.getInputStream(),options);
        System.out.println("上传成功");
        String url = minioProperties.getEndpoint() + "/" + minioProperties.getBucketName() + "/" + objectName;

        return url;
    }
}









