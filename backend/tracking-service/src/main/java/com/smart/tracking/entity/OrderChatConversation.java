package com.smart.tracking.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smart.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_chat_conversations")
public class OrderChatConversation extends BaseEntity {
    private Long orderId;
}
