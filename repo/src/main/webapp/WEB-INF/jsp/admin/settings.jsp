<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DispatchOps - System Settings</title>
    <link rel="stylesheet" href="/static/css/fluent-tokens.css">
    <link rel="stylesheet" href="/static/css/app.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/layout/_header.jsp" %>

<div class="page-title">
    <h1>System Settings</h1>
</div>

<div class="card" style="max-width:640px">
    <div style="background:var(--fluent-color-status-warning);color:#000;padding:var(--fluent-spacing-s) var(--fluent-spacing-m);margin-bottom:var(--fluent-spacing-l);border-radius:var(--fluent-radius-s)">
        <strong>Read-Only Reference</strong> &mdash; These settings are configured via server environment variables
        and require an application restart to take effect. The values shown below are for reference only and cannot
        be changed from this page.
    </div>
    <form id="settings-form">
        <div class="form-group">
            <label class="form-label" for="setting-idle-warning">Idle Warning Threshold (minutes)</label>
            <input type="number" class="form-input" id="setting-idle-warning" name="idleWarningThresholdMin"
                   min="1" max="120" placeholder="e.g. 10" disabled>
            <p class="text-caption mt-s">Time of inactivity before a yellow idle warning appears on the dashboard. <em>Env: <code>IDLE_WARNING_MINUTES</code></em></p>
        </div>

        <div class="form-group">
            <label class="form-label" for="setting-appeal-window">Appeal Window (hours)</label>
            <input type="number" class="form-input" id="setting-appeal-window" name="appealWindowHours"
                   min="1" max="720" placeholder="e.g. 48" disabled>
            <p class="text-caption mt-s">How long a courier has to appeal a violation after it is issued. <em>Env: <code>APPEAL_WINDOW_HOURS</code></em></p>
        </div>

        <div class="form-group">
            <label class="form-label" for="setting-lockout-duration">Lockout Duration (minutes)</label>
            <input type="number" class="form-input" id="setting-lockout-duration" name="lockoutDurationMin"
                   min="1" max="1440" placeholder="e.g. 15" disabled>
            <p class="text-caption mt-s">Duration of account lockout after exceeding failed login attempts. <em>Env: <code>LOCKOUT_DURATION_MINUTES</code></em></p>
        </div>

        <div class="form-group">
            <label class="form-label" for="setting-max-upload">Max Upload Size (MB)</label>
            <input type="number" class="form-input" id="setting-max-upload" name="maxUploadSizeMb"
                   min="1" max="100" placeholder="e.g. 5" disabled>
            <p class="text-caption mt-s">Maximum file size allowed for avatar and document uploads. <em>Env: <code>MAX_UPLOAD_SIZE_MB</code></em></p>
        </div>
    </form>
</div>

<%@ include file="/WEB-INF/jsp/layout/_footer.jsp" %>
<script src="/static/js/admin.js"></script>
<script>
$(function() {
    if (typeof App.admin !== 'undefined' && typeof App.admin.settings !== 'undefined') {
        App.admin.settings.init();
    }
});
</script>
</body>
</html>
