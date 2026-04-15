package com.example.order_service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class OrderController {
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private static AtomicInteger count = new AtomicInteger();

    @PostMapping("/order")
    public ResponseEntity<String> createOrder(@RequestBody String orderId) {
        log.info("Order created for {}", orderId);
        count.incrementAndGet();
        return new ResponseEntity<>("Order created", HttpStatus.CREATED);
    }

    @GetMapping("/order-count")
    public ResponseEntity<Integer> orderCount() {
        int orderCount = count.get();
        log.info("{} orders created", orderCount);
        count.set(0);
        return new ResponseEntity<>(orderCount, HttpStatus.OK);
    }
}