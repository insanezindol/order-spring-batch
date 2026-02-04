package com.example.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
public class JobCompletionNotificationListener implements JobExecutionListener {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("### Job 시작: {}", jobExecution.getJobInstance().getJobName());
        log.info("### 시작 시간: {}", jobExecution.getStartTime());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = jobExecution.getStartTime();
        Duration duration = Duration.between(startTime, endTime);
        DecimalFormat decimalFormat = new DecimalFormat("#,###");

        // orders 테이블 통계
        Integer successCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders WHERE status = 'PROCESSED'", Integer.class);

        // processed_orders 테이블 통계
        Integer processedCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM processed_orders WHERE processing_result = 'SUCCESS'", Integer.class);

        // 총 매출 합계
        Double totalSales = jdbcTemplate.queryForObject("SELECT SUM(total_amount) FROM orders WHERE status = 'PROCESSED'", Double.class);

        // 상품별 판매량
        var productSales = jdbcTemplate.queryForList(
                "SELECT product_name, SUM(quantity) as total_quantity, SUM(total_amount) as total_sales " +
                        "FROM orders WHERE status = 'PROCESSED' " +
                        "GROUP BY product_name ORDER BY total_sales DESC");

        log.info("========================================");
        log.info("        배치 처리 리포트");
        log.info("========================================");
        log.info("작업 이름: {}", jobExecution.getJobInstance().getJobName());
        log.info("시작 시간: {}", startTime);
        log.info("종료 시간: {}", endTime);
        log.info("소요 시간: {}초", duration.toSeconds());
        log.info("----------------------------------------");
        log.info("처리 결과:");
        log.info("  - 성공 처리 건수 (orders): {}", (successCount != null ? successCount : 0));
        log.info("  - 리포트 저장 건수 (processed_orders): {}", (processedCount != null ? processedCount : 0));
        log.info("  - 총 매출액: {}원", (totalSales != null ? String.format("%,.0f", totalSales) : "0"));
        log.info("  - 작업 상태: {}", jobExecution.getStatus());
        log.info("----------------------------------------");

        if (productSales != null && !productSales.isEmpty()) {
            log.info("상품별 판매 현황:");
            for (var row : productSales) {
                String productName = (String) row.get("product_name");
                Long quantity = ((BigDecimal) row.get("total_quantity")).longValue();
                Double sales = (Double) row.get("total_sales");
                log.info("  - {}: {}개, {}원", productName, quantity, sales != null ? decimalFormat.format(sales) : 0.0);
            }
        }

        log.info("========================================");

        log.info("Job 완료 리포트 출력 완료");
    }

}