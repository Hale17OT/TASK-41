package com.dispatchops.web.controller;

import com.dispatchops.application.service.SearchService;
import com.dispatchops.domain.model.SearchIndex;
import com.dispatchops.domain.model.TrendingTerm;
import com.dispatchops.domain.model.User;
import com.dispatchops.web.dto.ApiResult;
import com.dispatchops.web.dto.PageResult;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<ApiResult<PageResult<SearchIndex>>> search(
            @RequestParam String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            HttpSession session) {
        log.debug("Searching for '{}' type={} sort={} author={} dateFrom={} dateTo={} status={}",
                q, type, sort, author, dateFrom, dateTo, status);
        User currentUser = (User) session.getAttribute("currentUser");
        Long userId = currentUser != null ? currentUser.getId() : null;

        PageResult<SearchIndex> result;
        if (currentUser != null && currentUser.getRole() == com.dispatchops.domain.model.enums.Role.COURIER) {
            // Courier-scoped search: query-level filtering by courier_id/author_id before pagination
            result = searchService.searchCourierScoped(q, currentUser.getId(), sort, dateFrom, dateTo, status, page, size);
        } else {
            result = searchService.search(q, type, sort, author, dateFrom, dateTo, status, page, size, userId);
        }

        return ResponseEntity.ok(ApiResult.success(result));
    }

    @GetMapping("/suggest")
    public ResponseEntity<ApiResult<Map<String, Object>>> getSuggestions(@RequestParam String q) {
        log.debug("Getting suggestions for '{}'", q);
        Map<String, Object> suggestions = searchService.getSuggestions(q);
        return ResponseEntity.ok(ApiResult.success(suggestions));
    }

    @GetMapping("/trending")
    public ResponseEntity<ApiResult<List<TrendingTerm>>> getTrending(
            @RequestParam(defaultValue = "10") int topN) {
        log.debug("Getting top {} trending terms", topN);
        List<TrendingTerm> trending = searchService.getTrendingTerms(topN);
        return ResponseEntity.ok(ApiResult.success(trending));
    }
}
