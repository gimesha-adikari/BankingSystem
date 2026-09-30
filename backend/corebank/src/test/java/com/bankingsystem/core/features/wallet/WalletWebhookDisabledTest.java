package com.bankingsystem.core.features.wallet;

import com.bankingsystem.core.features.wallet.domain.repository.PaymentIntentRepository;
import com.bankingsystem.core.features.wallet.interfaces.WalletWebhookController;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class WalletWebhookDisabledTest {

    PaymentIntentRepository intentRepo = mock(PaymentIntentRepository.class);
    WalletWebhookController controller = new WalletWebhookController(intentRepo);

    @Test
    void webhookEndpointIsDisabledAndReturns410() {
        var response = controller.handle("any-intent-id", "SUCCESS");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
        verify(intentRepo, never()).findById(any());
        verify(intentRepo, never()).save(any());
    }

    @Test
    void webhookDoesNotPersistStatusChange() {
        controller.handle("intent-xyz", "SUCCESS");
        verifyNoInteractions(intentRepo);
    }
}
