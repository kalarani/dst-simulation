package com.example.cart_service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@RestController
public class CartController {
    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    @Value("${max.attempts}")
    private int MAX_ATTEMPTS;

    @Value("${payment.url}")
    private String paymentServiceUrl;

    @Value("${order.url}")
    private String orderServiceUrl;

    @PostMapping("/checkout")
    public ResponseEntity<String> checkout(@RequestBody String orderId) {
        long startTime = System.currentTimeMillis();

        createOrder(orderId);

        if (MAX_ATTEMPTS == 1) initiatePayment(orderId);
        else initiatePaymentWithRetry(orderId, MAX_ATTEMPTS);

        long timeTaken = System.currentTimeMillis() - startTime;
        return new ResponseEntity<>("Cart checked out in " + timeTaken + " ms", HttpStatus.CREATED);
    }

    private void initiatePaymentWithRetry(String orderId, int retriesLeft) {
        if (retriesLeft <= 0) {
            System.out.println("Max attempts to reach payment service exhausted");
            return;
        }

        try {
            System.out.println("Calling payment service for " + orderId + ". Attempts left: " + retriesLeft);
            initiatePayment(orderId);
        } catch (RestClientException e) {
            initiatePaymentWithRetry(orderId, --retriesLeft);
        }
    }

    private void createOrder(String orderId) {
        String orderResponse = new RestTemplate().postForObject(orderServiceUrl + "/order", orderId, String.class);
        System.out.println(orderServiceUrl);
        System.out.println("Response from order service: " + orderResponse);
    }

    private void initiatePayment(String orderId) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(100));
        factory.setReadTimeout(Duration.ofMillis(900));
        RestTemplate template = new RestTemplate(factory);
        String paymentResponse = template.postForObject(paymentServiceUrl + "/payment", orderId, String.class);
        System.out.println(paymentServiceUrl);
        System.out.println("Response from payment service: " + paymentResponse);
    }

}
