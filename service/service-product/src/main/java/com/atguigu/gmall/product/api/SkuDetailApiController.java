package com.atguigu.gmall.product.api;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuImage;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.model.to.CategoryViewTo;
import com.atguigu.gmall.model.to.SkuDetailTo;
import com.atguigu.gmall.product.service.BaseCategory3Service;
import com.atguigu.gmall.product.service.SkuInfoService;
import com.atguigu.gmall.product.service.SpuSaleAttrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RequestMapping("/api/inner/rpc/product")
@RestController
public class SkuDetailApiController {
    @Autowired
    SkuInfoService skuInfoService;

    @Autowired
    SpuSaleAttrService spuSaleAttrService;

    @Autowired
    BaseCategory3Service baseCategory3Service;

//    @GetMapping("/skuDetail/{skuId}")
//    public Result<SkuDetailTo> getSkuDetail(@PathVariable("skuId")Long skuId){
//        SkuDetailTo skuDetailTo = skuInfoService.getSkuDetail(skuId);
//        return Result.ok(skuDetailTo);
//    }

    /**
     * 查询sku的基本信息
     * @param skuId
     * @return
     */
    @GetMapping("/skuDetail/info/{skuId}")
    public Result<SkuInfo> getSkuInfo(@PathVariable("skuId")Long skuId){
        SkuInfo skuInfo = skuInfoService.getDetailSkuInfo(skuId);
        return Result.ok(skuInfo);
    }

    /**
     * 查询sku的图片信息
     * @param skuId
     * @return
     */
    @GetMapping("/skuDetail/image/{skuId}")
    public Result<List<SkuImage>> getSkuImages(@PathVariable("skuId")Long skuId){
        List<SkuImage> images = skuInfoService.getDetailSkuImages(skuId);
        return Result.ok(images);
    }

    /**
     * 查询sku的实时价格
     * @param skuId
     * @return
     */
    @GetMapping("/skuDetail/price/{skuId}")
    public Result<BigDecimal> getSku1010Price(@PathVariable("skuId")Long skuId){
        BigDecimal price = skuInfoService.get1010Price(skuId);
        return Result.ok(price);
    }

    /**
     * 查询sku对应的spu定义的所有属性名和值
     * @return
     */
    @GetMapping("/skuDetail/saleAttrValues/{skuId}/{spuId}")
    public Result<List<SpuSaleAttr>> getSkuSaleAttrValues(@PathVariable("skuId")Long skuId,
                                         @PathVariable("spuId")Long spuId){
        List<SpuSaleAttr> saleAttrList = spuSaleAttrService
                .getSaleAttrAndValueMarkSku(spuId,skuId);
        return Result.ok(saleAttrList);
    }

    /**
     * 查询sku组合 valueJson
     * @param spuId
     * @return
     */
    @GetMapping("/skuDetail/valueJson/{spuId}")
    public Result<String> getSkuValueJson(@PathVariable("spuId")Long spuId){
        String valueJson = spuSaleAttrService.getAllSkuSaleAttrValueJson(spuId);
        return Result.ok(valueJson);
    }

    /**
     * 查分类
     * @param c3Id
     * @return
     */
    @GetMapping("/skuDetail/categoryView/{c3Id}")
    public Result<CategoryViewTo> getCategoryView(@PathVariable("c3Id")Long c3Id){
        CategoryViewTo categoryViewTo = baseCategory3Service.getCategoryView(c3Id);
        return Result.ok(categoryViewTo);
    }
}
