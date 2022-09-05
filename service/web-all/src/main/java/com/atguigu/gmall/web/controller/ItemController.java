package com.atguigu.gmall.web.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.feign.item.SkuDetailFeignClient;
import com.atguigu.gmall.model.to.SkuDetailTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 商品详情页
 */
@Controller
public class ItemController {
    @Autowired
    SkuDetailFeignClient skuDetailFeignClient;

    @GetMapping("/{skuId}.html")
    public String item(@PathVariable("skuId")Long skuId,
                       Model model){
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

        Result<SkuDetailTo> result = skuDetailFeignClient.getSkuDetail(skuId);
        if (result.isOk()){
            SkuDetailTo skuDetailTo = result.getData();

            if (skuDetailTo == null || skuDetailTo.getSkuInfo() == null){
                //说明远程没有查到商品
                return "item/404";
            }
            model.addAttribute("categoryView",skuDetailTo.getCategoryView());
            model.addAttribute("skuInfo",skuDetailTo.getSkuInfo());
            model.addAttribute("price",skuDetailTo.getPrice());
            model.addAttribute("spuSaleAttrList",skuDetailTo.getSpuSaleAttrList());
            model.addAttribute("valuesSkuJson",skuDetailTo.getValueSkuJson()); //注意values和value
        }
        return "item/index";
    }
}
