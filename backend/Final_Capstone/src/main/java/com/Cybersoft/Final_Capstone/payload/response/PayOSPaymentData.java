package com.Cybersoft.Final_Capstone.payload.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayOSPaymentData {
    private Long orderCode;
    private String status;

    @JsonProperty("amount")
    private Long amount;

    @JsonProperty("checkoutUrl")
    private String checkoutUrl;

    @JsonProperty("qrCode")
    private String qrCode;

    private String message;

    private String accountNumber;

    private String accountName;


}