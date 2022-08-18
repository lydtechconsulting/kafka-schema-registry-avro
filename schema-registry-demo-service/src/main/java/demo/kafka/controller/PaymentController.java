package demo.kafka.controller;

import demo.kafka.rest.api.SendPaymentRequest;
import demo.kafka.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/payments")
public class PaymentController {

    @Autowired
    private final PaymentService paymentService;

    @PostMapping("/send")
    public ResponseEntity<String> sendPayment(@RequestBody SendPaymentRequest request) {
        try {
            paymentService.process(request.getPaymentId(), request);
            return ResponseEntity.ok(request.getPaymentId());
        } catch(Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.internalServerError().body(request.getPaymentId());
        }
    }
}
