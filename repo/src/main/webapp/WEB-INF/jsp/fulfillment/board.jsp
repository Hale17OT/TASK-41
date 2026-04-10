<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DispatchOps - Fulfillment Board</title>
    <link rel="stylesheet" href="/static/css/fluent-tokens.css">
    <link rel="stylesheet" href="/static/css/app.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/layout/_header.jsp" %>

<div class="page-title flex justify-between items-center">
    <h1>Fulfillment Board</h1>
    <div class="flex gap-s">
        <button class="fluent-btn fluent-btn-primary" id="btn-create-job">Create Job</button>
        <button class="fluent-btn fluent-btn-secondary" id="btn-pick-list">Generate Pick List</button>
        <button class="fluent-btn fluent-btn-secondary" id="btn-sort-list">Generate Sort List</button>
    </div>
</div>

<div class="kanban-board" id="kanban-board">
    <!-- Created Column -->
    <div class="kanban-column" id="col-created">
        <div class="kanban-column-header">
            <span>Created</span>
            <span class="count-badge" data-count="created">0</span>
        </div>
        <div class="kanban-list" data-status="CREATED">
            <div class="shimmer shimmer-card"></div>
            <div class="shimmer shimmer-card"></div>
            <div class="shimmer shimmer-card"></div>
        </div>
    </div>

    <!-- Picked Column -->
    <div class="kanban-column" id="col-picked">
        <div class="kanban-column-header">
            <span>Picked</span>
            <span class="count-badge" data-count="picked">0</span>
        </div>
        <div class="kanban-list" data-status="PICKED">
            <div class="shimmer shimmer-card"></div>
            <div class="shimmer shimmer-card"></div>
        </div>
    </div>

    <!-- In Transit Column -->
    <div class="kanban-column" id="col-in-transit">
        <div class="kanban-column-header">
            <span>In Transit</span>
            <span class="count-badge" data-count="in-transit">0</span>
        </div>
        <div class="kanban-list" data-status="IN_TRANSIT">
            <div class="shimmer shimmer-card"></div>
            <div class="shimmer shimmer-card"></div>
        </div>
    </div>

    <!-- Delivered Column -->
    <div class="kanban-column" id="col-delivered">
        <div class="kanban-column-header">
            <span>Delivered</span>
            <span class="count-badge" data-count="delivered">0</span>
        </div>
        <div class="kanban-list" data-status="DELIVERED">
            <div class="shimmer shimmer-card"></div>
        </div>
    </div>

    <!-- Exception Column -->
    <div class="kanban-column" id="col-exception">
        <div class="kanban-column-header">
            <span>Exception</span>
            <span class="count-badge" data-count="exception">0</span>
        </div>
        <div class="kanban-list" data-status="EXCEPTION">
            <div class="shimmer shimmer-card"></div>
        </div>
    </div>
</div>

<%-- Job creation modal is built dynamically by fulfillment.js via App.modal.confirm() --%>

<%@ include file="/WEB-INF/jsp/layout/_footer.jsp" %>
<script src="/static/js/fulfillment.js"></script>
<script>
$(function() {
    if (typeof App.fulfillment !== 'undefined') {
        App.fulfillment.init();
    }
});
</script>
</body>
</html>
