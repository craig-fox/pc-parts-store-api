package nz.fox.craig.order.service;

import lombok.RequiredArgsConstructor;
import nz.fox.craig.order.model.Order;
import nz.fox.craig.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderPersistenceService {

    private final OrderRepository orderRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order save(Order order) {
        return orderRepository.saveAndFlush(order);
    }
}
