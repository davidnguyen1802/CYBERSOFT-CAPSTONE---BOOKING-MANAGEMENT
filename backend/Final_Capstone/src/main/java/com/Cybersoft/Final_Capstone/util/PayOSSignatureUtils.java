package com.Cybersoft.Final_Capstone.util;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Map;
import java.util.TreeMap;

@Component
public class PayOSSignatureUtils {

    public String createSignature(Map<String, Object> data, String checksumKey) {
        try {
            TreeMap<String, Object> sortedData = new TreeMap<>(data);
            StringBuilder dataStr = new StringBuilder();

            sortedData.forEach((key, value) -> {
                if (dataStr.length() > 0) {
                    dataStr.append("&");
                }
                dataStr.append(key).append("=").append(value);
            });

            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(checksumKey.getBytes(), "HmacSHA256");
            sha256Hmac.init(secretKey);

            byte[] hash = sha256Hmac.doFinal(dataStr.toString().getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error creating signature", e);
        }
    }

    public boolean verifySignature(String signature, Map<String, Object> data, String checksumKey) {
        String calculatedSignature = createSignature(data, checksumKey);
        return calculatedSignature.equals(signature);
    }
}
