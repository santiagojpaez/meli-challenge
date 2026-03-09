package com.challenge.service;

import com.challenge.model.CurrencyCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyExchangeServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersUriSpec uriSpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersSpec headersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Creates a service with a key+URL that require the RestClient for fetching. */
    private CurrencyExchangeService serviceWithKey() {
        return new CurrencyExchangeService("test-key", "https://api.example.com", restClient);
    }

    @SuppressWarnings("unchecked")
    private void stubHttpChain() {
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void getFactorToARS_returnsOne_whenCurrencyIsARS() {
        CurrencyExchangeService service = serviceWithKey();

        BigDecimal result = service.getFactorToARS(CurrencyCode.ARS);

        assertThat(result).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void getFactorToARS_returnsNull_whenApiKeyIsBlank() {
        CurrencyExchangeService blankKeyService =
                new CurrencyExchangeService("", "https://api.example.com", restClient);

        BigDecimal result = blankKeyService.getFactorToARS(CurrencyCode.USD);

        assertThat(result).isNull();
    }

    @Test
    void getFactorToARS_fetchesRateFromApi_onCacheMiss() {
        stubHttpChain();
        when(responseSpec.body(ExchangeRateApiResponse.class))
                .thenReturn(new ExchangeRateApiResponse("success", "USD", "ARS", BigDecimal.valueOf(980)));

        BigDecimal result = serviceWithKey().getFactorToARS(CurrencyCode.USD);

        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(980));
    }

    @Test
    void getFactorToARS_returnsCachedRate_onSecondCall() {
        stubHttpChain();
        when(responseSpec.body(ExchangeRateApiResponse.class))
                .thenReturn(new ExchangeRateApiResponse("success", "USD", "ARS", BigDecimal.valueOf(980)));

        CurrencyExchangeService service = serviceWithKey();
        service.getFactorToARS(CurrencyCode.USD);
        BigDecimal second = service.getFactorToARS(CurrencyCode.USD);

        assertThat(second).isEqualByComparingTo(BigDecimal.valueOf(980));
        verify(responseSpec, times(1)).body(ExchangeRateApiResponse.class);
    }

    @Test
    void getFactorToARS_returnsNull_whenApiResponseIsNull() {
        stubHttpChain();
        when(responseSpec.body(ExchangeRateApiResponse.class)).thenReturn(null);

        BigDecimal result = serviceWithKey().getFactorToARS(CurrencyCode.USD);

        assertThat(result).isNull();
    }

    @Test
    void getFactorToARS_returnsNull_whenApiResponseIsNotSuccess() {
        stubHttpChain();
        when(responseSpec.body(ExchangeRateApiResponse.class))
                .thenReturn(new ExchangeRateApiResponse("error", "USD", "ARS", null));

        BigDecimal result = serviceWithKey().getFactorToARS(CurrencyCode.USD);

        assertThat(result).isNull();
    }

    @Test
    void getFactorToARS_returnsNull_whenApiThrowsException() {
        stubHttpChain();
        when(responseSpec.body(ExchangeRateApiResponse.class))
                .thenThrow(new RuntimeException("network error"));

        BigDecimal result = serviceWithKey().getFactorToARS(CurrencyCode.USD);

        assertThat(result).isNull();
    }

    @Test
    void getFactorToARS_fetchesRatesForDifferentCurrenciesIndependently() {
        stubHttpChain();
        when(responseSpec.body(ExchangeRateApiResponse.class))
                .thenReturn(new ExchangeRateApiResponse("success", "USD", "ARS", BigDecimal.valueOf(980)))
                .thenReturn(new ExchangeRateApiResponse("success", "BRL", "ARS", BigDecimal.valueOf(200)));

        CurrencyExchangeService service = serviceWithKey();
        BigDecimal usd = service.getFactorToARS(CurrencyCode.USD);
        BigDecimal brl = service.getFactorToARS(CurrencyCode.BRL);

        assertThat(usd).isEqualByComparingTo(BigDecimal.valueOf(980));
        assertThat(brl).isEqualByComparingTo(BigDecimal.valueOf(200));
    }
}
