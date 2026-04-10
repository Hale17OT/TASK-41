<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DispatchOps - Search Results<c:if test="${not empty param.q}"> for "<c:out value="${param.q}"/>"</c:if></title>
    <link rel="stylesheet" href="/static/css/fluent-tokens.css">
    <link rel="stylesheet" href="/static/css/app.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/layout/_header.jsp" %>

<div class="page-title">
    <h1>Search</h1>
    <div class="flex gap-s mt-s" style="max-width:600px">
        <input type="text" class="form-input" id="search-query" placeholder="Search jobs, tasks, contracts..."
               value="<c:out value="${param.q}"/>" style="flex:1">
        <button class="fluent-btn fluent-btn-primary" id="search-btn">Search</button>
    </div>
</div>

<div class="flex gap-l">
    <!-- Filters Sidebar -->
    <aside style="width:200px;flex-shrink:0">
        <div class="card">
            <h3 class="mb-m">Filters</h3>

            <div class="form-group">
                <label class="form-label">Entity Type</label>
                <div class="flex flex-col gap-xs">
                    <label class="flex items-center gap-xs" style="cursor:pointer">
                        <input type="checkbox" name="type" value="JOB" checked> Jobs
                    </label>
                    <label class="flex items-center gap-xs" style="cursor:pointer">
                        <input type="checkbox" name="type" value="TASK" checked> Tasks
                    </label>
                    <label class="flex items-center gap-xs" style="cursor:pointer">
                        <input type="checkbox" name="type" value="USER" checked> Users
                    </label>
                    <label class="flex items-center gap-xs" style="cursor:pointer">
                        <input type="checkbox" name="type" value="CONTRACT" checked> Contracts
                    </label>
                    <label class="flex items-center gap-xs" style="cursor:pointer">
                        <input type="checkbox" name="type" value="PAYMENT" checked> Payments
                    </label>
                </div>
            </div>

            <div class="form-group">
                <label class="form-label" for="filter-date-from">Date From</label>
                <input type="date" class="form-input" id="filter-date-from" name="dateFrom">
            </div>

            <div class="form-group">
                <label class="form-label" for="filter-date-to">Date To</label>
                <input type="date" class="form-input" id="filter-date-to" name="dateTo">
            </div>

            <div class="form-group">
                <label class="form-label" for="filter-status">Status</label>
                <select class="form-select" id="filter-status" name="status">
                    <option value="">All Statuses</option>
                    <option value="CREATED">Created</option>
                    <option value="PICKED">Picked</option>
                    <option value="IN_TRANSIT">In Transit</option>
                    <option value="DELIVERED">Delivered</option>
                    <option value="EXCEPTION">Exception</option>
                </select>
            </div>

            <div class="form-group">
                <label class="form-label" for="filter-sort">Sort By</label>
                <select class="form-select" id="filter-sort" name="sort">
                    <option value="">Relevance</option>
                    <option value="date">Date (newest)</option>
                    <option value="title">Title (A-Z)</option>
                </select>
            </div>

            <div class="form-group">
                <label class="form-label" for="filter-author">Author ID</label>
                <input type="number" class="form-input" id="filter-author" name="author" placeholder="User ID">
            </div>

            <button class="fluent-btn fluent-btn-primary w-full" id="btn-apply-filters">Apply Filters</button>
        </div>
    </aside>

    <!-- Results Area -->
    <div style="flex:1;min-width:0">
        <div id="search-results">
            <div class="card mb-s">
                <div class="shimmer shimmer-title"></div>
                <div class="shimmer shimmer-text"></div>
                <div class="shimmer shimmer-text" style="width:50%"></div>
            </div>
            <div class="card mb-s">
                <div class="shimmer shimmer-title"></div>
                <div class="shimmer shimmer-text"></div>
                <div class="shimmer shimmer-text" style="width:50%"></div>
            </div>
            <div class="card mb-s">
                <div class="shimmer shimmer-title"></div>
                <div class="shimmer shimmer-text"></div>
                <div class="shimmer shimmer-text" style="width:50%"></div>
            </div>
        </div>

        <!-- No Results State (hidden by default) -->
        <div class="empty-state hidden" id="no-results">
            <div class="empty-icon">&#128269;</div>
            <div class="empty-title">No results for "<span id="no-results-query"></span>"</div>
            <p class="text-secondary mt-s">Try adjusting your filters or search with different keywords.</p>
            <div class="mt-m" id="search-suggestions">
                <!-- Suggestions populated by JS -->
            </div>
        </div>
    </div>

    <!-- Trending Sidebar -->
    <aside style="width:200px;flex-shrink:0">
        <div class="card">
            <h3 class="mb-m">Trending</h3>
            <ol id="trending-list" style="padding-left:var(--fluent-spacing-m);margin:0">
                <li class="mb-s"><div class="shimmer shimmer-text" style="width:90%"></div></li>
                <li class="mb-s"><div class="shimmer shimmer-text" style="width:85%"></div></li>
                <li class="mb-s"><div class="shimmer shimmer-text" style="width:80%"></div></li>
                <li class="mb-s"><div class="shimmer shimmer-text" style="width:75%"></div></li>
                <li class="mb-s"><div class="shimmer shimmer-text" style="width:70%"></div></li>
            </ol>
        </div>
    </aside>
</div>

<%@ include file="/WEB-INF/jsp/layout/_footer.jsp" %>
<script src="/static/js/search.js"></script>
<script>
$(function() {
    if (typeof App.search !== 'undefined') {
        App.search.init();
    }
});
</script>
</body>
</html>
