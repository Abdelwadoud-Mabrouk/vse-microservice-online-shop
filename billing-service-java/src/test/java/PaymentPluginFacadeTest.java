

import com.vseonlineshop.payment.dto.PaymentRequest;
import com.vseonlineshop.payment.dto.PaymentProviderResponse;
import com.vseonlineshop.payment.dto.PaymentStatusResponse;
import com.vseonlineshop.payment.dto.enums.PaymentMethod;
import com.vseonlineshop.payment.dto.refund.RefundProviderResponse;
import com.vseonlineshop.payment.entity.PaymentStatus;
import com.vseonlineshop.payment.exception.PaymentServiceException;
import com.vseonlineshop.payment.strategy.config.PaymentRoutingProperties;
import com.vseonlineshop.payment.strategy.facade.PaymentPluginFacade;
import com.vseonlineshop.payment.strategy.PaymentStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentPluginFacadeTest {


    @Mock
    private PaymentStrategy stripeStrategy;

    @Mock
    private PaymentStrategy paypalStrategy;

    @Mock
    private PaymentRoutingProperties routingProperties;

    private PaymentPluginFacade facade;

    private PaymentRequest creditCardRequest;
    private PaymentRequest paypalRequest;
    private UUID samplePaymentId;

    @BeforeEach
    void setUp() {
        facade = new PaymentPluginFacade(
                List.of(stripeStrategy, paypalStrategy),
                routingProperties);

        // Routing-Map aufsetzen
        Map<PaymentMethod, String> routingMap = new EnumMap<>(PaymentMethod.class);
        routingMap.put(PaymentMethod.CREDIT_CARD, "STRIPE");
        routingMap.put(PaymentMethod.APPLE_PAY, "STRIPE");
        routingMap.put(PaymentMethod.GOOGLE_PAY, "STRIPE");
        routingMap.put(PaymentMethod.PAYPAL, "PAYPAL");

        lenient().when(routingProperties.getRouting()).thenReturn(routingMap);

        lenient().when(stripeStrategy.getProviderName()).thenReturn("STRIPE");
        lenient().when(paypalStrategy.getProviderName()).thenReturn("PAYPAL");

        samplePaymentId = UUID.randomUUID();

        creditCardRequest = new PaymentRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("49.99"),
                "EUR",
                UUID.randomUUID(),
                PaymentMethod.CREDIT_CARD,
                List.of()
        );

        paypalRequest = new PaymentRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("49.99"),
                "EUR",
                UUID.randomUUID(),
                PaymentMethod.PAYPAL,
                List.of()
        );
    }

    // ==========================================
    // 1. ROUTING-TESTS DURCH DIE PROPERTIES
    // ==========================================

    @Test
    @DisplayName("CREDIT_CARD wird laut Properties an STRIPE geroutet")
    void charge_CreditCard_RoutesToStripe() {
        when(stripeStrategy.supports(PaymentMethod.CREDIT_CARD)).thenReturn(true);
        PaymentProviderResponse expected = new PaymentProviderResponse("STRIPE_URL", "ch_123", "rawJson");
        when(stripeStrategy.charge(creditCardRequest, samplePaymentId)).thenReturn(expected);

        PaymentProviderResponse result = facade.charge(creditCardRequest, samplePaymentId);

        assertThat(result.approvalUrl()).isEqualTo("STRIPE_URL");
        verify(stripeStrategy).charge(creditCardRequest, samplePaymentId);
        verify(paypalStrategy, never()).charge(any(), any());
    }

    @Test
    @DisplayName("PAYPAL wird laut Properties an PAYPAL geroutet")
    void charge_Paypal_RoutesToPaypal() {
        when(paypalStrategy.supports(PaymentMethod.PAYPAL)).thenReturn(true);
        PaymentProviderResponse expected = new PaymentProviderResponse("PAYPAL_URL", "PAY-123", "rawJson");
        when(paypalStrategy.charge(paypalRequest, samplePaymentId)).thenReturn(expected);

        PaymentProviderResponse result = facade.charge(paypalRequest, samplePaymentId);

        assertThat(result.approvalUrl()).isEqualTo("PAYPAL_URL");
        verify(paypalStrategy).charge(paypalRequest, samplePaymentId);
        verify(stripeStrategy, never()).charge(any(), any());

    }

    // ==========================================
    // 2. FEHLERBEHANDLUNG & TIMEOUTS
    // ==========================================

    @Test
    @DisplayName("Fehler bei Stripe reicht PaymentServiceException weiter")
    void charge_StripeError_ThrowsPaymentServiceException() {
        when(stripeStrategy.supports(PaymentMethod.CREDIT_CARD)).thenReturn(true);
        when(stripeStrategy.charge(creditCardRequest, samplePaymentId))
                .thenThrow(new PaymentServiceException("Ungültige Zahlungsanforderung: Invalid currency"));

        assertThatThrownBy(() -> facade.charge(creditCardRequest, samplePaymentId))
                .isInstanceOf(PaymentServiceException.class)
                .hasMessageContaining("Ungültige Zahlungsanforderung");
    }

    @Test
    @DisplayName("Timeout bei PayPal wird mit Cause gewickelt")
    void charge_PaypalTimeout_ThrowsPaymentServiceExceptionWithCause() {
        when(paypalStrategy.supports(PaymentMethod.PAYPAL)).thenReturn(true);
        SocketTimeoutException timeout = new SocketTimeoutException("Connection timed out");

        when(paypalStrategy.charge(paypalRequest, samplePaymentId))
                .thenThrow(new PaymentServiceException("Netzwerkfehler bei Verbindung zu PayPal", timeout));

        assertThatThrownBy(() -> facade.charge(paypalRequest, samplePaymentId))
                .isInstanceOf(PaymentServiceException.class)
                .hasMessageContaining("Netzwerkfehler")
                .hasCauseInstanceOf(SocketTimeoutException.class);
    }

    @Test
    @DisplayName("Timeout bei Stripe wird mit Cause gewickelt")
    void charge_StripeTimeout_ThrowsPaymentServiceExceptionWithCause() {
        when(stripeStrategy.supports(PaymentMethod.CREDIT_CARD)).thenReturn(true);
        SocketTimeoutException timeout = new SocketTimeoutException("Read timed out");

        when(stripeStrategy.charge(creditCardRequest, samplePaymentId))
                .thenThrow(new PaymentServiceException("Netzwerkfehler bei Verbindung zu Stripe", timeout));

        assertThatThrownBy(() -> facade.charge(creditCardRequest, samplePaymentId))
                .isInstanceOf(PaymentServiceException.class)
                .hasMessageContaining("Netzwerkfehler")
                .hasCauseInstanceOf(SocketTimeoutException.class);
    }

