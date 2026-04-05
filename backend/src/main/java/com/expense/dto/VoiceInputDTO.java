package com.expense.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceInputDTO {
    
    private String transcript;
    private String audioPath;
    private String language;
    private Double confidence;
    private String deviceInfo;
}
