package com.basebackend.mall.product.service.impl;

import com.basebackend.mall.product.entity.MallSku;
import com.basebackend.mall.product.event.MallOrderPayStatus;
import com.basebackend.mall.product.event.MallOrderStatus;
import com.basebackend.mall.product.event.OrderCreatedMessage;
import com.basebackend.mall.product.mapper.MallSkuMapper;
import com.basebackend.common.idempotent.store.IdempotentStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ProductServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl 单元测试")
class ProductServiceImplTest {

    @Mock
    private MallSkuMapper mallSkuMapper;

    @Mock
    private IdempotentStore idempotentStore;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    @DisplayName("下单预占库存应增加锁定库存")
    void shouldIncreaseLockedStockWhenReserveStock() {
        MallSku mallSku = new MallSku();
        mallSku.setId(10001L);
        mallSku.setStockQuantity(20);
        mallSku.setLockQuantity(5);
        mallSku.setSalePrice(new BigDecimal("99.00"));

        when(mallSkuMapper.selectById(10001L)).thenReturn(mallSku);
        when(idempotentStore.tryAcquire(any(), anyLong(), any())).thenReturn(true);

        OrderCreatedMessage message = new OrderCreatedMessage(
                301L,
                "TRD202603030001",
                10001L,
                new BigDecimal("99.00"),
                MallOrderStatus.CREATED,
                MallOrderPayStatus.UNPAID,
                List.of(new OrderCreatedMessage.OrderItem(10001L, 3))
        );

        productService.reserveStockForOrder(message);

        ArgumentCaptor<MallSku> skuCaptor = ArgumentCaptor.forClass(MallSku.class);
        verify(mallSkuMapper).updateById(skuCaptor.capture());
        MallSku updatedSku = skuCaptor.getValue();
        assertEquals(20, updatedSku.getStockQuantity());
        assertEquals(8, updatedSku.getLockQuantity());
    }

    @Test
    @DisplayName("库存不足时预占应抛出异常")
    void shouldThrowExceptionWhenReserveStockIfInventoryInsufficient() {
        MallSku mallSku = new MallSku();
        mallSku.setId(10001L);
        mallSku.setStockQuantity(5);
        mallSku.setLockQuantity(4);

        when(mallSkuMapper.selectById(10001L)).thenReturn(mallSku);
        when(idempotentStore.tryAcquire(any(), anyLong(), any())).thenReturn(true);

        OrderCreatedMessage message = new OrderCreatedMessage(
                302L,
                "TRD202603030002",
                10002L,
                new BigDecimal("99.00"),
                MallOrderStatus.CREATED,
                MallOrderPayStatus.UNPAID,
                List.of(new OrderCreatedMessage.OrderItem(10001L, 2))
        );

        assertThrows(IllegalStateException.class, () -> productService.reserveStockForOrder(message));
        verify(mallSkuMapper, never()).updateById(any(MallSku.class));
    }
}
