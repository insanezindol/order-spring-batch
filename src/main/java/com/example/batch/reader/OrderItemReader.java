package com.example.batch.reader;

import com.example.batch.dto.OrderInputDto;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

import java.io.File;
import java.io.FileReader;
import java.util.Iterator;

@Slf4j
public class OrderItemReader implements ItemReader<OrderInputDto> {

    private Iterator<OrderInputDto> orderIterator;
    private final String filePath;

    public OrderItemReader(String filePath) {
        log.info("OrderItemReader 생성 - 파일 경로: {}", filePath);
        this.filePath = filePath;
    }

    @Override
    public OrderInputDto read() throws Exception {
        if (orderIterator == null) {
            log.info("CSV 파일 읽기 시작: {}", filePath);
            if (filePath == null || filePath.trim().isEmpty()) {
                throw new IllegalArgumentException("파일 경로가 null이거나 비어있습니다.");
            }
            orderIterator = readCsvFile().iterator();
        }

        if (orderIterator.hasNext()) {
            OrderInputDto order = orderIterator.next();
            log.debug("데이터 읽기: {}", order.getOrderId());
            return order;
        } else {
            log.info("모든 데이터 읽기 완료");
            return null;
        }
    }

    private Iterable<OrderInputDto> readCsvFile() throws Exception {
        if (filePath == null) {
            throw new IllegalArgumentException("파일 경로가 null입니다.");
        }

        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("파일이 존재하지 않습니다: " + filePath);
        }

        FileReader reader = new FileReader(file);

        CsvToBean<OrderInputDto> csvToBean = new CsvToBeanBuilder<OrderInputDto>(reader)
                .withType(OrderInputDto.class)
                .withIgnoreLeadingWhiteSpace(true)
                .build();

        return csvToBean;
    }

}