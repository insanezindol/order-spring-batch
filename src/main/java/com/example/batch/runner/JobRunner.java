package com.example.batch.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobRunner implements CommandLineRunner {

    private final Job processOrderJob;
    private final JobRepository jobRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("JobRunner 실행 시작");

        // 입력 파일 경로
        String inputFile = "input/orders.csv";

        // 입력 파일이 없으면 테스트 파일 생성
        Path inputPath = Paths.get(inputFile);
        if (!Files.exists(inputPath)) {
            log.warn("입력 파일이 존재하지 않습니다: {}", inputFile);
            createTestFile(inputFile);
        }

        // 파일 존재 확인
        if (!Files.exists(Paths.get(inputFile))) {
            log.error("입력 파일을 찾을 수 없습니다: {}", inputFile);
            return;
        }

        log.info("입력 파일 확인 완료: {}", inputFile);

        // Job 파라미터 설정
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("inputFile", inputFile)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        log.info("배치 작업 시작 - 입력 파일: {}", inputFile);

        try {
            // TaskExecutorJobLauncher 생성 및 설정
            TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
            jobLauncher.setJobRepository(jobRepository);
            jobLauncher.setTaskExecutor(new SyncTaskExecutor()); // 동기 실행
            jobLauncher.afterPropertiesSet(); // 초기화

            // Job 실행
            jobLauncher.run(processOrderJob, jobParameters);
            log.info("배치 작업 완료");
        } catch (Exception e) {
            log.error("배치 작업 실행 중 오류 발생", e);
            throw e; // 예외를 다시 던져서 로그 확인
        }
    }

    private void createTestFile(String filePath) throws Exception {
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());

        String csvContent = """
            order_id,customer_name,product_name,quantity,price,order_date
            ORD001,홍길동,노트북,2,1500000,2024-01-15 10:30:00
            ORD002,김철수,스마트폰,1,1200000,2024-01-16 14:20:00
            ORD003,이영희,태블릿,3,800000,2024-01-17 09:15:00
            ORD004,박지성,헤드폰,5,200000,2024-01-18 16:45:00
            ORD005,손흥민,키보드,5,150000,2024-01-19 11:10:00
            ORD006,김민재,마우스,2,50000,2024-01-20 13:25:00
            ORD007,김연아,모니터,1,300000,2024-01-21 15:30:00
            ORD008,류현진,의자,10,300000,2023-01-22 10:00:00
            """;

        Files.writeString(path, csvContent);
        log.info("테스트 CSV 파일 생성: {}", filePath);
    }
}