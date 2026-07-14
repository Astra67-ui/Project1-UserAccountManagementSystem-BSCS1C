package com.lampara.util;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
 
/**
 * Sends a 6-digit OTP to the user's email using Gmail SMTP.
 */
public class EmailOtpService {
 
    
    private static final String FROM     = System.getProperty("LAMPARA_EMAIL");
    private static final String APP_PASS = System.getProperty("LAMPARA_EMAIL_PASSWORD");
 
    public static void sendOtp(String toEmail, String otp) throws MessagingException {
 
        // Gmail SMTP settings
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",            "587");
 
        // Authenticate with the sender account
        final String user = FROM;
        final String pass = APP_PASS;
        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });
 
        // Build the email
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("Your Email Verification Code");
        message.setContent(
            "<div style='font-family: Arial, sans-serif; font-size: 14px;'>" +
            "<h2> Help us protect your account</h2>" +
            "<p>Hello User,</p>" +
            "<p>You are receiving this email because a request was made to verify your account or authorize a login on LAMPARA.</p>" +
            "<p>Before you finish creating your account, we need to verify your identity. On the verification page, enter the following code.</p>" +
            "<div style='background:#f0f9f9; border:2px solid #006666; border-radius:8px; padding:16px 24px; display:inline-block; margin:12px 0;'>" +
            "<span style='font-size:32px; font-weight:bold; letter-spacing:8px; color:#006666;'>" + otp + "</span>" +
            "</div>" +
            "<p>This code is valid for the next 10 minutes and can only be used once.</p>" +
            "<p>If you did not initiate this request, you can safely ignore this email. Someone may have typed your email address by mistake.</p>" +
            "<p>Do not share this code with other people. Keep it confidential.</p>" +
            "<br/>" +
            "</div>",
            "text/html"
        );
 
        Transport.send(message);
        System.out.println("[EmailOtpService] OTP sent to: " + toEmail);
    }
}
 