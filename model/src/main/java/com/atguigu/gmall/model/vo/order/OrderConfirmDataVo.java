package com.atguigu.gmall.model.vo.order;

import com.atguigu.gmall.model.user.UserAddress;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单确认页
 * detailArrayList  totalNum  totalAmount  userAddressList  tradeNo
 */
@Data
public class OrderConfirmDataVo {
    private List<CartInfoVo> detailArrayList;
    private Integer totalNum;
    private BigDecimal totalAmount;
    private  List<UserAddress> userAddressList;
    private String tradeNo;
}
