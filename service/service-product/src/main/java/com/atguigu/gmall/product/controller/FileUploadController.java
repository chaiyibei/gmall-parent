package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import io.minio.MinioClient;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Slf4j
@RequestMapping("/admin/product")
@RestController
public class FileUploadController {
    @Value("${minio.bucketName}")
    private String bucketName;
    @Autowired
    private MinioClient minioClient;

    /**
     * 文件上传功能
     * Post请求数据在请求体（包含了文件[流]）
     *
     * @RequestParam("file")MultipartFile file
     * @RequestPart("file")MultipartFile file 专门处理文件的
     *
     * 各种注解接不同位置的请求数据
     * @RequestParam：无论是什么请求 接请求参数： 用一个Pojo把所有数据都接了
     * @RequestPart：接请求参数里的文件项
     * @RequestBody：接请求体中的所有数据 (json转为pojo)
     * @PathVariable：接路径上的动态变量
     * @RequestHeader：获取浏览器发送的请求头中的某些数据
     * @CookieValue：获取浏览器发送的请求的Cookie值
     *
     * @return
     */
    @PostMapping("/fileUpload")
    public Result fileUpload(@RequestPart("file")MultipartFile file) throws IOException {
        log.info("开始处理文件上传，文件名是{}",file.getOriginalFilename());

        try {
            boolean exists = minioClient.bucketExists(bucketName);
            if (exists){
                log.info("存储桶已经存在！");
            }else {
                minioClient.makeBucket(bucketName);
            }
            //
            String filename = file.getOriginalFilename();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            //设置存储对象名称
            String objectName = sdf.format(new Date()) + "/" + filename;
            //使用putObject上传一个文件到存储桶中
            minioClient.putObject(bucketName,objectName,file.getInputStream(),file.getContentType());
            log.info("文件上传成功");
            //拼接将要返回的字符串
            String url = "http:192.168.6.100:9000" + "/" + bucketName + "/" + objectName;
            return Result.ok(url);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(null);
        }
    }

    @DeleteMapping("/fileDelete")
    public Result fileDelete(@RequestPart("objectName")String objectName){
        try {
            minioClient.removeObject(bucketName,objectName);
            return Result.ok();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.fail();
    }
}












