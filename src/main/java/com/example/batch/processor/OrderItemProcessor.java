package com.example.batch.processor;

import com.example.batch.domain.Order;
import com.example.batch.dto.OrderInputDto;
import com.example.batch.error.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
@Component
public class OrderItemProcessor implements ItemProcessor<OrderInputDto, Order> {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Order process(OrderInputDto item) throws Exception {
        log.debug("데이터 처리 시작: {}", item.getOrderId());

        // 1. 유효성 검증
        validateItem(item);

        if (!item.isValid()) {
            log.warn("데이터 유효성 검증 실패 - Order ID: {}, 이유: {}", item.getOrderId(), item.getErrorMessage());
            // return null; // 필터링
            throw new ValidationException(item.getErrorMessage());
        }

        // 2. 데이터 변환 및 Order 객체 생성
        Order order = Order.builder()
                .orderId(item.getOrderId())
                .customerName(item.getCustomerName())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .totalAmount(item.getPrice() * item.getQuantity())
                .orderDate(item.getOrderDate())
                .status("PROCESSED")
                .processedAt(LocalDateTime.now())
                .build();

        log.debug("데이터 처리 완료: {}", order.getOrderId());
        return order;
    }

    private void validateItem(OrderInputDto item) {
        // 기본값 설정
        item.setValid(true);

        // 필수 필드 검증
        if (item.getOrderId() == null || item.getOrderId().trim().isEmpty()) {
            markInvalid(item, "Order ID is required");
            return;
        }

        if (item.getCustomerName() == null || item.getCustomerName().trim().isEmpty()) {
            markInvalid(item, "Customer name is required");
            return;
        }

        if (item.getProductName() == null || item.getProductName().trim().isEmpty()) {
            markInvalid(item, "Product name is required");
            return;
        }

        // 수량 검증
        try {
            int quantity = Integer.parseInt(item.getQuantityStr());
            if (quantity <= 0) {
                markInvalid(item, "Quantity must be greater than 0");
                return;
            }
            if (quantity > 1000) {
                markInvalid(item, "Quantity cannot exceed 1000");
                return;
            }
            item.setQuantity(quantity);
        } catch (NumberFormatException e) {
            markInvalid(item, "Invalid quantity format: " + item.getQuantityStr());
            return;
        }

        // 가격 검증
        try {
            double price = Double.parseDouble(item.getPriceStr());
            if (price <= 0) {
                markInvalid(item, "Price must be greater than 0");
                return;
            }
            if (price > 1000000) {
                markInvalid(item, "Price cannot exceed 1,000,000");
                return;
            }
            item.setPrice(price);
        } catch (NumberFormatException e) {
            markInvalid(item, "Invalid price format: " + item.getPriceStr());
            return;
        }

        // 주문일자 검증
        try {
            if (item.getOrderDateStr() == null || item.getOrderDateStr().trim().isEmpty()) {
                markInvalid(item, "Order date is required");
                return;
            }

            LocalDateTime orderDate = LocalDateTime.parse(item.getOrderDateStr(), DATE_FORMATTER);
            if (orderDate.isAfter(LocalDateTime.now())) {
                markInvalid(item, "Order date cannot be in the future");
                return;
            }
            item.setOrderDate(orderDate);
        } catch (DateTimeParseException e) {
            markInvalid(item, "Invalid date format. Expected: yyyy-MM-dd HH:mm:ss, Actual: " + item.getOrderDateStr());
            return;
        }

        item.setValid(true);
    }

    private void markInvalid(OrderInputDto item, String errorMessage) {
        item.setValid(false);
        item.setErrorMessage(errorMessage);
    }

}