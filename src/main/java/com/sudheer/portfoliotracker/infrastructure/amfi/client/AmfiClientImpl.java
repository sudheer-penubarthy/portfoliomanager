package com.sudheer.portfoliotracker.infrastructure.amfi.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmfiClientImpl implements AmfiClient{

    @Autowired
    AmfiHttpClient httpClient;
    @Override
    public InputStream fetchNavData(LocalDate from, LocalDate to) {
        String data = httpClient.fetchNavFile();
        return new ByteArrayInputStream(
                data.getBytes(StandardCharsets.UTF_8)
        );
    }
}
