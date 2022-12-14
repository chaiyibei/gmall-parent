package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseCategory3;
import com.atguigu.gmall.model.to.CategoryViewTo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 柴小贝
* @description 针对表【base_category3(三级分类表)】的数据库操作Service
* @createDate 2022-08-22 21:48:33
*/
public interface BaseCategory3Service extends IService<BaseCategory3> {

    List<BaseCategory3> getCategory2Child(Long c2Id);

    /**
     * 根据三级分类ID查所有分类
     * @param c3Id
     * @return
     */
    CategoryViewTo getCategoryView(Long c3Id);
}
