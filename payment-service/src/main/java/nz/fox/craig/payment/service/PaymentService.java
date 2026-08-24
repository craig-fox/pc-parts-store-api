package nz.fox.craig.payment.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import nz.fox.craig.payment.dto.CreatePaymentRequest;
import nz.fox.craig.payment.dto.PaymentResponse;
import nz.fox.craig.payment.model.Payment;
import nz.fox.craig.payment.model.PaymentStatus;
import nz.fox.craig.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
    
        return paymentRepository.findByOrderId(request.orderId())
                .map(this::toResponse)
                .orElseGet(() -> createNewPayment(request));
    }
    
    private PaymentResponse createNewPayment(CreatePaymentRequest request) {
    
        Payment payment = new Payment();
    
        payment.setOrderId(request.orderId());
        payment.setCustomerId(request.customerId());
        payment.setAmount(request.amount());
        payment.setCurrency(request.currency());
        payment.setStatus(PaymentStatus.COMPLETED);
    
        Payment savedPayment = paymentRepository.save(payment);
    
        return toResponse(savedPayment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByCustomer(UUID customerId) {
        return paymentRepository.findByCustomerId(customerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }
}
