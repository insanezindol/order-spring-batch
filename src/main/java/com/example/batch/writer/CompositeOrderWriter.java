package com.example.batch.writer;

import com.example.batch.domain.Order;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class CompositeOrderWriter {

    @Autowired
    private OrderItemWriter orderItemWriter;

    @Autowired
    private ProcessedOrderWriter processedOrderWriter;

    @Bean
    public ItemWriter<Order> compositeWriter() {
        return new CompositeItemWriterBuilder<Order>()
                .delegates(Arrays.asList(orderItemWriter, processedOrderWriter))
                .build();
    }

}