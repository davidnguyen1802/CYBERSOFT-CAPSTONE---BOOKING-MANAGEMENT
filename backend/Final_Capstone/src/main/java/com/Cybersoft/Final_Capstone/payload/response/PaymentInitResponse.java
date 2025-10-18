package com.Cybersoft.Final_Capstone.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitResponse {
    private String orderId;
    private String requestId;
    private String payUrl;
    private String qrCodeUrl;
    private String deeplink;
    private Long amount;
    private String message;
}

