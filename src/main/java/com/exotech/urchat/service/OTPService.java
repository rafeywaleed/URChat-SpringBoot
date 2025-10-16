package com.exotech.urchat.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class OTPService {

    private final JavaMailSender mailSender;

    private static final int OTP_LENGTH = 4;
    private static final long OTP_EXPIRY_MINUTES = 10;

    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();

    @Data
    @AllArgsConstructor
    private static class OtpData {
        private String otp;
        private long createdAt;
        private String purpose;
    }

    public String generateOtp() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) otp.append(random.nextInt(10));
        return otp.toString();
    }

    public void saveOtp(String email, String otp, String purpose) {
        otpStorage.put(email + ":" + purpose, new OtpData(otp, System.currentTimeMillis(), purpose));
    }

    public boolean verifyOtp(String email, String otp, String purpose) {
        String key = email + ":" + purpose;
        OtpData otpData = otpStorage.get(key);
        if (otpData == null) {
            return false;
        }

        // Check if OTP is expired
        long currentTime = System.currentTimeMillis();
        long elapsedMinutes = (currentTime - otpData.getCreatedAt()) / (1000 * 60);

        if (elapsedMinutes > OTP_EXPIRY_MINUTES) {
            otpStorage.remove(key);
            return false;
        }

        boolean isValid = otp.equals(otpData.getOtp());
        if (isValid) {
            otpStorage.remove(key);
        }
        return isValid;
    }

    public void sendRegistrationOtpEmail(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("urchata5@gmail.com");
        message.setTo(email);
        message.setSubject("URChat - Email Verification OTP");
        message.setText("Welcome to URChat!\n\n" +
                "Your email verification OTP is: " + otp +
                "\nThis OTP will expire in " + OTP_EXPIRY_MINUTES + " minutes." +
                "\n\nEnter this OTP in the app to complete your registration.");
        mailSender.send(message);
    }

    public void sendPasswordResetOtpEmail(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("urchata5@gmail.com");
        message.setTo(email);
        message.setSubject("URChat - Password Reset OTP");
        message.setText("You requested to reset your password.\n\n" +
                "Your password reset OTP is: " + otp +
                "\nThis OTP will expire in " + OTP_EXPIRY_MINUTES + " minutes." +
                "\n\nEnter this OTP in the app along with your new password.");
        mailSender.send(message);
    }

    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void cleanupExpiredEntries() {
        long currentTime = System.currentTimeMillis();

        otpStorage.entrySet().removeIf(entry -> {
            long elapsedMinutes = (currentTime - entry.getValue().getCreatedAt()) / (1000 * 60);
            return elapsedMinutes > OTP_EXPIRY_MINUTES;
        });
    }

}
