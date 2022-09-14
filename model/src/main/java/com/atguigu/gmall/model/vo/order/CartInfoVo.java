package com.atguigu.gmall.model.vo.order;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartInfoVo {
    private Long skuId;
    private String imgUrl;
    private String skuName;
    private BigDecimal orderPrice; //实时价格
    private Integer skuNum;
    private String hasStock = "1"; //是否又会
}
