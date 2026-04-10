<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DispatchOps - Task Calendar</title>
    <link rel="stylesheet" href="/static/css/fluent-tokens.css">
    <link rel="stylesheet" href="/static/css/app.css">
    <link rel="stylesheet" href="/static/css/vendor/fullcalendar.min.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/layout/_header.jsp" %>

<div class="page-title flex justify-between items-center">
    <h1>Task Calendar</h1>
    <div class="flex gap-s">
        <button class="fluent-btn fluent-btn-secondary" id="btn-list-view" onclick="window.location.href='/tasks'">List View</button>
        <button class="fluent-btn fluent-btn-primary" id="btn-create-task-cal">Create Task</button>
    </div>
</div>

<div class="card">
    <div id="task-calendar" style="min-height:600px">
        <!-- FullCalendar renders here -->
    </div>
</div>

<%@ include file="/WEB-INF/jsp/layout/_footer.jsp" %>
<script src="/static/js/vendor/fullcalendar.min.js"></script>
<script src="/static/js/tasks.js"></script>
<script>
$(function() {
    if (typeof App.tasks !== 'undefined' && typeof App.tasks.calendar !== 'undefined') {
        App.tasks.calendar.init();
    }
});
</script>
</body>
</html>
