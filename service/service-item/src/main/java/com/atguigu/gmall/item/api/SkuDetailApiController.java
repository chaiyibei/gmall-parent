package com.atguigu.gmall.item.api;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.service.SkuDetailService;
import com.atguigu.gmall.model.to.SkuDetailTo;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@Api(tags = "三级分类商品详情的RPC接口")
@RequestMapping("/api/inner/rpc/item")
@RestController
public class SkuDetailApiController {
    //远程查询出商品的详细信息
    //1、商品（sku）所属的完整分类信息 base_category1、base_category2、base_category3
    //2、商品（sku）的基本信息【价格、重量、名字。。。】 sku_info
    //3、商品（sku）的图片 sku_image
    //4、商品（sku）所属的spu当时定义的所有销售属性名值组合，并高亮
    //      spu_sale_attr、spu_sale_attr_value、sku_sale_attr_value
    //5、商品（sku）类似推荐
    //6、商品（sku）介绍[所属的spu的海报] sku_poster（×）
    //7、商品（sku）的规格参数 sku_attr_value
    //8、商品（sku）的售后、评论。。。（×）

    @Autowired
    SkuDetailService skuDetailService;

    @GetMapping("/skuDetail/{skuId}")
    public Result<SkuDetailTo> getSkuDetail(@PathVariable("skuId")Long skuId){
        SkuDetailTo skuDetailTo = skuDetailService.getSkuDetail(skuId);
        return Result.ok(skuDetailTo);
    }
}
