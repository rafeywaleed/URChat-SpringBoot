package com.exotech.urchat.service;

import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.resource.Emailv31;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class OTPService {

    private final MailjetClient mailjetClient = new MailjetClient(
            System.getenv("MAILJET_APIKEY_PUBLIC"),
            System.getenv("MAILJET_APIKEY_SECRET")
    );

    private static final int OTP_LENGTH = 4;
    private static final long OTP_EXPIRY_MINUTES = 10;
    private static final String FROM_EMAIL = "urchata5@gmail.com";
    private static final String FROM_NAME = "URChat";

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

    private void sendMail(String to, String subject, String text) {
        try {
            JSONObject message = new JSONObject()
                    .put(Emailv31.Message.FROM, new JSONObject()
                            .put("Email", FROM_EMAIL)
                            .put("Name", FROM_NAME))
                    .put(Emailv31.Message.TO, new JSONArray()
                            .put(new JSONObject().put("Email", to)))
                    .put(Emailv31.Message.SUBJECT, subject)
                    .put(Emailv31.Message.TEXTPART, text);

            MailjetRequest request = new MailjetRequest(Emailv31.resource)
                    .property(Emailv31.MESSAGES, new JSONArray().put(message));

            mailjetClient.post(request);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Failed to send email via Mailjet: " + e.getMessage());
        }
    }

    public void sendRegistrationOtpEmail(String email, String otp) {
        String text = "Welcome to URChat!\n\n" +
                "Your email verification OTP is: " + otp +
                "\nThis OTP will expire in " + OTP_EXPIRY_MINUTES + " minutes." +
                "\n\nEnter this OTP in the app to complete your registration.";
        sendMail(email, "URChat - Email Verification OTP", text);
    }

    public void sendPasswordResetOtpEmail(String email, String otp) {
        String text = "You requested to reset your password.\n\n" +
                "Your password reset OTP is: " + otp +
                "\nThis OTP will expire in " + OTP_EXPIRY_MINUTES + " minutes." +
                "\n\nEnter this OTP in the app along with your new password.";
        sendMail(email, "URChat - Password Reset OTP", text);
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
