<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DispatchOps - Contract Templates</title>
    <link rel="stylesheet" href="/static/css/fluent-tokens.css">
    <link rel="stylesheet" href="/static/css/app.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/layout/_header.jsp" %>

<div class="page-title flex justify-between items-center">
    <h1>Contract Templates</h1>
    <button class="fluent-btn fluent-btn-primary" id="btn-create-template">Create Template</button>
</div>

<div class="data-table-wrapper">
    <table class="dataTable w-full" id="contracts-table">
        <thead>
            <tr>
                <th>Template Name</th>
                <th>Created</th>
                <th>Status</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody id="contracts-body">
            <tr>
                <td colspan="4"><div class="shimmer shimmer-text"></div></td>
            </tr>
            <tr>
                <td colspan="4"><div class="shimmer shimmer-text"></div></td>
            </tr>
            <tr>
                <td colspan="4"><div class="shimmer shimmer-text"></div></td>
            </tr>
        </tbody>
    </table>
</div>

<!-- Create/Edit Template Modal -->
<div class="modal-overlay" id="template-modal">
    <div class="modal-dialog" style="max-width:640px">
        <div class="modal-header">
            <h3 id="template-modal-title">Create Template</h3>
            <button class="modal-close" data-dismiss="modal">&times;</button>
        </div>
        <div class="modal-body">
            <form id="template-form">
                <div class="form-group">
                    <label class="form-label" for="template-name">Template Name</label>
                    <input type="text" class="form-input" id="template-name" name="name" required>
                </div>
                <div class="form-group">
                    <label class="form-label" for="template-body">Template Body</label>
                    <textarea class="form-textarea" id="template-body" name="body" rows="12" placeholder="Enter contract template with {{placeholders}}..." required></textarea>
                </div>
            </form>
        </div>
        <div class="modal-footer">
            <button class="fluent-btn fluent-btn-secondary" data-dismiss="modal">Cancel</button>
            <button class="fluent-btn fluent-btn-primary" id="btn-submit-template">Save Template</button>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/layout/_footer.jsp" %>
<script src="/static/js/contracts.js"></script>
<script>
$(function() {
    if (typeof App.contracts !== 'undefined') {
        App.contracts.init();
    }
});
</script>
</body>
</html>
