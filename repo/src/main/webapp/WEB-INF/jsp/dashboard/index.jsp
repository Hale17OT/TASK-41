<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DispatchOps - Dashboard</title>
    <link rel="stylesheet" href="/static/css/fluent-tokens.css">
    <link rel="stylesheet" href="/static/css/app.css">
</head>
<body>
    <%@ include file="/WEB-INF/jsp/layout/_header.jsp" %>

    <div class="page-title">
        <h1>Dashboard</h1>
        <p class="text-secondary">Welcome, <c:out value="${currentUser.displayName}"/></p>
    </div>

    <!-- Metric Cards -->
    <div class="card-grid" id="metric-cards">
        <div class="card metric-card">
            <div class="shimmer shimmer-title"></div>
            <div class="shimmer shimmer-text"></div>
        </div>
        <div class="card metric-card">
            <div class="shimmer shimmer-title"></div>
            <div class="shimmer shimmer-text"></div>
        </div>
        <div class="card metric-card">
            <div class="shimmer shimmer-title"></div>
            <div class="shimmer shimmer-text"></div>
        </div>
    </div>

    <!-- Activity Feed -->
    <div class="card mt-l">
        <h2 class="mb-m">Recent Activity</h2>
        <div id="activity-feed">
            <div class="shimmer shimmer-text"></div>
            <div class="shimmer shimmer-text"></div>
            <div class="shimmer shimmer-text"></div>
        </div>
    </div>

<%@ include file="/WEB-INF/jsp/layout/_footer.jsp" %>
<script src="/static/js/dashboard.js"></script>
<script>
$(function() {
    if (typeof App.dashboard !== 'undefined') {
        App.dashboard.init();
    }
});
</script>
</body>
</html>
