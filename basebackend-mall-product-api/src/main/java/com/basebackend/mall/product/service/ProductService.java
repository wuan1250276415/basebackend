package com.basebackend.mall.product.service;

import com.basebackend.mall.product.dto.ProductDetailDTO;
import com.basebackend.mall.product.event.OrderCancelledMessage;
import com.basebackend.mall.product.event.OrderCreatedMessage;
import com.basebackend.mall.product.event.OrderTimeoutClosedMessage;
import com.basebackend.mall.product.event.PaymentSucceededMessage;

/**
 * 商品服务接口
 */
public interface ProductService {

    /**
     * 健康检查信息
     *
     * @return 服务状态
     */
    String ping();

    /**
     * 根据 SKU ID 获取商品详情
     *
     * @param skuId SKU ID
     * @return 商品详情
     */
    ProductDetailDTO getProductBySkuId(Long skuId);

    /**
     * 处理支付成功后的库存扣减
     *
     * @param message 支付成功消息
     */
    void deductStockForPaidOrder(PaymentSucceededMessage message);

    /**
     * 处理下单后的库存预占
     *
     * @param message 订单创建消息
     */
    void reserveStockForOrder(OrderCreatedMessage message);

    /**
     * 处理订单取消后的库存释放
     *
     * @param message 订单取消消息
     */
    void releaseReservedStockForCancelledOrder(OrderCancelledMessage message);

    /**
     * 处理订单超时关闭后的库存释放
     *
     * @param message 订单超时关闭消息
     */
    void releaseReservedStockForTimeoutOrder(OrderTimeoutClosedMessage message);
}
