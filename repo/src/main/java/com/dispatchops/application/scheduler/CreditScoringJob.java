package com.dispatchops.application.scheduler;

import com.dispatchops.application.service.CredibilityService;
import com.dispatchops.domain.model.User;
import com.dispatchops.infrastructure.persistence.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Daily batch recalculation of courier credit levels.
 * Runs at 00:00 every day.
 */
@Component
public class CreditScoringJob {

    private static final Logger log = LoggerFactory.getLogger(CreditScoringJob.class);

    private final UserMapper userMapper;
    private final CredibilityService credibilityService;

    public CreditScoringJob(UserMapper userMapper, CredibilityService credibilityService) {
        this.userMapper = userMapper;
        this.credibilityService = credibilityService;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void recalculateAllCreditLevels() {
        log.info("Starting daily credit level recalculation...");
        long start = System.currentTimeMillis();

        List<User> couriers = userMapper.findByRole("COURIER");
        int successCount = 0;
        int errorCount = 0;

        for (User courier : couriers) {
            try {
                credibilityService.recalculateCreditLevel(courier.getId());
                successCount++;
            } catch (Exception e) {
                errorCount++;
                log.error("Failed to recalculate credit for courier {}: {}", courier.getId(), e.getMessage());
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("Credit level recalculation complete: {} success, {} errors, {} ms",
                successCount, errorCount, elapsed);
    }
}
