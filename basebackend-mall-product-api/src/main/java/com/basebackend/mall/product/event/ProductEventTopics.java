package com.basebackend.mall.product.event;

/**
 * 商品域事件 Topic 约定
 */
public final class ProductEventTopics {

    private ProductEventTopics() {
    }

    /**
     * 商品上架
     */
    public static final String PRODUCT_ON_SHELF = "mall.product.on-shelf";

    /**
     * 商品下架
     */
    public static final String PRODUCT_OFF_SHELF = "mall.product.off-shelf";

    /**
     * 库存预占
     */
    public static final String STOCK_RESERVED = "mall.stock.reserved";

    /**
     * 库存释放
     */
    public static final String STOCK_RELEASED = "mall.stock.released";
}
