package com.example.batch.writer;

import com.example.batch.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class CompositeOrderWriter {

    private final OrderItemWriter orderItemWriter;
    private final ProcessedOrderWriter processedOrderWriter;

    @Bean
    public ItemWriter<Order> compositeWriter() {
        return new CompositeItemWriterBuilder<Order>()
                .delegates(Arrays.asList(orderItemWriter, processedOrderWriter))
                .build();
    }

}