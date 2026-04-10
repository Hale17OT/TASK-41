<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DispatchOps - Notifications</title>
    <link rel="stylesheet" href="/static/css/fluent-tokens.css">
    <link rel="stylesheet" href="/static/css/app.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/layout/_header.jsp" %>

<div class="page-title flex justify-between items-center">
    <h1>Notifications</h1>
    <button class="fluent-btn fluent-btn-secondary" id="btn-mark-all-read">Mark All Read</button>
</div>

<div class="tab-bar" id="notification-tabs">
    <button class="tab-item active" data-tab="inbox">Inbox</button>
    <%-- Preferences tab hidden until backend persistence is implemented --%>
</div>

<!-- Inbox Tab -->
<div class="tab-panel" id="tab-inbox">
    <div id="notification-list">
        <!-- Notification Item Template (shimmer placeholders) -->
        <div class="card mb-s flex items-center gap-m p-m">
            <div class="shimmer shimmer-avatar"></div>
            <div style="flex:1">
                <div class="shimmer shimmer-text" style="width:70%"></div>
                <div class="shimmer shimmer-text" style="width:40%"></div>
            </div>
        </div>
        <div class="card mb-s flex items-center gap-m p-m">
            <div class="shimmer shimmer-avatar"></div>
            <div style="flex:1">
                <div class="shimmer shimmer-text" style="width:65%"></div>
                <div class="shimmer shimmer-text" style="width:35%"></div>
            </div>
        </div>
        <div class="card mb-s flex items-center gap-m p-m">
            <div class="shimmer shimmer-avatar"></div>
            <div style="flex:1">
                <div class="shimmer shimmer-text" style="width:60%"></div>
                <div class="shimmer shimmer-text" style="width:45%"></div>
            </div>
        </div>
    </div>

    <!-- Empty State -->
    <div class="empty-state hidden" id="inbox-empty">
        <div class="empty-icon">&#128276;</div>
        <div class="empty-title">No notifications</div>
        <p class="text-secondary mt-s">You're all caught up.</p>
    </div>
</div>

