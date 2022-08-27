package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.model.to.CategoryViewTo;
import com.atguigu.gmall.model.to.SkuDetailTo;
import com.atguigu.gmall.model.to.ValueSkuJsonTo;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.SkuImageService;
import com.atguigu.gmall.product.service.SpuSaleAttrService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.product.service.SkuInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;

/**
* @author 柴小贝
* @description 针对表【sku_info(库存单元表)】的数据库操作Service实现
* @createDate 2022-08-23 10:13:11
*/
@Service
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoMapper, SkuInfo>
    implements SkuInfoService{
    @Autowired
    SkuInfoMapper skuInfoMapper;
    @Autowired
    SkuImageMapper skuImageMapper;
    @Autowired
    SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    @Autowired
    BaseCategory3Mapper baseCategory3Mapper;
    @Autowired
    SkuImageService skuImageService;
    @Autowired
    SpuSaleAttrService spuSaleAttrService;

    @Transactional
    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        //1、保存到sku_info
        skuInfoMapper.insert(skuInfo);
        Long skuId = skuInfo.getId();

        //2、保存到sku_image
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (!CollectionUtils.isEmpty(skuImageList)){
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuId);
                skuImageMapper.insert(skuImage);
            }
        }

        //3、保存到sku_attr_value
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (!CollectionUtils.isEmpty(skuAttrValueList)){
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuId);
                skuAttrValueMapper.insert(skuAttrValue);
            }
        }

        //4、保存到sku_sale_attr_value
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)){
            for (SkuSaleAttrValue saleAttrValue : skuSaleAttrValueList) {
                saleAttrValue.setSpuId(skuInfo.getSpuId());
                saleAttrValue.setSkuId(skuId);
                skuSaleAttrValueMapper.insert(saleAttrValue);
            }
        }
    }

    @Override
    public void onSale(Long skuId) {
        skuInfoMapper.updateIsSale(skuId,1);
    }

    @Override
    public void cancelSale(Long skuId) {
        skuInfoMapper.updateIsSale(skuId,0);
    }

    @Override
    public SkuDetailTo getSkuDetail(Long skuId) {
        SkuDetailTo skuDetailTo = new SkuDetailTo();
        //0、查询到skuInfo
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);

        //2、商品（sku）的基本信息【价格、重量、名字。。。】 sku_info
        skuDetailTo.setSkuInfo(skuInfo);

        //3、商品（sku）的图片 sku_image
        List<SkuImage> imageList = skuImageService.getSkuImage(skuId);
        skuInfo.setSkuImageList(imageList);

        //1、商品（sku）所属的完整分类信息 base_category1、base_category2、base_category3
        CategoryViewTo categoryViewTo = baseCategory3Mapper.getCategoryView(skuInfo.getCategory3Id());
        skuDetailTo.setCategoryView(categoryViewTo);

        //实时价格
        BigDecimal price = get1010Price(skuId);
        skuDetailTo.setPrice(price);

        //4、商品（sku）所属的spu当时定义的所有销售属性名值组合，并高亮
        //      spu_sale_attr、spu_sale_attr_value、sku_sale_attr_value
        List<SpuSaleAttr> saleAttrList = spuSaleAttrService.getSaleAttrAndValueMarkSku(skuInfo.getSpuId(),skuId);
        skuDetailTo.setSpuSaleAttrList(saleAttrList);

        // 商品（sku）的所有兄弟产品的销售属性名和值组合关系全部查出来，并封装成
        // {“118|120": "50","119|121": "50"}这样的json字符串
        Long spuId = skuInfo.getSpuId();
        String valueSkuJson = spuSaleAttrService.getAllSkuValueJson(spuId);
        skuDetailTo.setValueSkuJson(valueSkuJson);

        //5、商品（sku）类似推荐
        //6、商品（sku）介绍[所属的spu的海报] sku_poster（×）
        //7、商品（sku）的规格参数 sku_attr_value
        //8、商品（sku）的售后、评论。。。（×）
        return skuDetailTo;
    }

    @Override
    public BigDecimal get1010Price(Long skuId) {
        BigDecimal price = skuInfoMapper.getPrice(skuId);
        return price;
    }
}




