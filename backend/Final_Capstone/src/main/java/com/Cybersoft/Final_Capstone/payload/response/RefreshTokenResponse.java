package com.Cybersoft.Final_Capstone.payload.response;

import com.Cybersoft.Final_Capstone.Entity.Token;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO để trả về kết quả token rotation
 * Chứa cả JWT string (để set cookie) và Token entity (để lấy metadata)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResponse {
    private String jwtToken;        // JWT string thực tế (để set vào cookie)
    private Token tokenEntity;      // Token entity (để lấy user, rememberMe, etc.)
}

