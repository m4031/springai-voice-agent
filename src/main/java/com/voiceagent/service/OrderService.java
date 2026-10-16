package com.voiceagent.service;

import com.voiceagent.dto.OrderStatus;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    public OrderStatus getOrderStatus(String orderId) {

        return new OrderStatus( orderId, "SHIPPED", "Tomorrow" );
    }
}
