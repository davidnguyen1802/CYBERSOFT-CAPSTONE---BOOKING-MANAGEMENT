package com.Cybersoft.Final_Capstone.service;

import java.io.IOException;
import java.util.Map;

public interface AuthenticationService {
    String generateAuthUrl(String loginType);
    Map<String, Object> authenticateAndFetchProfile(String code, String loginType) throws IOException;
}
