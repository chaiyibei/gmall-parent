package com.atguigu.gmall.product;

import io.minio.MinioClient;
import io.minio.PutObjectOptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;

//@SpringBootTest
public class MinioTest {
    @Test
    public void uploadFile(){
        try {
            // 使用MinIO服务的URL，端口，Access key和Secret key创建一个MinioClient对象
            MinioClient minioClient = new MinioClient("http://192.168.6.100:9000",
                    "admin",
                    "admin123456");

            // 检查存储桶是否已经存在
            boolean isExist = minioClient.bucketExists("gmall");
            if(isExist) {
                System.out.println("Bucket already exists.");
            } else {
                // 创建一个名为asiatrip的存储桶，用于存储照片的zip文件。
                minioClient.makeBucket("gmall");
            }

            //文件流
            FileInputStream inputStream = new FileInputStream("E:\\05 图片文档\\img_其他\\GEM.png");
            //文件上传参数
            PutObjectOptions options = new PutObjectOptions(inputStream.available(),-1l);
            options.setContentType("image/png");
            // 使用putObject上传一个文件到存储桶中。
            minioClient.putObject("gmall","GEM.png", inputStream,options);
            System.out.println("上传成功");
        } catch(Exception e) {
            System.out.println("Error occurred: " + e);
        }
    }
}
