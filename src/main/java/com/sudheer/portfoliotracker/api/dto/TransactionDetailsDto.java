package com.sudheer.portfoliotracker.api.dto;

import com.sudheer.portfoliotracker.enums.TxnType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDetailsDto {
    private Long id;
    private String schemeCode;
    private String schemeName;
    private TxnType txnType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate txnDate;

    private BigDecimal units;
    private BigDecimal amount;
    private BigDecimal pricePerUnit;
    private String remarks;
}

