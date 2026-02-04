package com.example.batch.dto;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderInputDto {
    @CsvBindByName(column = "order_id")
    private String orderId;

    @CsvBindByName(column = "customer_name")
    private String customerName;

    @CsvBindByName(column = "product_name")
    private String productName;

    @CsvBindByName(column = "quantity")
    private String quantityStr;

    @CsvBindByName(column = "price")
    private String priceStr;

    @CsvBindByName(column = "order_date")
    private String orderDateStr;

    private Integer quantity;
    private Double price;
    private LocalDateTime orderDate;
    private boolean isValid = true;
    private String errorMessage;
}