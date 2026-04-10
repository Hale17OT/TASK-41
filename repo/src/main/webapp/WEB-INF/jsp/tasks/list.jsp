<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DispatchOps - Tasks</title>
    <link rel="stylesheet" href="/static/css/fluent-tokens.css">
    <link rel="stylesheet" href="/static/css/app.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/layout/_header.jsp" %>

<div class="page-title flex justify-between items-center">
    <h1>Tasks</h1>
    <div class="flex gap-s">
        <button class="fluent-btn fluent-btn-secondary" id="btn-calendar-view">Calendar View</button>
        <button class="fluent-btn fluent-btn-primary" id="btn-create-task">Create Task</button>
    </div>
</div>

<div class="tab-bar" id="task-tabs">
    <button class="tab-item active" data-tab="todo">To-Do</button>
    <button class="tab-item" data-tab="done">Done</button>
    <button class="tab-item" data-tab="cc">CC</button>
</div>

<!-- To-Do Tab Content -->
<div class="tab-panel" id="tab-todo">
    <div id="task-list">
        <div class="card mb-s">
            <div class="shimmer shimmer-text"></div>
            <div class="shimmer shimmer-text" style="width:60%"></div>
        </div>
        <div class="card mb-s">
            <div class="shimmer shimmer-text"></div>
            <div class="shimmer shimmer-text" style="width:60%"></div>
        </div>
        <div class="card mb-s">
            <div class="shimmer shimmer-text"></div>
            <div class="shimmer shimmer-text" style="width:60%"></div>
        </div>
        <div class="card mb-s">
            <div class="shimmer shimmer-text"></div>
            <div class="shimmer shimmer-text" style="width:60%"></div>
        </div>
    </div>
</div>

<!-- Done Tab Content -->
<div class="tab-panel hidden" id="tab-done">
    <div id="task-list-done">
        <div class="empty-state">
            <div class="empty-icon">&#9745;</div>
            <div class="empty-title">No completed tasks</div>
        </div>
    </div>
</div>

<!-- CC Tab Content -->
<div class="tab-panel hidden" id="tab-cc">
    <div id="task-list-cc">
        <div class="empty-state">
            <div class="empty-icon">&#128203;</div>
            <div class="empty-title">No CC tasks</div>
        </div>
    </div>
</div>

<%-- Task creation modal is built dynamically by tasks.js via App.modal.confirm() --%>

<%@ include file="/WEB-INF/jsp/layout/_footer.jsp" %>
<script src="/static/js/tasks.js"></script>
<script>
$(function() {
    if (typeof App.tasks !== 'undefined') {
        App.tasks.init();
    }
});
</script>
</body>
</html>
