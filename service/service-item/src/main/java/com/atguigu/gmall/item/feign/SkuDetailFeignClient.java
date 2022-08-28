package com.atguigu.gmall.item.feign;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuImage;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.model.to.CategoryViewTo;
import com.atguigu.gmall.model.to.SkuDetailTo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;

@RequestMapping("/api/inner/rpc/product")
@FeignClient("service-product")
public interface SkuDetailFeignClient {
//    @GetMapping("/skuDetail/{skuId}")
//    public Result<SkuDetailTo> getSkuDetail(@PathVariable("skuId")Long skuId);

    /**
     * 查询sku的基本信息
     * @param skuId
     * @return
     */
    @GetMapping("/skuDetail/info/{skuId}")
    public Result<SkuInfo> getSkuInfo(@PathVariable("skuId")Long skuId);

    /**
     * 查询sku的图片信息
     * @param skuId
     * @return
     */
    @GetMapping("/skuDetail/image/{skuId}")
    public Result<List<SkuImage>> getSkuImages(@PathVariable("skuId")Long skuId);

    /**
     * 查询sku的实时价格
     * @param skuId
     * @return
     */
    @GetMapping("/skuDetail/price/{skuId}")
    public Result<BigDecimal> getSku1010Price(@PathVariable("skuId")Long skuId);

    /**
     * 查询sku对应的spu定义的所有属性名和值
     * @return
     */
    @GetMapping("/skuDetail/saleAttrValues/{skuId}/{spuId}")
    public Result<List<SpuSaleAttr>> getSkuSaleAttrValues(@PathVariable("skuId")Long skuId,
                                                          @PathVariable("spuId")Long spuId);

    /**
     * 查询sku组合 valueJson
     * @param spuId
     * @return
     */
    @GetMapping("/skuDetail/valueJson/{spuId}")
    public Result<String> getSkuValueJson(@PathVariable("spuId")Long spuId);

    /**
     * 查分类
     * @param c3Id
     * @return
     */
    @GetMapping("/skuDetail/categoryView/{c3Id}")
    public Result<CategoryViewTo> getCategoryView(@PathVariable("c3Id")Long c3Id);
}
