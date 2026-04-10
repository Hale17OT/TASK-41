package com.dispatchops.application.service;

import com.dispatchops.domain.model.SearchIndex;
import com.dispatchops.domain.model.SearchTelemetry;
import com.dispatchops.domain.model.SynonymMapping;
import com.dispatchops.domain.model.TrendingTerm;
import com.dispatchops.infrastructure.persistence.mapper.SearchIndexMapper;
import com.dispatchops.infrastructure.persistence.mapper.SearchTelemetryMapper;
import com.dispatchops.infrastructure.persistence.mapper.SynonymMappingMapper;
import com.dispatchops.infrastructure.persistence.mapper.TrendingTermMapper;
import com.dispatchops.web.dto.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final SearchIndexMapper searchIndexMapper;
    private final SynonymMappingMapper synonymMappingMapper;
    private final SearchTelemetryMapper searchTelemetryMapper;
    private final TrendingTermMapper trendingTermMapper;

    public SearchService(SearchIndexMapper searchIndexMapper,
                         SynonymMappingMapper synonymMappingMapper,
                         SearchTelemetryMapper searchTelemetryMapper,
                         TrendingTermMapper trendingTermMapper) {
        this.searchIndexMapper = searchIndexMapper;
        this.synonymMappingMapper = synonymMappingMapper;
        this.searchTelemetryMapper = searchTelemetryMapper;
        this.trendingTermMapper = trendingTermMapper;
    }

    public PageResult<SearchIndex> search(String query, String entityType, int page, int size) {
        return search(query, entityType, null, null, page, size, null);
    }

    public PageResult<SearchIndex> search(String query, String entityType, int page, int size, Long userId) {
        return search(query, entityType, null, null, null, null, page, size, userId);
    }

    public PageResult<SearchIndex> search(String query, String entityType, String sort,
                                          String author, int page, int size, Long userId) {
        return search(query, entityType, sort, author, null, null, null, page, size, userId);
    }

    public PageResult<SearchIndex> search(String query, String entityType, String sort,
                                          String author, String dateFrom, String dateTo,
                                          int page, int size, Long userId) {
        return search(query, entityType, sort, author, dateFrom, dateTo, null, page, size, userId);
    }

    public PageResult<SearchIndex> search(String query, String entityType, String sort,
                                          String author, String dateFrom, String dateTo,
                                          String status, int page, int size, Long userId) {
        if (query == null || query.isBlank()) {
            log.debug("Empty search query, returning empty result");
            return new PageResult<>(Collections.emptyList(), page, size, 0);
        }

        Long authorId = null;
        if (author != null && !author.isBlank()) {
            try { authorId = Long.parseLong(author); } catch (NumberFormatException ignored) {}
        }

        log.debug("Searching query='{}', entityType={}, sort={}, authorId={}, dateFrom={}, dateTo={}, page={}, size={}",
                query, entityType, sort, authorId, dateFrom, dateTo, page, size);

        int offset = page * size;
        List<SearchIndex> results = searchIndexMapper.search(query, entityType, authorId, sort, dateFrom, dateTo, status, offset, size);
        int totalCount = searchIndexMapper.countSearch(query, entityType, authorId, dateFrom, dateTo, status);

        // Sanitize: strip description from results to prevent leaking operational details
        // Search results should show title/type/tags only - details accessed via entity-specific APIs with auth
        for (SearchIndex item : results) {
            item.setDescription(null);
        }

        // Record telemetry (non-blocking: don't fail search if telemetry insert fails)
        try {
            if (userId != null) {
                SearchTelemetry telemetry = new SearchTelemetry();
                telemetry.setUserId(userId);
                telemetry.setQueryText(query);
                telemetry.setResultCount(totalCount);
                telemetry.setSearchedAt(LocalDateTime.now());
                searchTelemetryMapper.insert(telemetry);
            }
        } catch (Exception e) {
            log.warn("Failed to record search telemetry: {}", e.getMessage());
        }

        log.info("Search query='{}' returned {} results (total={})", query, results.size(), totalCount);

        return new PageResult<>(results, page, size, totalCount);
    }

    /**
     * Courier-scoped search: JOB results filtered by delivery_jobs.courier_id at query level,
     * PROFILE results filtered by author_id. Pagination and counts are accurate.
     */
    public PageResult<SearchIndex> searchCourierScoped(String query, Long courierId, String sort,
                                                        String dateFrom, String dateTo, String status,
                                                        int page, int size) {
        if (query == null || query.isBlank()) {
            return new PageResult<>(Collections.emptyList(), page, size, 0);
        }

        int offset = page * size;
        List<SearchIndex> results = searchIndexMapper.searchCourierScoped(
                query, courierId, sort, dateFrom, dateTo, status, offset, size);
        int totalCount = searchIndexMapper.countSearchCourierScoped(
                query, courierId, dateFrom, dateTo, status);

        for (SearchIndex item : results) {
            item.setDescription(null);
        }

        try {
            SearchTelemetry telemetry = new SearchTelemetry();
            telemetry.setUserId(courierId);
            telemetry.setQueryText(query);
            telemetry.setResultCount(totalCount);
            telemetry.setSearchedAt(LocalDateTime.now());
            searchTelemetryMapper.insert(telemetry);
        } catch (Exception e) {
            log.warn("Failed to record search telemetry: {}", e.getMessage());
        }

        log.info("Courier-scoped search query='{}' courierId={} returned {} results (total={})",
                query, courierId, results.size(), totalCount);
        return new PageResult<>(results, page, size, totalCount);
    }

    public void indexEntity(String entityType, Long entityId, String title,
                            String description, String tags, Long authorId) {
        log.debug("Indexing entity entityType={}, entityId={}", entityType, entityId);

        SearchIndex idx = new SearchIndex();
        idx.setEntityType(entityType);
        idx.setEntityId(entityId);
        idx.setTitle(title);
        idx.setDescription(description);
        idx.setTags(tags);
        idx.setAuthorId(authorId);
        idx.setLastIndexedAt(LocalDateTime.now());

        searchIndexMapper.upsert(idx);
        log.info("Indexed entity entityType={}, entityId={}", entityType, entityId);
    }

    public void deindexEntity(String entityType, Long entityId) {
        log.debug("Deindexing entity entityType={}, entityId={}", entityType, entityId);
        searchIndexMapper.delete(entityType, entityId);
        log.info("Deindexed entity entityType={}, entityId={}", entityType, entityId);
    }

    public Map<String, Object> getSuggestions(String query) {
        log.debug("Getting suggestions for query='{}'", query);

        Set<String> synonyms = new LinkedHashSet<>();

        if (query != null && !query.isBlank()) {
            String[] words = query.trim().split("\\s+");
            for (String word : words) {
                List<SynonymMapping> mappings = synonymMappingMapper.findByTerm(word);
                for (SynonymMapping mapping : mappings) {
                    synonyms.add(mapping.getSynonym());
                }
            }
        }

        List<TrendingTerm> trending = trendingTermMapper.findRecent(5);

        // Related categories: entity types that contain results for query terms or synonyms
        List<String> relatedCategories = new ArrayList<>();
        List<String> allTerms = new ArrayList<>();
        allTerms.add(query);
        allTerms.addAll(synonyms);
        for (String term : allTerms) {
            if (term == null || term.isBlank()) continue;
            for (String category : List.of("JOB", "TASK", "CONTRACT", "PAYMENT", "PROFILE")) {
                try {
                    int count = searchIndexMapper.countSearch(term, category, null, null, null, null);
                    if (count > 0 && !relatedCategories.contains(category)) {
                        relatedCategories.add(category);
                    }
                } catch (Exception e) {
                    // FULLTEXT query may fail for very short terms; skip
                }
            }
            if (relatedCategories.size() >= 3) break;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("synonyms", new ArrayList<>(synonyms));
        result.put("relatedCategories", relatedCategories);
        result.put("trending", trending);
        return result;
    }

    public List<TrendingTerm> getTrendingTerms(int topN) {
        log.debug("Fetching top {} trending terms", topN);
        return trendingTermMapper.findRecent(topN);
    }
}
