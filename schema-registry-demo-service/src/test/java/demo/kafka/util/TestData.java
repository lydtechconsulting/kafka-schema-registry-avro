package demo.kafka.util;

import demo.kafka.event.PaymentSent;
import demo.kafka.event.SendPayment;
import demo.kafka.rest.api.SendPaymentRequest;

public class TestData {

    public static PaymentSent buildPaymentSent(String paymentId) {
        return PaymentSent.newBuilder()
            .setPaymentId(paymentId)
            .setAmount(2.0)
            .setCurrency("USD")
            .setToAccount("toAcc")
            .setFromAccount("fromAcc")
            .build();
    }

    public static SendPayment buildSendPayment(String paymentId) {
        return SendPayment.newBuilder()
                .setPaymentId(paymentId)
                .setAmount(2.0)
                .setCurrency("USD")
                .setToAccount("toAcc")
                .setFromAccount("fromAcc")
                .build();
    }

    public static SendPaymentRequest buildSendPaymentRequest(String paymentId) {
        return SendPaymentRequest.builder()
                .paymentId(paymentId)
                .amount(10.00)
                .currency("USD")
                .fromAccount("from-acct")
                .toAccount("to-acct")
                .build();
    }
}
