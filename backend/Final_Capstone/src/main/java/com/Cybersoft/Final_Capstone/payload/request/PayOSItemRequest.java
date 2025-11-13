package com.Cybersoft.Final_Capstone.payload.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PayOSItemRequest {
    private String name;
    private Integer quantity;
    private Long price;
    private Integer taxPercentage;
    private String unit;
}
