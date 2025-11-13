package com.Cybersoft.Final_Capstone.payload.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayOSPaymentResponse {
    private int code;
    private String desc;
    private PayOSPaymentData data;
    private String signature;
}