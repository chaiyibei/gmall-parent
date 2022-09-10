package com.atguigu.gmall.model.vo.search;

import lombok.Data;

/**
 * 封装检索条件
 */
@Data
public class SearchParamVo {

    Long category1Id;
    Long category2Id;
    Long category3Id;
    String keyword;
    String trademark;
    String[] props;
    String order = "1:desc";
    Integer pageNo = 1;
}
