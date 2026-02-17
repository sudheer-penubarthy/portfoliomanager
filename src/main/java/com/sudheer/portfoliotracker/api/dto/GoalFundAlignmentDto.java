package com.sudheer.portfoliotracker.api.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalFundAlignmentDto {
    private Long goalId;
    private String goalName;
    private List<String> schemeCodes;
    private List<SchemeAlignmentDto> schemes;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class SchemeAlignmentDto {
    private String schemeCode;
    private String schemeName;
    private String fundHouse;
}

