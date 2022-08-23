package com.atguigu.gmall.product.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioClientConfig {
    private String endpoint;
    private String bucketName;
    private String accessKey;
    private String secretKey;

    @Bean
    public MinioClient minioClient(){
        try {
            MinioClient minioClient = new MinioClient(endpoint,accessKey,secretKey);
            return minioClient;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
