package com.example.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StepExecuteListener implements StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("### Step 시작: {}", stepExecution.getStepName());
        log.info("### 읽기 카운트: {}", stepExecution.getReadCount());
        log.info("### 쓰기 카운트: {}", stepExecution.getWriteCount());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("### Step 완료: {}", stepExecution.getStepName());
        log.info("### 읽기 카운트: {}", stepExecution.getReadCount());
        log.info("### 쓰기 카운트: {}", stepExecution.getWriteCount());
        log.info("### 스킵 카운트: {}", stepExecution.getSkipCount());
        log.info("### 처리 시간: {}", stepExecution.getCommitCount());

        long failedCount = stepExecution.getReadCount() - stepExecution.getWriteCount() - stepExecution.getSkipCount();
        if (failedCount > 0) {
            log.info("### 실패 건수: {}", failedCount);
        }

        return stepExecution.getExitStatus();
    }
}