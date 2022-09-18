package com.atguigu.gmall.model.to.mq;

import lombok.Data;

@Data
public class WareDeductStatusMsg {
    private Long orderId;
    private String status;
}
