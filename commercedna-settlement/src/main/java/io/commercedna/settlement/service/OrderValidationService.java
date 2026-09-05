package io.commercedna.settlement.service;

import io.commercedna.catalog.repository.ProductEntity;
import io.commercedna.core.exception.IdempotencyConflictException;
import io.commercedna.core.exception.InventoryExhaustedException;
import io.commercedna.core.exception.ResourceNotFoundException;
import io.commercedna.settlement.entity.OrderEntity;
import io.commercedna.settlement.repository.OrderJpaRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * Order Validation Service.
 * Handles all validation logic for order creation, following Single Responsibility Principle.
 */
@Service
public class OrderValidationService {

    private final OrderJpaRepository orderRepository;

    public OrderValidationService(OrderJpaRepository orderRepository) {
        this.orderRepository = Objects.requireNonNull(orderRepository);
    }

    /**
     * Validates idempotency key and checks for existing orders.
     */
    public Optional<OrderEntity> checkIdempotency(String idempotencyKey) {
        return orderRepository.findByIdempotencyKey(idempotencyKey.trim());
    }

    /**
     * Validates that the existing order matches the new request parameters.
     */
    public void validateIdempotencyMatch(OrderEntity existingOrder, CreateOrderService.OrderRequest request) {
        if (!existingOrder.getSku().equalsIgnoreCase(request.sku().trim())
                || !Objects.equals(existingOrder.getQuantity(), request.quantity())
                || !Objects.equals(existingOrder.getUnitPricePaise(), request.unitPricePaise())) {
            throw new IdempotencyConflictException(request.idempotencyKey());
        }
    }

    /**
     * Validates inventory availability.
     */
    public void validateInventory(ProductEntity product, int requestedQuantity) {
        if (product.getStockQuantity() < requestedQuantity) {
            throw new InventoryExhaustedException(product.getSku(), requestedQuantity, product.getStockQuantity());
        }
    }
}