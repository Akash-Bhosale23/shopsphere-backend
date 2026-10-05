package com.codenza.shopsphere.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.codenza.shopsphere.config.RabbitMQConfig;
import com.codenza.shopsphere.event.OrderPlacedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final JavaMailSender mailSender;

    @RabbitListener(queues = RabbitMQConfig.ORDER_PLACED_QUEUE)
    public void handleOrderPlaced(OrderPlacedEvent event) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(event.customerEmail());
            message.setFrom("no-reply@shopsphere.com");
            message.setSubject("Your ShopSphere order #" + event.orderId() + " is confirmed");
            message.setText(
                    "Hi,\n\nThanks for your order! Order #" + event.orderId()
                            + " has been placed successfully.\nTotal: ₹" + event.totalAmount()
                            + "\n\nWe'll notify you when it ships.\n\n- ShopSphere");

            mailSender.send(message);
            log.info("Order confirmation email sent for order {} (via RabbitMQ)", event.orderId());

        } catch (Exception ex) {
            log.error("Failed to send order confirmation for order {}: {}", event.orderId(), ex.getMessage());
        }
    }
}