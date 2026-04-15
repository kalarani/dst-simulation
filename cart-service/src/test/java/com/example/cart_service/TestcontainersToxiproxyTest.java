package com.example.cart_service;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestcontainersToxiproxyTest {
    static Network network;
    static ToxiproxyContainer toxiproxy;
    static GenericContainer<?> payment;
    static GenericContainer<?> order;
    static GenericContainer<?> cart;
    static OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(8))
            .build();

    static ToxiproxyClient toxClient;
    static Proxy paymentProxy;
    static Proxy orderProxy;
    private static String cartUrl;

    @BeforeAll
    static void setup() throws Exception {
        network = Network.newNetwork();

        setupToxiProxy();
        setupPaymentService();
        setupOrderService();
        setupCartService();
    }

    private static void setupCartService() {
        cart = new GenericContainer<>("example/cart-service:0.0.1")
                .withExposedPorts(8080)
                .withNetwork(network)
                .withEnv("MAX_ATTEMPTS", "3")
                .withEnv("PAYMENT_URL", "http://toxiproxy:8666")
                .withEnv("ORDER_URL", "http://toxiproxy:8665")
                .withNetworkAliases("cart");
        cart.start();
        cartUrl = "http://%s:%d/checkout".formatted(cart.getHost(), cart.getMappedPort(8080));
    }

    private static void setupOrderService() throws IOException {
        order = new GenericContainer<>("example/order-service:0.0.1")
                .withExposedPorts(8081)
                .withNetwork(network)
                .withNetworkAliases("order");
        order.start();
        orderProxy = toxClient.createProxy("order_proxy", "0.0.0.0:8665", "order:8081");
    }

    private static void setupPaymentService() throws IOException {
        payment = new GenericContainer<>("example/payment-service:0.0.1")
                .withExposedPorts(8082)
                .withNetwork(network)
                .withEnv("BE_IDEMPOTENT", "true")
                .withNetworkAliases("payment");
        payment.start();
        paymentProxy = toxClient.createProxy("payment_proxy", "0.0.0.0:8666", "payment:8082");
    }

    private static void setupToxiProxy() {
        toxiproxy = new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.5.0")
                .withNetwork(network)
                .withNetworkAliases("toxiproxy");
        toxiproxy.start();
        String toxHost = toxiproxy.getHost();
        toxClient = new ToxiproxyClient(toxHost, toxiproxy.getControlPort());
    }

    @AfterAll
    static void teardown() {
        if (order != null) order.stop();
        if (payment != null) payment.stop();
        if (cart != null) cart.stop();
        if (toxiproxy != null) toxiproxy.stop();
        if (network != null) network.close();
    }

    @Test
    void duplicatePaymentScenario() throws IOException {
        List<Integer> failedSimulations = new ArrayList<>();
        List<Integer> erroredSimulations = new ArrayList<>();

        for (int seed = 0; seed < 10; seed++) {
            runSimulation(seed, erroredSimulations, failedSimulations);
        }

        printResults(erroredSimulations, failedSimulations);
    }

    private void printResults(List<Integer> erroredSimulations, List<Integer> failedSimulations) {
        if (erroredSimulations.isEmpty()) {
            System.out.println("Simulation completed without errors.");
        } else {
            System.out.printf("Few Simulations errored out: %s%n", String.join(", ", erroredSimulations.stream().map(Object::toString).toList()));
        }

        if (failedSimulations.isEmpty()) {
            System.out.println("No failed simulations.");
        } else {
            System.out.println("Few Simulations Failed.");
            System.out.printf("Duplicate payments charged in: %s%n", String.join(", ", failedSimulations.stream().map(Object::toString).toList()));
        }
    }

    private static void runSimulation(int i, List<Integer> erroredSimulations, List<Integer> failedSimulations) throws IOException {
        Random random = new Random(i);

        // Setup downstream latency
        int paymentLatency = random.nextInt(1000);
        paymentProxy.toxics().latency("payment_latency_down", ToxicDirection.DOWNSTREAM, paymentLatency);

        int orderLatency = random.nextInt(1000);
        orderProxy.toxics().latency("order_latency_down", ToxicDirection.DOWNSTREAM, orderLatency);

        // Call the endpoint under test
        Request req = new Request.Builder()
                .url(cartUrl)
                .post(RequestBody.create(("order-"+ i).getBytes()))
                .build();
        Response r = http.newCall(req).execute();
        if (r.code() != 201) {
            erroredSimulations.add(i); // Capture errors
        }
        System.out.printf("Simulation %d: %s%n", i, r.body().string());

        // Assert orders and payments
        Integer noOfOrdersCreated = getOrderCount();
        Integer noOfPaymentsInitiated = getPaymentCount();
        if (noOfOrdersCreated < noOfPaymentsInitiated) {
            failedSimulations.add(i); // Duplicate payment charged
        }

        // Clear toxics
        paymentProxy.toxics().getAll().forEach(t -> { try { t.remove(); } catch(Exception ignored){} });
        orderProxy.toxics().getAll().forEach(t -> { try { t.remove(); } catch(Exception ignored){} });
    }

    @NotNull
    private static Integer getPaymentCount() throws IOException {
        Request paymentCountRequest = new Request.Builder()
                .url("http://localhost:" + payment.getMappedPort(8082) + "/payment-count")
                .get()
                .build();
        Integer paymentCountResponse = Integer.parseInt(http.newCall(paymentCountRequest).execute().body().string());
        return paymentCountResponse;
    }

    @NotNull
    private static Integer getOrderCount() throws IOException {
        Request orderCountRequest = new Request.Builder()
                .url("http://localhost:" + order.getMappedPort(8081) + "/order-count")
                .get()
                .build();
        Integer orderCountResponse = Integer.parseInt(http.newCall(orderCountRequest).execute().body().string());
        return orderCountResponse;
    }
}
