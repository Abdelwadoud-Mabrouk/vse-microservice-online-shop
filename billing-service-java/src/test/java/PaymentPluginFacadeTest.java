
import com.vseonlineshop.payment.dto.PaymentRequest;
import com.vseonlineshop.payment.dto.PaymentProviderResponse;
import com.vseonlineshop.payment.dto.enums.PaymentMethod;
import com.vseonlineshop.payment.exception.PaymentServiceException;
import com.vseonlineshop.payment.facade.PaymentPluginFacade;
import com.vseonlineshop.payment.strategy.PaymentStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentPluginFacadeTest {

    @Mock
    private PaymentStrategy stripeStrategy;

    @Mock
    private PaymentStrategy paypalStrategy;

    private PaymentPluginFacade facade;

    private PaymentRequest sampleRequest;
    private UUID samplePaymentId;

    @BeforeEach
    void setUp() {
        facade = new PaymentPluginFacade(List.of(stripeStrategy, paypalStrategy));
        ReflectionTestUtils.setField(facade, "activeProvider", "AUTO");

        lenient().when(stripeStrategy.getProviderName()).thenReturn("STRIPE");
        lenient().when(paypalStrategy.getProviderName()).thenReturn("PAYPAL");

        sampleRequest = new PaymentRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("49.99"),
                "EUR",
                UUID.randomUUID(),
                PaymentMethod.CREDIT_CARD,
                List.of()
        );

        samplePaymentId = UUID.randomUUID();
    }

    // ==========================================
    // 1. ERFOLG & PROVIDER-WECHSEL
    // ==========================================

    @Test
    @DisplayName("AUTO-Modus verwendet passenden Provider")
    void charge_AutoModeUsesMatchingProvider() {
        when(stripeStrategy.supports(PaymentMethod.CREDIT_CARD)).thenReturn(true);
        PaymentProviderResponse expected = new PaymentProviderResponse("url", "TX-123", "raw");
        when(stripeStrategy.charge(sampleRequest, samplePaymentId)).thenReturn(expected);

        PaymentProviderResponse result = facade.charge(sampleRequest, samplePaymentId);

        assertThat(result.approvalUrl()).isEqualTo("url");
        assertThat(result.providerTransactionId()).isEqualTo("TX-123");
        verify(stripeStrategy).charge(sampleRequest, samplePaymentId);
    }

    @Test
    @DisplayName("Verwendet konfigurierten Provider (Anbieterwechsel per Konfiguration)")
    void charge_UsesConfiguredProvider() {
        ReflectionTestUtils.setField(facade, "activeProvider", "PAYPAL");
        when(paypalStrategy.supports(PaymentMethod.CREDIT_CARD)).thenReturn(true);
        PaymentProviderResponse expected = new PaymentProviderResponse("PAYPAL_URL", "PAYPAL-123", "raw");
        when(paypalStrategy.charge(sampleRequest, samplePaymentId)).thenReturn(expected);

        PaymentProviderResponse result = facade.charge(sampleRequest, samplePaymentId);

        assertThat(result.approvalUrl()).isEqualTo("PAYPAL_URL");
        verify(paypalStrategy).charge(sampleRequest, samplePaymentId);
    }

    @Test
    @DisplayName("Fallback auf anderen Provider wenn konfigurierter Provider nicht unterstützt")
    void charge_FallbackToMatchingProvider() {
        ReflectionTestUtils.setField(facade, "activeProvider", "PAYPAL");
        when(paypalStrategy.supports(PaymentMethod.CREDIT_CARD)).thenReturn(false);
        when(stripeStrategy.supports(PaymentMethod.CREDIT_CARD)).thenReturn(true);
        PaymentProviderResponse expected = new PaymentProviderResponse("url", "STRIPE-999", "raw");
        when(stripeStrategy.charge(sampleRequest, samplePaymentId)).thenReturn(expected);

        PaymentProviderResponse result = facade.charge(sampleRequest, samplePaymentId);

        assertThat(result.providerTransactionId()).isEqualTo("STRIPE-999");
        verify(stripeStrategy).charge(sampleRequest, samplePaymentId);
        verify(paypalStrategy, never()).charge(any(), any());
    }

    // ==========================================
    // 2. FEHLERBEHANDLUNG / ABLEHNUNGEN
    // ==========================================

    @Test
    @DisplayName("Fehler durch Stripe (z.B. Ungültige Parameter)")
    void charge_StripeError_ThrowsPaymentServiceException() {
        ReflectionTestUtils.setField(facade, "activeProvider", "STRIPE");
        when(stripeStrategy.supports(PaymentMethod.CREDIT_CARD)).thenReturn(true);

        when(stripeStrategy.charge(sampleRequest, samplePaymentId))
                .thenThrow(new PaymentServiceException("Ungültige Zahlungsanforderung: Invalid currency"));

        assertThatThrownBy(() -> facade.charge(sampleRequest, samplePaymentId))
                .isInstanceOf(PaymentServiceException.class)
                .hasMessageContaining("Ungültige Zahlungsanforderung");
    }

    @Test
    @DisplayName("Fehler durch PayPal (z.B. API-Fehler)")
    void charge_PaypalError_ThrowsPaymentServiceException() {
        ReflectionTestUtils.setField(facade, "activeProvider", "PAYPAL");
        when(paypalStrategy.supports(PaymentMethod.CREDIT_CARD)).thenReturn(true);

        when(paypalStrategy.charge(sampleRequest, samplePaymentId))
                .thenThrow(new PaymentServiceException("Zahlung über PayPal fehlgeschlagen"));

        assertThatThrownBy(() -> facade.charge(sampleRequest, samplePaymentId))
                .isInstanceOf(PaymentServiceException.class)
                .hasMessageContaining("PayPal fehlgeschlagen");
    }

    // ==========================================
    // 3. TIMEOUTS & URSACEN (Exception-Chaining mit Cause)
    // ==========================================

    @Test
    @DisplayName("Timeout bei Stripe Gateway wird mit Cause gewickelt")
    void charge_StripeTimeout_ThrowsPaymentServiceExceptionWithCause() {
        ReflectionTestUtils.setField(facade, "activeProvider", "STRIPE");
        when(stripeStrategy.supports(PaymentMethod.CREDIT_CARD)).thenReturn(true);

        SocketTimeoutException timeout = new SocketTimeoutException("Read timed out");
        when(stripeStrategy.charge(sampleRequest, samplePaymentId))
                .thenThrow(new PaymentServiceException("Netzwerkfehler bei Verbindung zu Stripe", timeout));

        assertThatThrownBy(() -> facade.charge(sampleRequest, samplePaymentId))
                .isInstanceOf(PaymentServiceException.class)
                .hasMessageContaining("Netzwerkfehler")
                .hasCauseInstanceOf(SocketTimeoutException.class);
    }

    @Test
    @DisplayName("Timeout bei PayPal Gateway wird mit Cause gewickelt")
    void charge_PaypalTimeout_ThrowsPaymentServiceExceptionWithCause() {
        ReflectionTestUtils.setField(facade, "activeProvider", "PAYPAL");
        when(paypalStrategy.supports(PaymentMethod.CREDIT_CARD)).thenReturn(true);

        SocketTimeoutException timeout = new SocketTimeoutException("Connection timed out");
        when(paypalStrategy.charge(sampleRequest, samplePaymentId))
                .thenThrow(new PaymentServiceException("Netzwerkfehler bei Verbindung zu PayPal", timeout));

        assertThatThrownBy(() -> facade.charge(sampleRequest, samplePaymentId))
                .isInstanceOf(PaymentServiceException.class)
                .hasMessageContaining("Netzwerkfehler")
                .hasCauseInstanceOf(SocketTimeoutException.class);
    }

    // ==========================================
    // 4. WEITERE ECKFÄLLE (Refund & Status)
    // ==========================================

    @Test
    @DisplayName("Exception wenn kein Provider die Zahlungsmethode unterstützt")
    void charge_NoProviderFound() {
        when(stripeStrategy.supports(PaymentMethod.CREDIT_CARD)).thenReturn(false);
        when(paypalStrategy.supports(PaymentMethod.CREDIT_CARD)).thenReturn(false);

        assertThatThrownBy(() -> facade.charge(sampleRequest, samplePaymentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kein Provider");
    }

    @Test
    @DisplayName("Refund verwendet passenden Provider")
    void refund_UsesMatchingProvider() {
        when(stripeStrategy.supports(PaymentMethod.CREDIT_CARD)).thenReturn(true);
        when(stripeStrategy.refund("TX123", new BigDecimal("10.00"), "EUR"))
                .thenReturn("REFUND-999");

        String result = facade.refund("TX123", new BigDecimal("10.00"), "EUR", PaymentMethod.CREDIT_CARD);

        assertThat(result).isEqualTo("REFUND-999");
        verify(stripeStrategy).refund("TX123", new BigDecimal("10.00"), "EUR");
    }

    @Test
    @DisplayName("Refund wirft PaymentServiceException bei Fehler")
    void refund_Error_ThrowsPaymentServiceException() {
        when(stripeStrategy.supports(PaymentMethod.CREDIT_CARD)).thenReturn(true);
        when(stripeStrategy.refund("TX123", new BigDecimal("10.00"), "EUR"))
                .thenThrow(new PaymentServiceException("Rückerstattung über Stripe fehlgeschlagen"));

        assertThatThrownBy(() -> facade.refund("TX123", new BigDecimal("10.00"), "EUR", PaymentMethod.CREDIT_CARD))
                .isInstanceOf(PaymentServiceException.class)
                .hasMessageContaining("Rückerstattung über Stripe fehlgeschlagen");
    }

    @Test
    @DisplayName("Status wird über passenden Provider abgefragt")
    void getStatus_UsesOwningProvider() {
        when(paypalStrategy.supportsTransactionId("TX123")).thenReturn(true);
        when(paypalStrategy.getStatus("TX123")).thenReturn("COMPLETED");

        String result = facade.getStatus("TX123");

        assertThat(result).isEqualTo("COMPLETED");
        verify(paypalStrategy).getStatus("TX123");
        verify(stripeStrategy, never()).getStatus(any());
    }

    @Test
    @DisplayName("Status liefert UNKNOWN wenn Provider Exception abfängt")
    void getStatus_ProviderError_ReturnsUnknown() {
        when(paypalStrategy.supportsTransactionId("TX123")).thenReturn(true);
        when(paypalStrategy.getStatus("TX123")).thenReturn("UNKNOWN");

        String result = facade.getStatus("TX123");

        assertThat(result).isEqualTo("UNKNOWN");
        verify(paypalStrategy).getStatus("TX123");
    }

    @Test
    @DisplayName("Status verwendet konfigurierten Provider als Fallback")
    void getStatus_FallbackToConfiguredProvider() {
        ReflectionTestUtils.setField(facade, "activeProvider", "STRIPE");

        when(stripeStrategy.supportsTransactionId("TX123")).thenReturn(false);
        when(paypalStrategy.supportsTransactionId("TX123")).thenReturn(false);
        when(stripeStrategy.getStatus("TX123")).thenReturn("UNKNOWN");

        String result = facade.getStatus("TX123");

        assertThat(result).isEqualTo("UNKNOWN");
        verify(stripeStrategy).getStatus("TX123");
    }

}