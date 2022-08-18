package demo.kafka.rest.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendPaymentRequest {

    private String paymentId;
    private Double amount;
    private String currency;
    private String fromAccount;
    private String toAccount;
}
