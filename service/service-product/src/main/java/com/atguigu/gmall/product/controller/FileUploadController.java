package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/admin/product")
@RestController
public class FileUploadController {

    /**
     *
     * @RequestParam("file")MultipartFile file
     * @RequestPart("file")MultipartFile file
     *
     * @RequestParam：无论是什么请求 接请求参数
     * @RequestPart：接请求参数里的文件项
     * @RequestBody：接请求体中的所有数据
     * @PathVariable：接路径上的动态变量
     *
     * @return
     */
    @PostMapping("/fileUpload")
    public Result fileUpload(@RequestPart("file")MultipartFile file){

        return Result.ok();
    }
}
