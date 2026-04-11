package com.smart.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smart.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("orders")
public class Order extends BaseEntity {
    private String orderNo;
    private String senderName;
    private String senderPhone;
    private String senderAddress;
    private BigDecimal senderLng;
    private BigDecimal senderLat;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private BigDecimal receiverLng;
    private BigDecimal receiverLat;
    private String goodsName;
    private BigDecimal weight;
    private BigDecimal volume;
    /** pending_review / pending / dispatched / in_progress / completed / cancelled / exception */
    private String status;
    private String remark;
    private Long creatorId;
}
