package com.example.batch.writer;

import com.example.batch.domain.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@Component
public class ProcessedOrderWriter implements ItemWriter<Order> {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ProcessedOrderWriter(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void write(Chunk<? extends Order> chunk) throws Exception {
        List<? extends Order> orders = chunk.getItems();

        String sql = """
                INSERT INTO processed_orders (original_order_id, customer_name, product_name, quantity, price, total_amount, order_date, status, processed_at, processing_result) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Order order = orders.get(i);
                ps.setString(1, order.getOrderId());
                ps.setString(2, order.getCustomerName());
                ps.setString(3, order.getProductName());
                ps.setInt(4, order.getQuantity());
                ps.setDouble(5, order.getPrice());
                ps.setDouble(6, order.getTotalAmount());
                ps.setObject(7, order.getOrderDate());
                ps.setString(8, order.getStatus());
                ps.setObject(9, order.getProcessedAt());
                ps.setString(10, "SUCCESS");
            }

            @Override
            public int getBatchSize() {
                return orders.size();
            }
        });

        log.info("ProcessedOrders 테이블에 " + orders.size() + "개 레코드 저장 완료");
    }

}