// ==========================================
    // 3. ROUTING- & CONFIG-FEHLERFÄLLE (RESOLVE STRATEGY)
    // ==========================================

    @Test
    @DisplayName("Exception wenn kein Routing für eine Zahlungsmethode konfiguriert ist")
    void charge_UnconfiguredMethod_ThrowsPaymentServiceException() {
        PaymentRequest klarnaRequest = new PaymentRequest(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("10.00"), "EUR",
                UUID.randomUUID(), PaymentMethod.KLARNA, List.of()
        );

        assertThatThrownBy(() -> facade.charge(klarnaRequest, samplePaymentId))
                .isInstanceOf(PaymentServiceException.class)
                .hasMessageContaining("Kein Provider-Routing für Zahlungsmethode 'KLARNA'");
    }

    @Test
    @DisplayName("Exception wenn konfigurierter Provider die Methode laut supports() nicht unterstützt")
    void charge_ProviderDoesNotSupportMethod_ThrowsPaymentServiceException() {
        when(stripeStrategy.supports(PaymentMethod.CREDIT_CARD)).thenReturn(false);

        assertThatThrownBy(() -> facade.charge(creditCardRequest, samplePaymentId))
                .isInstanceOf(PaymentServiceException.class)
                .hasMessageContaining("Konfigurierter Provider 'STRIPE' unterstützt Zahlungsmethode 'CREDIT_CARD' nicht");
    }

    // ==========================================
    // 4. REFUND TESTFALLE
    // ==========================================

    @Test
    @DisplayName("Refund für CREDIT_CARD nutzt Stripe entsprechend der Property-Config")
    void refund_CreditCard_UsesStripe() {
        when(stripeStrategy.supports(PaymentMethod.CREDIT_CARD)).thenReturn(true);
        when(stripeStrategy.refund("TX123", new BigDecimal("10.00"), "EUR"))
                .thenReturn(RefundProviderResponse.builder()
                        .refundId("REFUND-999")
                        .paymentStatus(PaymentStatus.REFUND_SUCCEEDED)
                        .build());

        RefundProviderResponse response = facade.refund("TX123", new BigDecimal("10.00"), "EUR", PaymentMethod.CREDIT_CARD);

        assertThat(response.refundId()).isEqualTo("REFUND-999");
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.REFUND_SUCCEEDED);
        verify(stripeStrategy).refund("TX123", new BigDecimal("10.00"), "EUR");
    }

    @Test
    @DisplayName("Refund reicht PaymentServiceException bei Provider-Fehler weiter")
    void refund_ProviderError_ThrowsPaymentServiceException() {
        when(stripeStrategy.supports(PaymentMethod.CREDIT_CARD)).thenReturn(true);
        when(stripeStrategy.refund("TX123", new BigDecimal("10.00"), "EUR"))
                .thenThrow(new PaymentServiceException("Rückerstattung über Stripe fehlgeschlagen"));

        assertThatThrownBy(() -> facade.refund("TX123", new BigDecimal("10.00"), "EUR", PaymentMethod.CREDIT_CARD))
                .isInstanceOf(PaymentServiceException.class)
                .hasMessageContaining("Rückerstattung über Stripe fehlgeschlagen");
    }

    // ==========================================
    // 5. GET STATUS TESTFÄLLE
    // ==========================================

    @Test
    @DisplayName("getStatus liefert Status vom Provider, der die Transaktions-ID unterstützt")
    void getStatus_Success() {
        when(paypalStrategy.supportsTransactionId("PAYPAL-TX-123")).thenReturn(true);
        when(paypalStrategy.getStatus("PAYPAL-TX-123"))
                .thenReturn(PaymentStatusResponse.builder()
                        .status(PaymentStatus.CONFIRMED)
                        .transactionId("PAYPAL-TX-123")
                        .build());

        PaymentStatusResponse result = facade.getStatus("PAYPAL-TX-123");

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(result.getTransactionId()).isEqualTo("PAYPAL-TX-123");
        verify(paypalStrategy).getStatus("PAYPAL-TX-123");
        verify(stripeStrategy, never()).getStatus(any());
    }

    @Test
    @DisplayName("getStatus wirft Exception, wenn kein Provider die Transaktions-ID kennt")
    void getStatus_UnknownTransactionId_ThrowsPaymentServiceException() {
        when(stripeStrategy.supportsTransactionId("UNKNOWN_TX")).thenReturn(false);
        when(paypalStrategy.supportsTransactionId("UNKNOWN_TX")).thenReturn(false);

        assertThatThrownBy(() -> facade.getStatus("UNKNOWN_TX"))
                .isInstanceOf(PaymentServiceException.class)
                .hasMessageContaining("Kein Provider für Transaktions-ID 'UNKNOWN_TX' gefunden");
    }


}
