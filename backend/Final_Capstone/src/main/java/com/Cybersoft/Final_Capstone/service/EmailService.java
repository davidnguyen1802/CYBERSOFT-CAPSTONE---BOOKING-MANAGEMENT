package com.Cybersoft.Final_Capstone.service;

public interface EmailService {
    /**
     * Send password reset email with token
     * @param toEmail Recipient email address
     * @param resetToken Password reset token
     * @param userName User's full name
     */
    void sendPasswordResetEmail(String toEmail, String resetToken, String userName);
}

