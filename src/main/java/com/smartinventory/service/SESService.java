package com.smartinventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class SESService {

    private final JavaMailSender mailSender;

    @Value("${aws.ses.from-email:noreply@smartinventory.com}")
    private String fromEmail;

    public void sendWelcomeEmail(String toEmail, String username) {
        String subject = "Welcome to SmartInventory!";
        String body = buildWelcomeEmailBody(username);
        sendEmail(toEmail, subject, body);
    }

    public void sendOrderConfirmationEmail(String toEmail, String orderNumber, String totalAmount) {
        String subject = "Order Confirmation - " + orderNumber;
        String body = buildOrderConfirmationBody(orderNumber, totalAmount);
        sendEmail(toEmail, subject, body);
    }

    public void sendLowStockAlertEmail(String toEmail, String productName, int currentStock) {
        String subject = "Low Stock Alert: " + productName;
        String body = buildLowStockAlertBody(productName, currentStock);
        sendEmail(toEmail, subject, body);
    }

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String subject = "Password Reset Request";
        String body = buildPasswordResetBody(resetToken);
        sendEmail(toEmail, subject, body);
    }

    private void sendEmail(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to: {}, subject: {}", toEmail, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildWelcomeEmailBody(String username) {
        return """
            <html><body>
            <h2>Welcome to SmartInventory, %s!</h2>
            <p>Your account has been successfully created.</p>
            <p>You can now log in and start managing your inventory efficiently.</p>
            <br><p>Best regards,<br>SmartInventory Team</p>
            </body></html>
            """.formatted(username);
    }

    private String buildOrderConfirmationBody(String orderNumber, String totalAmount) {
        return """
            <html><body>
            <h2>Order Confirmed!</h2>
            <p>Your order <strong>%s</strong> has been placed successfully.</p>
            <p>Total Amount: <strong>$%s</strong></p>
            <p>We will process your order shortly.</p>
            <br><p>Best regards,<br>SmartInventory Team</p>
            </body></html>
            """.formatted(orderNumber, totalAmount);
    }

    private String buildLowStockAlertBody(String productName, int currentStock) {
        return """
            <html><body>
            <h2>Low Stock Alert</h2>
            <p>Product <strong>%s</strong> is running low on stock.</p>
            <p>Current Stock: <strong>%d units</strong></p>
            <p>Please restock as soon as possible.</p>
            <br><p>Best regards,<br>SmartInventory System</p>
            </body></html>
            """.formatted(productName, currentStock);
    }

    private String buildPasswordResetBody(String resetToken) {
        return """
            <html><body>
            <h2>Password Reset Request</h2>
            <p>Use the following token to reset your password:</p>
            <p><strong>%s</strong></p>
            <p>This token expires in 1 hour.</p>
            <br><p>Best regards,<br>SmartInventory Team</p>
            </body></html>
            """.formatted(resetToken);
    }
}
