package com.dispatchops.application.scheduler;

import com.dispatchops.domain.model.TrendingTerm;
import com.dispatchops.infrastructure.persistence.mapper.SearchTelemetryMapper;
import com.dispatchops.infrastructure.persistence.mapper.TrendingTermMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Hourly aggregation of search telemetry into materialized trending_terms table.
 * Prevents MySQL performance degradation from real-time aggregation queries.
 */
@Component
public class TrendingTermsJob {

    private static final Logger log = LoggerFactory.getLogger(TrendingTermsJob.class);

    private final SearchTelemetryMapper searchTelemetryMapper;
    private final TrendingTermMapper trendingTermMapper;

    public TrendingTermsJob(SearchTelemetryMapper searchTelemetryMapper,
                            TrendingTermMapper trendingTermMapper) {
        this.searchTelemetryMapper = searchTelemetryMapper;
        this.trendingTermMapper = trendingTermMapper;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void aggregateTrendingTerms() {
        log.info("Starting hourly trending terms aggregation...");
        long start = System.currentTimeMillis();

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime since = now.minusHours(24);

        // Clean old trending data
        trendingTermMapper.deleteOlderThan(now.minusDays(7));

        // Aggregate top terms from last 24 hours
        List<TrendingTerm> topTerms = searchTelemetryMapper.aggregateTopTerms(since, 20);

        for (TrendingTerm term : topTerms) {
            term.setPeriodStart(since);
            term.setPeriodEnd(now);
            term.setCalculatedAt(now);
            trendingTermMapper.insert(term);
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("Trending terms aggregation complete: {} terms, {} ms", topTerms.size(), elapsed);
    }
}
