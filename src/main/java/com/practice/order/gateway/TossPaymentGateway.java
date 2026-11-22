package com.practice.order.gateway;

import com.practice.order.domain.Money;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
@Profile("real")
public class TossPaymentGateway implements PaymentGateway {

    private static final String APPROVE_URL = "https://api.tosspayments.com/v1/payments/confirm";
    private static final String CANCEL_URL = "https://api.tosspayments.com/v1/payments/";

    private final RestTemplate restTemplate;
    private final String authorization;

    public TossPaymentGateway(
            RestTemplate restTemplate,
            @Value("${pg.toss.secret-key:}") String secretKey) {
        this.restTemplate = restTemplate;
        String encoded = Base64.getEncoder().encodeToString(
                (secretKey + ":").getBytes(StandardCharsets.UTF_8));
        this.authorization = "Basic " + encoded;
    }

    @Override
    public ApprovalResult approve(String paymentKey, String orderId, Money amount) {
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    APPROVE_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of(
                            "paymentKey", paymentKey,
                            "orderId", orderId,
                            "amount", amount.amount()
                    ), headers()),
                    Map.class
            );

            Map body = response.getBody();
            if (body == null) {
                throw new PaymentGatewayException("PG 응답 본문 없음", true);
            }
            return new ApprovalResult(
                    String.valueOf(body.get("paymentKey")),
                    String.valueOf(body.get("transactionKey")),
                    String.valueOf(body.get("status")),
                    ((Number) body.get("totalAmount")).longValue()
            );
        } catch (ResourceAccessException | HttpServerErrorException exception) {
            throw new PaymentGatewayException("PG 통신 실패", exception, true);
        } catch (HttpClientErrorException exception) {
            throw new PaymentGatewayException("PG 승인 거절", exception, false);
        }
    }

    @Override
    public void cancel(String paymentKey, String cancelReason) {
        try {
            restTemplate.exchange(
                    CANCEL_URL + paymentKey + "/cancel",
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("cancelReason", cancelReason), headers()),
                    Map.class
            );
        } catch (ResourceAccessException | HttpServerErrorException exception) {
            throw new PaymentGatewayException("PG 취소 통신 실패", exception, true);
        } catch (HttpClientErrorException exception) {
            throw new PaymentGatewayException("PG 취소 거절", exception, false);
        }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        return headers;
    }
}
