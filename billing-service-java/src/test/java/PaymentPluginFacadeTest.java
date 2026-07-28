
import com.vseonlineshop.payment.dto.PaymentRequest;
import com.vseonlineshop.payment.dto.PaymentProviderResponse;
import com.vseonlineshop.payment.dto.enums.PaymentMethod;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    @DisplayName("AUTO-Modus verwendet passenden Provider")
    void charge_AutoModeUsesMatchingProvider() {

        when(stripeStrategy.supports(PaymentMethod.CREDIT_CARD)).thenReturn(true);

        PaymentProviderResponse expected =
                new PaymentProviderResponse("url", "TX-123", "raw");

        when(stripeStrategy.charge(sampleRequest, samplePaymentId))
                .thenReturn(expected);

        PaymentProviderResponse result =
                facade.charge(sampleRequest, samplePaymentId);

        assertThat(result.providerTransactionId()).isEqualTo("TX-123");

        verify(stripeStrategy).charge(sampleRequest, samplePaymentId);
        verify(paypalStrategy, never()).charge(any(), any());
    }

    @Test
    @DisplayName("Verwendet konfigurierten Provider")
    void charge_UsesConfiguredProvider() {

        ReflectionTestUtils.setField(facade, "activeProvider", "PAYPAL");

        when(paypalStrategy.supports(PaymentMethod.CREDIT_CARD)).thenReturn(true);

        PaymentProviderResponse expected =
                new PaymentProviderResponse("url", "PAYPAL-123", "raw");

        when(paypalStrategy.charge(sampleRequest, samplePaymentId))
                .thenReturn(expected);

        PaymentProviderResponse result =
                facade.charge(sampleRequest, samplePaymentId);

        assertThat(result.providerTransactionId()).isEqualTo("PAYPAL-123");

        verify(paypalStrategy).charge(sampleRequest, samplePaymentId);
        verify(stripeStrategy, never()).charge(any(), any());
    }

    @Test
    @DisplayName("Fallback auf anderen Provider wenn konfigurierter Provider nicht unterstützt")
    void charge_FallbackToMatchingProvider() {

        ReflectionTestUtils.setField(facade, "activeProvider", "PAYPAL");

        when(paypalStrategy.supports(PaymentMethod.CREDIT_CARD)).thenReturn(false);
        when(stripeStrategy.supports(PaymentMethod.CREDIT_CARD)).thenReturn(true);

        PaymentProviderResponse expected =
                new PaymentProviderResponse("url", "STRIPE-999", "raw");

        when(stripeStrategy.charge(sampleRequest, samplePaymentId))
                .thenReturn(expected);

        PaymentProviderResponse result =
                facade.charge(sampleRequest, samplePaymentId);

        assertThat(result.providerTransactionId()).isEqualTo("STRIPE-999");

        verify(stripeStrategy).charge(sampleRequest, samplePaymentId);
        verify(paypalStrategy, never()).charge(any(), any());
    }

    @Test
    @DisplayName("Exception wenn kein Provider die Zahlungsmethode unterstützt")
    void charge_NoProviderFound() {

        when(stripeStrategy.supports(PaymentMethod.CREDIT_CARD)).thenReturn(false);
        when(paypalStrategy.supports(PaymentMethod.CREDIT_CARD)).thenReturn(false);

        assertThatThrownBy(() ->
                facade.charge(sampleRequest, samplePaymentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kein Provider");
    }

    @Test
    @DisplayName("Refund verwendet passenden Provider")
    void refund_UsesMatchingProvider() {

        when(stripeStrategy.supports(PaymentMethod.CREDIT_CARD)).thenReturn(true);

        when(stripeStrategy.refund(
                "TX123",
                new BigDecimal("10.00"),
                "EUR"))
                .thenReturn("REFUND_OK");

        String result = facade.refund(
                "TX123",
                new BigDecimal("10.00"),
                "EUR",
                PaymentMethod.CREDIT_CARD);

        assertThat(result).isEqualTo("REFUND_OK");

        verify(stripeStrategy)
                .refund("TX123", new BigDecimal("10.00"), "EUR");
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