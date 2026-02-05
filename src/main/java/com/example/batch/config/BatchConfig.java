package com.example.batch.config;

import com.example.batch.domain.Order;
import com.example.batch.dto.OrderInputDto;
import com.example.batch.listener.JobCompletionNotificationListener;
import com.example.batch.listener.StepExecuteListener;
import com.example.batch.processor.OrderItemProcessor;
import com.example.batch.reader.OrderItemReader;
import com.example.batch.writer.CompositeOrderWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.batch.item.support.builder.CompositeItemProcessorBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Arrays;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final OrderItemProcessor orderItemProcessor;
    private final CompositeOrderWriter compositeOrderWriter;
    private final JobCompletionNotificationListener jobCompletionListener;
    private final StepExecuteListener stepExecuteListener;

    @Bean
    @StepScope  // Job 실행시마다 새로운 빈 생성
    public OrderItemReader orderItemReader(
            @Value("#{jobParameters['inputFile']}") String inputFile) {
        return new OrderItemReader(inputFile);
    }

    @Bean
    public Job processOrderJob() {
        return new JobBuilder("processOrderJob", jobRepository)
                .start(processOrderStep())
                .listener(jobCompletionListener)
                .build();
    }

    @Bean
    public Step processOrderStep() {
        return new StepBuilder("processOrderStep", jobRepository)
                .<OrderInputDto, Order>chunk(10, transactionManager) // 청크 사이즈 10으로 줄임 (테스트용)
                .reader(orderItemReader(null))  // 런타임에 주입됨
                .processor(compositeItemProcessor())
                .writer(compositeOrderWriter.compositeWriter())  // Composite Writer 사용
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(10)  // 스킵 제한을 10으로 줄임 (테스트용)
                .listener(stepExecuteListener)
                .build();
    }

    @Bean
    public Step reportStep() {
        return new StepBuilder("reportStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    // 리포트 생성 로직
                    log.info("=== 배치 처리 리포트 ===");
                    log.info("처리 완료: orders 테이블과 processed_orders 테이블에 데이터 저장됨");
                    return null;
                }, transactionManager)
                .build();
    }

    @Bean
    public CompositeItemProcessor<OrderInputDto, Order> compositeItemProcessor() {
        return new CompositeItemProcessorBuilder<OrderInputDto, Order>()
                .delegates(Arrays.asList(orderItemProcessor))
                .build();
    }

}