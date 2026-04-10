<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DispatchOps - Payments</title>
    <link rel="stylesheet" href="/static/css/fluent-tokens.css">
    <link rel="stylesheet" href="/static/css/app.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/layout/_header.jsp" %>

<div class="page-title flex justify-between items-center">
    <h1>Payments</h1>
    <span class="status-badge sync-pending hidden" id="payment-sync-badge"></span>
</div>

<div class="tab-bar" id="payment-tabs">
    <button class="tab-item active" data-tab="record">Record Payment</button>
    <button class="tab-item" data-tab="history">History</button>
    <c:if test="${currentUser.role == 'AUDITOR' || currentUser.role == 'ADMIN'}">
    <button class="tab-item" data-tab="reconciliation">Reconciliation</button>
    </c:if>
</div>

<!-- Record Payment Tab -->
<div class="tab-panel" id="tab-record">
    <div class="card" style="max-width:560px">
        <h2 class="mb-m">Record Payment</h2>
        <form id="payment-form">
            <div class="form-group">
                <label class="form-label">Payment Method</label>
                <div class="flex gap-m mt-s">
                    <label class="flex items-center gap-xs" style="cursor:pointer">
                        <input type="radio" name="method" value="CASH" checked> Cash
                    </label>
                    <label class="flex items-center gap-xs" style="cursor:pointer">
                        <input type="radio" name="method" value="CHECK"> Check
                    </label>
                    <label class="flex items-center gap-xs" style="cursor:pointer">
                        <input type="radio" name="method" value="INTERNAL_BALANCE"> Balance
                    </label>
                </div>
            </div>
            <div class="form-group hidden" id="check-number-group">
                <label class="form-label" for="payment-check">Check Number</label>
                <input type="text" class="form-input" id="payment-check" name="checkNumber"
                       placeholder="Check number">
            </div>
            <div class="form-group">
                <label class="form-label" for="payment-amount">Amount</label>
                <input type="number" class="form-input" id="payment-amount" name="amount"
                       step="0.01" min="0.01" placeholder="0.00" required>
            </div>
            <div class="form-group">
                <label class="form-label" for="payment-order">Job ID</label>
                <input type="number" class="form-input" id="payment-order" name="jobId"
                       placeholder="e.g. 1" min="1" required>
            </div>
            <div class="form-group">
                <label class="form-label" for="payment-notes">Notes</label>
                <textarea class="form-textarea" id="payment-notes" name="notes" rows="3"
                          placeholder="Optional payment notes..."></textarea>
            </div>
            <button type="submit" class="fluent-btn fluent-btn-primary" id="btn-submit-payment">Submit Payment</button>
        </form>
    </div>
</div>

<!-- History Tab -->
<div class="tab-panel hidden" id="tab-history">
    <div class="data-table-wrapper">
        <table class="dataTable w-full" id="payment-history-table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Date</th>
                    <th>Job</th>
                    <th>Amount</th>
                    <th>Method</th>
                    <th>Status</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody id="payment-history-body">
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
</div>

<!-- Reconciliation Tab (AUDITOR/ADMIN only) -->
<c:if test="${currentUser.role == 'AUDITOR' || currentUser.role == 'ADMIN'}">
<div class="tab-panel hidden" id="tab-reconciliation">
    <div class="card mb-m">
        <h2 class="mb-m">Reconciliation</h2>
        <div class="flex items-center gap-m mb-m">
            <div class="form-group" style="margin-bottom:0">
                <label class="form-label" for="recon-date-from">From</label>
                <input type="date" class="form-input" id="recon-date-from" name="dateFrom">
            </div>
            <div class="form-group" style="margin-bottom:0">
                <label class="form-label" for="recon-date-to">To</label>
                <input type="date" class="form-input" id="recon-date-to" name="dateTo">
            </div>
            <button class="fluent-btn fluent-btn-secondary" id="btn-run-recon" style="align-self:flex-end">Run Report</button>
            <button class="fluent-btn fluent-btn-secondary" id="btn-export-csv" style="align-self:flex-end">Export CSV</button>
        </div>
    </div>

    <div class="card-grid" id="recon-stats">
        <div class="card metric-card">
            <span class="metric-label">Total Collected</span>
            <span class="metric-value" id="recon-total-collected">--</span>
        </div>
        <div class="card metric-card">
            <span class="metric-label">Total Settled</span>
            <span class="metric-value" id="recon-total-settled">--</span>
        </div>
        <div class="card metric-card">
            <span class="metric-label">Pending Settlement</span>
            <span class="metric-value" id="recon-pending">--</span>
        </div>
        <div class="card metric-card">
            <span class="metric-label">Discrepancies</span>
            <span class="metric-value text-danger" id="recon-discrepancies">--</span>
        </div>
    </div>
</div>
</c:if>

<%@ include file="/WEB-INF/jsp/layout/_footer.jsp" %>
<script src="/static/js/payments.js"></script>
<script>
$(function() {
    if (typeof App.payments !== 'undefined') {
        App.payments.init();
    }
});
</script>
</body>
</html>
