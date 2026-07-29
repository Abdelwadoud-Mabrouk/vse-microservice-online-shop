
package com.vseonlineshop.shopservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.vseonlineshop.shopservice.domain.Order;
import com.vseonlineshop.shopservice.domain.OrderStatus;
import com.vseonlineshop.shopservice.dto.order.request.CustomerInformationRequest;
import com.vseonlineshop.shopservice.dto.order.request.ItemRequest;
import com.vseonlineshop.shopservice.dto.order.request.OrderRequest;
import com.vseonlineshop.shopservice.dto.order.request.ShippingAddressRequest;
import com.vseonlineshop.shopservice.dto.payment.PaymentMethod;
import com.vseonlineshop.shopservice.repository.OrderRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest(
        classes = {
                ShopServiceApplication.class,
        },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/sql/seed-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ShopOrderIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMockExtension = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .configureStaticDsl(true)
            .build();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("services.warehouse.url", () -> "http://localhost:" + wireMockExtension.getPort());
        registry.add("services.payment.url", () -> "http://localhost:" + wireMockExtension.getPort() + "/api/v1/billing");
    }


    @Autowired
    private MockMvc mockMvc;


    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private OrderRepository orderRepository;


    private OrderRequest sampleRequest;


    private static final UUID PRODUCT_ID =
            UUID.fromString("11111111-1111-4111-8111-111111111111");

    @BeforeEach
    void setup() {


        orderRepository.deleteAll();

        sampleRequest =
                new OrderRequest(
                        new CustomerInformationRequest(
                                "Max",
                                "Mustermann",
                                "max@test.de",
                                ""
                        ),
                        new ShippingAddressRequest(
                                "Berlin",
                                "Hauptstraße 1",
                                "10115",
                                "DE",
                                "test"
                        ),
                        List.of(
                                new ItemRequest(
                                        PRODUCT_ID,
                                        1,
                                        "SKU-CHAIR-OAK"
                                )
                        ),
                        "EURO",
                        PaymentMethod.CREDIT_CARD
                );

        reset();
    }


    @Test
    @DisplayName("""
            HAPPY PATH:
            Order erstellen -> Warehouse reserviert -> Payment erfolgreich -> AWAITING_PAYMENT
            """)
    void createOrder_success() throws Exception {

        UUID reservationId = UUID.randomUUID();

        // 1. WireMock-Mocks aufsetzen
        mockWarehouseSuccess(reservationId);
        mockPaymentSuccess();

        // 2. Request ausführen & CheckoutResponse im HTTP-Body prüfen
        mockMvc.perform(
                        post("/api/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(sampleRequest))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.status").value("AWAITING_PAYMENT"))
                .andExpect(jsonPath("$.totalAmount").value(296.31))
                .andExpect(jsonPath("$.currency").value("EURO"))
                .andExpect(jsonPath("$.approvalUrl").value("https://payment.test/123"));

        // 3. Datenbank-Prüfungen
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);

        Order order = orders.get(0);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
        assertThat(order.getReservationId()).isEqualTo(reservationId);

        // 4. External Calls verifizieren
        verify(1, postRequestedFor(urlEqualTo("/reservations")));
        verify(1, postRequestedFor(urlEqualTo("/api/v1/billing/pay")));
    }

    @Test
    @DisplayName("""
            FEHLERSZENARIO 1:
            Warehouse hat keinen Bestand (409 Conflict) -> Order wird OUT_OF_STOCK -> Status 409 Conflict
            """)
    void createOrder_warehouseOutOfStock_shouldFailWithConflict() throws Exception {
        // 1. Mocking: Warehouse antwortet mit 409 Conflict (Client Error)
        stubFor(
                WireMock.post(urlEqualTo("/reservations"))
                        .willReturn(aResponse().withStatus(409))
        );

        // 2. Request ausführen & HTTP Status 409 erwarten
        mockMvc.perform(
                        post("/api/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(sampleRequest))
                )
                .andExpect(status().isConflict());

        // 3. DB Prüfen: Order existiert mit Status OUT_OF_STOCK
        List<Order> orders = orderRepository.findAll();
        System.out.println(orders.stream().findFirst().get().getStatus());
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getStatus()).isEqualTo(OrderStatus.OUT_OF_STOCK);

        // 4. Verifizieren: Warehouse wurde aufgerufen, Billing wurde NIEMALS aufgerufen
        verify(1, postRequestedFor(urlEqualTo("/reservations")));
        verify(0, postRequestedFor(urlEqualTo("/api/v1/billing/pay")));
    }

    @Test
    @DisplayName("""
            FEHLERSZENARIO 2:
            Payment-Service nicht erreichbar (500) -> Warehouse Rollback (DELETE) -> Status PAYMENT_FAILED -> Status 503
            """)
    void createOrder_paymentUnavailable_shouldRollbackWarehouseAndReturn503() throws Exception {
        UUID reservationId = UUID.randomUUID();

        // 1. Mocking: Warehouse reserviert erfolgreich
        mockWarehouseSuccess(reservationId);

        // Mocking: Warehouse DELETE für den Rollback erlauben
        stubFor(
                WireMock.delete(urlEqualTo("/reservations/" + reservationId))
                        .willReturn(aResponse().withStatus(201))
        );

        // Mocking: Payment gibt 500 Internal Server Error zurück
        stubFor(
                WireMock.post(urlEqualTo("/api/v1/billing/pay"))
                        .willReturn(aResponse().withStatus(503))
        );

        // 2. Request ausführen & HTTP Status 503 Service Unavailable erwarten
        mockMvc.perform(
                        post("/api/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(sampleRequest))
                )
                .andExpect(status().isServiceUnavailable());

        // 3. DB Prüfen: Order hat Status PAYMENT_FAILED
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);

        // 4. Verifizieren: Warehouse reserviert UND danach per DELETE storniert (Rollback)!
        verify(1, postRequestedFor(urlEqualTo("/reservations")));
        verify(4, postRequestedFor(urlEqualTo("/api/v1/billing/pay")));
        verify(1, deleteRequestedFor(urlEqualTo("/reservations/" + reservationId)));
    }

    @Test
    @DisplayName("""
            FEHLERSZENARIO 3:
            Warehouse liefert leere Reservierung -> Order wird CANCELLED -> Status 400 Bad Request
            """)
    void createOrder_emptyWarehouseReservation_shouldFailWithBadRequest() throws Exception {
        // 1. Mocking: Warehouse schickt leeres items-Array
        stubFor(
                WireMock.post(urlEqualTo("/reservations"))
                        .willReturn(
                                WireMock.okJson("""
                                        {
                                          "reservationId": "%s",
                                          "items": []
                                        }
                                        """.formatted(UUID.randomUUID()))
                        )
        );

        // 2. Request ausführen & HTTP Status 400 erwartet
        mockMvc.perform(
                        post("/api/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(sampleRequest))
                )
                .andExpect(status().isBadRequest());

        // 3. DB Prüfen: Order wurde auf CANCELLED gesetzt
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // 4. Billing wurde nicht aufgerufen
        verify(0, postRequestedFor(urlEqualTo("/api/v1/billing/pay")));
    }

    @Test
    @DisplayName("""
            FEHLERSZENARIO:
            Warehouse-Service nicht erreichbar / 503 Server Error
            -> Kein Billing-Aufruf -> Kein Warehouse-Rollback -> Status 503 Service Unavailable
            """)
    void createOrder_warehouseServerError_shouldReturn503AndNotCallBilling() throws Exception {

        // 1. Mocking: Warehouse antwortet mit 503 Service Unavailable

        stubFor(
                WireMock.post(WireMock.urlEqualTo("/reservations"))
                        .willReturn(WireMock.aResponse().withStatus(503))
        );

        // 2. Request ausführen & 503 Service Unavailable vom Shop-Service erwarten
        mockMvc.perform(
                        post("/api/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(sampleRequest))
                )
                .andExpect(status().isServiceUnavailable());

        // 3. DB Prüfen: Order wurde abgespeichert & storniert / als fehlerhaft markiert
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);

        Order order = orders.get(0);
        // Passe den erwarteten Status an deine Business-Logik an (z. B. CANCELLED oder FAILED)
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // 4. WireMock-Verifizierungen:

        verify(3, postRequestedFor(urlEqualTo("/reservations")));
        verify(0, postRequestedFor(urlEqualTo("/api/v1/billing/pay")));
        verify(0, deleteRequestedFor(urlPathMatching("/reservations/.*")));
    }

    private void mockWarehouseSuccess(UUID reservationId) {

        stubFor(
                WireMock.post(
                                WireMock.urlEqualTo("/reservations")
                        )
                        .willReturn(
                                WireMock.okJson("""
                                        {
                                          "reservationId": "%s",
                                          "items": [
                                            {
                                              "productId": "%s",
                                              "quantity": 1
                                            }
                                          ]
                                        }
                                        """.formatted(reservationId, PRODUCT_ID))
                        )
        );
    }


    private void mockPaymentSuccess() {

        WireMock.stubFor(
                WireMock.post(
                                WireMock.urlEqualTo("/api/v1/billing/pay")
                        )
                        .willReturn(
                                WireMock.okJson("""
                                        {
                                          "orderId": "4dba207f-2fb7-4187-9c12-8eea8d326d99",
                                          "paymentId": "07008f59-4682-47fa-af2b-5f508e2441ac",
                                          "approvalUrl": "https://payment.test/123",
                                          "status": "PENDING",
                                          "paidAt": null
                                        }
                                        """)
                        )
        );
    }
}
