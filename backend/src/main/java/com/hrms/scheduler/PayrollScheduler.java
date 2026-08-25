package com.hrms.scheduler;

import com.hrms.service.PayslipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Automatically runs payroll (generate PDFs + email) for the PREVIOUS month,
 * on a cron defined in application.yml (default: 2 AM on the 1st of each month).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PayrollScheduler {

    private final PayslipService payslipService;

    @Scheduled(cron = "${app.payroll.auto-run-cron}")
    public void runMonthlyPayrollJob() {
        LocalDate previousMonth = LocalDate.now().minusMonths(1);
        int month = previousMonth.getMonthValue();
        int year = previousMonth.getYear();

        log.info("Starting scheduled payroll run for {}/{}", month, year);
        try {
            var run = payslipService.runMonthlyPayroll(month, year);
            log.info("Scheduled payroll run complete: processed={}, emailSuccess={}, emailFailed={}",
                    run.getProcessedCount(), run.getEmailSuccessCount(), run.getEmailFailedCount());
        } catch (Exception ex) {
            log.error("Scheduled payroll run failed: {}", ex.getMessage(), ex);
        }
    }
}
