package com.codenza.shopsphere.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.codenza.shopsphere.entity.Order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendOrderConfirmation(String toEmail, Order order) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setFrom("no-reply@shopsphere.com");
            message.setSubject("Your ShopSphere order #" + order.getId() + " is confirmed");
            message.setText(
                    "Hi,\n\nThanks for your order! Order #" + order.getId()
                            + " has been placed successfully.\nTotal: ₹" + order.getTotalAmount()
                            + "\n\nWe'll notify you when it ships.\n\n- ShopSphere");

            mailSender.send(message);
            log.info("Order confirmation email sent for order {}", order.getId());

        } catch (Exception ex) {
            log.error("Failed to send order confirmation email for order {}: {}", order.getId(), ex.getMessage());
        }
    }
}