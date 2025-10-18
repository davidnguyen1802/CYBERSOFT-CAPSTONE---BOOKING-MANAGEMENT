package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImp implements EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${mail.from}")
    private String fromEmail;
    
    @Value("${password-reset.frontend-url}")
    private String frontendUrl;
    
    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken, String userName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Password Reset Request - Hotel Airbnb");
            
            String resetUrl = frontendUrl + "?token=" + resetToken;
            
            String htmlContent = buildPasswordResetEmail(userName, resetUrl, resetToken);
            
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
            System.out.println("Password reset email sent successfully to: " + toEmail);
            
        } catch (MessagingException e) {
            System.err.println("Failed to send password reset email: " + e.getMessage());
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
    
    private String buildPasswordResetEmail(String userName, String resetUrl, String token) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    .container {
                        background-color: #f9f9f9;
                        border-radius: 10px;
                        padding: 30px;
                        box-shadow: 0 2px 5px rgba(0,0,0,0.1);
                    }
                    .header {
                        text-align: center;
                        color: #FF385C;
                        margin-bottom: 30px;
                    }
                    .button {
                        display: inline-block;
                        padding: 12px 30px;
                        background-color: #FF385C;
                        color: white;
                        text-decoration: none;
                        border-radius: 5px;
                        margin: 20px 0;
                    }
                    .token-box {
                        background-color: #fff;
                        padding: 15px;
                        border-left: 4px solid #FF385C;
                        margin: 20px 0;
                        word-break: break-all;
                    }
                    .warning {
                        color: #666;
                        font-size: 14px;
                        margin-top: 20px;
                    }
                    .footer {
                        text-align: center;
                        margin-top: 30px;
                        color: #999;
                        font-size: 12px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🏠 Hotel Airbnb</h1>
                        <h2>Password Reset Request</h2>
                    </div>
                    
                    <p>Hello %s,</p>
                    
                    <p>We received a request to reset your password. Click the button below to reset your password:</p>
                    
                    <div style="text-align: center;">
                        <a href="%s" class="button">Reset Password</a>
                    </div>
                    
                    <div class="warning">
                        <p><strong>⚠️ Important:</strong></p>
                        <ul>
                            <li>This link will expire in 1 hour</li>
                            <li>If you didn't request a password reset, please ignore this email</li>
                            <li>Your password will remain unchanged until you create a new one</li>
                        </ul>
                    </div>
                    
                    <div class="footer">
                        <p>This is an automated message, please do not reply to this email.</p>
                        <p>&copy; 2025 Hotel. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, resetUrl, resetUrl, token);
    }
}

