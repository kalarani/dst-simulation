package com.example.payment_service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class PaymentController {
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private static final AtomicInteger count = new AtomicInteger();
    private static final Set<String> processedOrders = new HashSet<>();

    @Value("${be.idempotent}")
    private Boolean beIdempotent;

    @PostMapping("/payment")
    public ResponseEntity<String> initiatePayment(@RequestBody String orderId) {
        log.info("Payment initiated for {}", orderId);

        chargePayment(orderId);

        return new ResponseEntity<>("Payment initiated", HttpStatus.CREATED);
    }

    private void chargePayment(String orderId) {
        if (beIdempotent) {
            System.out.println("Being idempotent - Keeping stock of processed orders");
            if (!processedOrders.contains(orderId)) {
                processedOrders.add(orderId);
                count.incrementAndGet();
            }
        } else {
            System.out.println("Being buggy - incrementing payment count always");
            count.incrementAndGet();
        }
    }

    @GetMapping("/payment-count")
    public ResponseEntity<Integer> paymentCount() {
        int paymentCount = count.get();
        log.info("{} payments charged", paymentCount);
        count.set(0);
        return new ResponseEntity<>(paymentCount, HttpStatus.OK);
    }
}
