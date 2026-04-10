<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DispatchOps - User Management</title>
    <link rel="stylesheet" href="/static/css/fluent-tokens.css">
    <link rel="stylesheet" href="/static/css/app.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/layout/_header.jsp" %>

<div class="page-title flex justify-between items-center">
    <h1>User Management</h1>
    <button class="fluent-btn fluent-btn-primary" id="btn-add-user">Add User</button>
</div>

<div class="data-table-wrapper">
    <table class="dataTable w-full" id="users-table">
        <thead>
            <tr>
                <th>Username</th>
                <th>Display Name</th>
                <th>Role</th>
                <th>Credit Level</th>
                <th>Status</th>
                <th>Last Login</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody id="users-body">
            <tr>
                <td colspan="7"><div class="shimmer shimmer-text"></div></td>
            </tr>
            <tr>
                <td colspan="7"><div class="shimmer shimmer-text"></div></td>
            </tr>
            <tr>
                <td colspan="7"><div class="shimmer shimmer-text"></div></td>
            </tr>
            <tr>
                <td colspan="7"><div class="shimmer shimmer-text"></div></td>
            </tr>
        </tbody>
    </table>
</div>

<%-- User create/edit modals are built dynamically by admin.js via App.modal.confirm() --%>

<%@ include file="/WEB-INF/jsp/layout/_footer.jsp" %>
<script src="/static/js/admin.js"></script>
<script>
$(function() {
    if (typeof App.admin !== 'undefined' && typeof App.admin.users !== 'undefined') {
        App.admin.users.init();
    }
});
</script>
</body>
</html>
