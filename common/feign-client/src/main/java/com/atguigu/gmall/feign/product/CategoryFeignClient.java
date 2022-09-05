package com.atguigu.gmall.feign.product;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.to.CategoryTreeTo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient("service-product") //告诉SpringBoot 这是一个远程调用的客户端
public interface CategoryFeignClient {

    @GetMapping("/api/inner/rpc/product/category/tree")
    Result<List<CategoryTreeTo>> getAllCategoryWithTree();
}
