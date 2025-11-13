package com.Cybersoft.Final_Capstone.payload.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PayOSPaymentRequest {
    private Integer orderCode;   // ✅ Changed from Long to Integer (PayOS requires integer type)
    private Long amount;
    private String description;
    private String cancelUrl;
    private String returnUrl;
    private Long expiredAt;
    private String signature;
    private List<PayOSItemRequest> items;
}
