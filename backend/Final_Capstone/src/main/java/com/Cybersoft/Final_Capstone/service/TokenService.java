package com.Cybersoft.Final_Capstone.service;

import com.Cybersoft.Final_Capstone.Entity.Token;
import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import com.Cybersoft.Final_Capstone.payload.response.RefreshTokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public interface TokenService {
    Token addRefreshToken(UserAccount user, String token);
    Token addRefreshToken(UserAccount user, String token, boolean rememberMe, HttpServletRequest request);
    RefreshTokenResponse refreshToken(String refreshToken) throws Exception;
    RefreshTokenResponse refreshToken(String refreshToken, HttpServletRequest request) throws Exception;
    void revokeToken(String token);
    void revokeAllUserTokens(UserAccount user);
    void revokeAllUserRefreshTokens(UserAccount user);
}
