package com.hrms.service;

import com.hrms.entity.EmailLog;
import com.hrms.entity.EmailStatus;
import com.hrms.entity.Payslip;
import com.hrms.repository.EmailLogRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Sends the generated payslip PDF as an email attachment and records the outcome
 * (SUCCESS / FAILED) in email_logs — required so failures never fail silently.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;

    @Value("${app.mail.from-address}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    /**
     * Attempts to email the payslip. Never throws — always logs the result and
     * returns whether it succeeded so the caller (PayslipService) can update counts.
     */
    @Transactional
    public boolean sendPayslipEmail(Payslip payslip) {
        String recipient = payslip.getEmployee().getEmail();
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            String monthName = Month.of(payslip.getPayPeriodMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH);

            helper.setFrom(fromAddress, fromName);
            helper.setTo(recipient);
            helper.setSubject(String.format("Your Payslip for %s %d", monthName, payslip.getPayPeriodYear()));
            helper.setText(buildEmailBody(payslip, monthName), false);

            File pdfFile = new File(payslip.getPdfPath());
            helper.addAttachment(pdfFile.getName(), pdfFile);

            mailSender.send(message);

            logResult(payslip, recipient, EmailStatus.SUCCESS, null);
            return true;

        } catch (MessagingException | java.io.UnsupportedEncodingException | RuntimeException ex) {
            log.error("Failed to email payslip {} to {}: {}", payslip.getId(), recipient, ex.getMessage());
            logResult(payslip, recipient, EmailStatus.FAILED, ex.getMessage());
            return false;
        }
    }

    private String buildEmailBody(Payslip payslip, String monthName) {
        var e = payslip.getEmployee();
        return "Dear " + e.getFullName() + ",\n\n"
                + "Please find attached your payslip for " + monthName + " " + payslip.getPayPeriodYear() + ".\n\n"
                + "Net Pay: Rs. " + payslip.getNetSalary() + "\n\n"
                + "If you have any questions about your payslip, please reach out to HR.\n\n"
                + "Regards,\nHR Team";
    }

    private void logResult(Payslip payslip, String recipient, EmailStatus status, String error) {
        EmailLog logEntry = EmailLog.builder()
                .payslip(payslip)
                .recipientEmail(recipient)
                .status(status)
                .errorMessage(error != null ? truncate(error, 1000) : null)
                .retryCount(0)
                .build();
        emailLogRepository.save(logEntry);
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }
}
