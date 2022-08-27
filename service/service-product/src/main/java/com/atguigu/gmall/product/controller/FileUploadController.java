package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.service.FileUploadService;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Slf4j
@RequestMapping("/admin/product")
@RestController
public class FileUploadController {
    @Autowired
    FileUploadService fileUploadService;

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
    public Result fileUpload(@RequestPart("file")MultipartFile file) throws Exception {
       String url = fileUploadService.upload(file);
       return Result.ok(url);
    }

    @DeleteMapping("/fileDelete")
    public Result fileDelete(@RequestPart("objectName")String objectName){
        return null;
    }
}












