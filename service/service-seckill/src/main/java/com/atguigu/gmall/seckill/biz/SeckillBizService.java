package com.atguigu.gmall.seckill.biz;

import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.vo.seckill.SeckillOrderConfirmVo;

public interface SeckillBizService {
    /**
     * 生成秒杀码
     * @param skuId
     * @return
     */
    String generateSeckillCode(Long skuId);

    /**
     * 校验秒杀码
     * @param skuId
     * @param code
     * @return
     */
    Boolean checkSeckillCode(Long skuId,String code);

    /**
     * 秒杀预下单
     * @param skuId
     * @param skuIdStr
     * @return
     */
    ResultCodeEnum seckillOrder(Long skuId, String skuIdStr);

    /**
     * 检查秒杀单状态
     * @param skuId
     * @return
     */
    ResultCodeEnum checkSeckillOrderStatus(Long skuId);

    /**
     * 获取秒杀确认页数据
     * @param skuId
     * @return
     */
    SeckillOrderConfirmVo getSeckillOrderConfirmVo(Long skuId);

    Long submitSeckillOrder(OrderInfo orderInfo);
}
