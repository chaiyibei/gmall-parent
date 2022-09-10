package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.list.SearchAttr;
import com.atguigu.gmall.model.product.SkuAttrValue;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 柴小贝
* @description 针对表【sku_attr_value(sku平台属性值关联表)】的数据库操作Service
* @createDate 2022-08-23 10:13:11
*/
public interface SkuAttrValueService extends IService<SkuAttrValue> {
    /**
     * 查询当前sku所有的平台属性名和值
     * @param skuId
     * @return
     */
    List<SearchAttr> getSkuAttrNameAndValue(Long skuId);

}
