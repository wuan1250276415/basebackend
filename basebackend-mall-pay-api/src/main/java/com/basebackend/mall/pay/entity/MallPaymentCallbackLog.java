package com.basebackend.mall.pay.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 支付回调日志实体
 */
@Data
@TableName("mall_payment_callback_log")
public class MallPaymentCallbackLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("pay_no")
    private String payNo;

    @TableField("pay_channel")
    private String payChannel;

    @TableField("callback_payload")
    private String callbackPayload;

    @TableField("sign_verified")
    private Integer signVerified;

    @TableField("process_status")
    private String processStatus;

    @TableField("process_message")
    private String processMessage;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
