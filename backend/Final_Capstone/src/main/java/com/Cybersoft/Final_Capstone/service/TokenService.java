package com.Cybersoft.Final_Capstone.service;

import com.Cybersoft.Final_Capstone.Entity.Token;
import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import org.springframework.stereotype.Service;

@Service
public interface TokenService {
    Token addAccessToken(UserAccount user, String token, boolean isMobileDevice);
    Token addRefreshToken(UserAccount user, String token);
    Token refreshToken(String refreshToken) throws Exception;
    void revokeToken(String token);
    void revokeAllUserTokens(UserAccount user);
    void revokeAllUserRefreshTokens(UserAccount user);
}