<!-- Preferences Tab (hidden until backend persistence is implemented) -->
<div class="tab-panel hidden" id="tab-preferences" style="display:none">
    <div class="card">
        <h2 class="mb-m">Notification Preferences</h2>
        <p class="text-caption mb-m">Choose which notifications you want to receive.</p>

        <div class="data-table-wrapper">
            <table class="dataTable w-full" id="preferences-table">
                <thead>
                    <tr>
                        <th>Notification Type</th>
                        <th>Description</th>
                        <th style="width:80px;text-align:center">Enabled</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td><strong>Job Status Updates</strong></td>
                        <td class="text-secondary">When a job changes status in the fulfillment board</td>
                        <td style="text-align:center">
                            <label style="position:relative;display:inline-block;width:44px;height:24px;cursor:pointer">
                                <input type="checkbox" name="pref_job_status" checked style="opacity:0;width:0;height:0">
                                <span style="position:absolute;inset:0;background:var(--fluent-color-neutral-stroke-1);border-radius:var(--fluent-radius-circular);transition:background var(--fluent-duration-normal)"></span>
                                <span style="position:absolute;top:2px;left:2px;width:20px;height:20px;background:white;border-radius:var(--fluent-radius-circular);transition:transform var(--fluent-duration-normal);box-shadow:var(--fluent-shadow-2)"></span>
                            </label>
                        </td>
                    </tr>
                    <tr>
                        <td><strong>Task Assignments</strong></td>
                        <td class="text-secondary">When a task is assigned to you or you are CC'd</td>
                        <td style="text-align:center">
                            <label style="position:relative;display:inline-block;width:44px;height:24px;cursor:pointer">
                                <input type="checkbox" name="pref_task_assign" checked style="opacity:0;width:0;height:0">
                                <span style="position:absolute;inset:0;background:var(--fluent-color-neutral-stroke-1);border-radius:var(--fluent-radius-circular);transition:background var(--fluent-duration-normal)"></span>
                                <span style="position:absolute;top:2px;left:2px;width:20px;height:20px;background:white;border-radius:var(--fluent-radius-circular);transition:transform var(--fluent-duration-normal);box-shadow:var(--fluent-shadow-2)"></span>
                            </label>
                        </td>
                    </tr>
                    <tr>
                        <td><strong>Payment Confirmations</strong></td>
                        <td class="text-secondary">When a payment is recorded or settled for your orders</td>
                        <td style="text-align:center">
                            <label style="position:relative;display:inline-block;width:44px;height:24px;cursor:pointer">
                                <input type="checkbox" name="pref_payment" checked style="opacity:0;width:0;height:0">
                                <span style="position:absolute;inset:0;background:var(--fluent-color-neutral-stroke-1);border-radius:var(--fluent-radius-circular);transition:background var(--fluent-duration-normal)"></span>
                                <span style="position:absolute;top:2px;left:2px;width:20px;height:20px;background:white;border-radius:var(--fluent-radius-circular);transition:transform var(--fluent-duration-normal);box-shadow:var(--fluent-shadow-2)"></span>
                            </label>
                        </td>
                    </tr>
                    <tr>
                        <td><strong>Credibility Alerts</strong></td>
                        <td class="text-secondary">Rating updates, credit level changes, and violation notices</td>
                        <td style="text-align:center">
                            <label style="position:relative;display:inline-block;width:44px;height:24px;cursor:pointer">
                                <input type="checkbox" name="pref_credibility" checked style="opacity:0;width:0;height:0">
                                <span style="position:absolute;inset:0;background:var(--fluent-color-neutral-stroke-1);border-radius:var(--fluent-radius-circular);transition:background var(--fluent-duration-normal)"></span>
                                <span style="position:absolute;top:2px;left:2px;width:20px;height:20px;background:white;border-radius:var(--fluent-radius-circular);transition:transform var(--fluent-duration-normal);box-shadow:var(--fluent-shadow-2)"></span>
                            </label>
                        </td>
                    </tr>
                    <tr>
                        <td><strong>Contract Updates</strong></td>
                        <td class="text-secondary">When a contract requires your signature or is countersigned</td>
                        <td style="text-align:center">
                            <label style="position:relative;display:inline-block;width:44px;height:24px;cursor:pointer">
                                <input type="checkbox" name="pref_contract" checked style="opacity:0;width:0;height:0">
                                <span style="position:absolute;inset:0;background:var(--fluent-color-neutral-stroke-1);border-radius:var(--fluent-radius-circular);transition:background var(--fluent-duration-normal)"></span>
                                <span style="position:absolute;top:2px;left:2px;width:20px;height:20px;background:white;border-radius:var(--fluent-radius-circular);transition:transform var(--fluent-duration-normal);box-shadow:var(--fluent-shadow-2)"></span>
                            </label>
                        </td>
                    </tr>
                    <tr>
                        <td><strong>System Announcements</strong></td>
                        <td class="text-secondary">Maintenance windows, feature updates, and policy changes</td>
                        <td style="text-align:center">
                            <label style="position:relative;display:inline-block;width:44px;height:24px;cursor:pointer">
                                <input type="checkbox" name="pref_system" checked style="opacity:0;width:0;height:0">
                                <span style="position:absolute;inset:0;background:var(--fluent-color-neutral-stroke-1);border-radius:var(--fluent-radius-circular);transition:background var(--fluent-duration-normal)"></span>
                                <span style="position:absolute;top:2px;left:2px;width:20px;height:20px;background:white;border-radius:var(--fluent-radius-circular);transition:transform var(--fluent-duration-normal);box-shadow:var(--fluent-shadow-2)"></span>
                            </label>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <div class="mt-m">
            <button class="fluent-btn fluent-btn-primary" id="btn-save-preferences">Save Preferences</button>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/layout/_footer.jsp" %>
<script src="/static/js/notifications.js"></script>
<script>
$(function() {
    if (typeof App.notifications !== 'undefined') {
        App.notifications.init();
    }
});
</script>
</body>
</html>
