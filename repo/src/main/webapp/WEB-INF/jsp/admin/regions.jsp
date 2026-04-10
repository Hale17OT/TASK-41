<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DispatchOps - Shipping Regions &amp; Rules</title>
    <link rel="stylesheet" href="/static/css/fluent-tokens.css">
    <link rel="stylesheet" href="/static/css/app.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/layout/_header.jsp" %>

<div class="page-title">
    <h1>Shipping Regions &amp; Rules</h1>
</div>

<div class="flex gap-l">
    <!-- Left Column: Region Tree -->
    <div style="width:320px;flex-shrink:0">
        <div class="card">
            <div class="flex justify-between items-center mb-m">
                <h2>Regions</h2>
                <button class="fluent-btn fluent-btn-primary fluent-btn-sm" id="btn-add-region">Add Region</button>
            </div>

            <div id="region-tree">
                <ul style="list-style:none;padding:0;margin:0">
                    <!-- Region tree items rendered by JS -->
                    <li><div class="shimmer shimmer-text mb-s"></div></li>
                    <li><div class="shimmer shimmer-text mb-s" style="width:70%"></div></li>
                    <li><div class="shimmer shimmer-text mb-s" style="width:65%"></div></li>
                </ul>
            </div>
        </div>
    </div>

    <!-- Right Column: Rules Table -->
    <div style="flex:1">
        <div class="card">
            <div class="flex justify-between items-center mb-m">
                <h2>Shipping Rules</h2>
                <button class="fluent-btn fluent-btn-secondary fluent-btn-sm" id="btn-add-rule">Add Rule</button>
            </div>

            <p class="text-caption mb-m" id="rules-region-label">Select a region to view its rules.</p>

            <div class="data-table-wrapper">
                <table class="dataTable w-full" id="rules-table">
                    <thead>
                        <tr>
                            <th>State</th>
                            <th>ZIP Range</th>
                            <th>Max Weight</th>
                            <th>Max Order</th>
                            <th>Allow/Block</th>
                            <th>Priority</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody id="rules-body">
                        <tr>
                            <td colspan="7">
                                <div class="empty-state" style="padding:var(--fluent-spacing-l)">
                                    <div class="empty-icon">&#127758;</div>
                                    <div class="empty-title">No region selected</div>
                                    <p class="text-secondary">Select a region from the tree to manage its shipping rules.</p>
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<!-- Add/Edit Region Modal -->
<div class="modal-overlay" id="region-modal">
    <div class="modal-dialog">
        <div class="modal-header">
            <h3 id="region-modal-title">Add Region</h3>
            <button class="modal-close" data-dismiss="modal">&times;</button>
        </div>
        <div class="modal-body">
            <form id="region-form">
                <input type="hidden" id="region-id" name="id">
                <div class="form-group">
                    <label class="form-label" for="region-name">Region Name</label>
                    <input type="text" class="form-input" id="region-name" name="name" required>
                </div>
                <div class="form-group">
                    <label class="form-label" for="region-parent">Parent Region</label>
                    <select class="form-select" id="region-parent" name="parentId">
                        <option value="">None (Top Level)</option>
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label" for="region-code">Region Code</label>
                    <input type="text" class="form-input" id="region-code" name="code" placeholder="e.g. US-CA">
                </div>
            </form>
        </div>
        <div class="modal-footer">
            <button class="fluent-btn fluent-btn-secondary" data-dismiss="modal">Cancel</button>
            <button class="fluent-btn fluent-btn-primary" id="btn-submit-region">Save</button>
        </div>
    </div>
</div>

<!-- Add/Edit Rule Modal -->
<div class="modal-overlay" id="rule-modal">
    <div class="modal-dialog">
        <div class="modal-header">
            <h3 id="rule-modal-title">Add Rule</h3>
            <button class="modal-close" data-dismiss="modal">&times;</button>
        </div>
        <div class="modal-body">
            <form id="rule-form">
                <input type="hidden" id="rule-id" name="id">
                <div class="form-group">
                    <label class="form-label" for="rule-state">State Code</label>
                    <input type="text" class="form-input" id="rule-state" name="stateCode" placeholder="e.g. CA" maxlength="2">
                </div>
                <div class="flex gap-s">
                    <div class="form-group" style="flex:1">
                        <label class="form-label" for="rule-zip-start">ZIP Range Start</label>
                        <input type="text" class="form-input" id="rule-zip-start" name="zipRangeStart" placeholder="e.g. 90000">
                    </div>
                    <div class="form-group" style="flex:1">
                        <label class="form-label" for="rule-zip-end">ZIP Range End</label>
                        <input type="text" class="form-input" id="rule-zip-end" name="zipRangeEnd" placeholder="e.g. 96199">
                    </div>
                </div>
                <div class="flex gap-s">
                    <div class="form-group" style="flex:1">
                        <label class="form-label" for="rule-max-weight">Max Weight (lbs)</label>
                        <input type="number" class="form-input" id="rule-max-weight" name="maxWeightLbs" step="0.01" placeholder="e.g. 50">
                    </div>
                    <div class="form-group" style="flex:1">
                        <label class="form-label" for="rule-max-order">Max Order ($)</label>
                        <input type="number" class="form-input" id="rule-max-order" name="maxOrderAmount" step="0.01" placeholder="e.g. 500">
                    </div>
                </div>
                <div class="flex gap-s">
                    <div class="form-group" style="flex:1">
                        <label class="form-label" for="rule-allowed">Action</label>
                        <select class="form-select" id="rule-allowed" name="allowed">
                            <option value="true">Allow</option>
                            <option value="false">Block</option>
                        </select>
                    </div>
                    <div class="form-group" style="flex:1">
                        <label class="form-label" for="rule-priority">Priority</label>
                        <input type="number" class="form-input" id="rule-priority" name="priority" min="1" value="10">
                    </div>
                </div>
            </form>
        </div>
        <div class="modal-footer">
            <button class="fluent-btn fluent-btn-secondary" data-dismiss="modal">Cancel</button>
            <button class="fluent-btn fluent-btn-primary" id="btn-submit-rule">Save</button>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/layout/_footer.jsp" %>
<script src="/static/js/admin.js"></script>
<script>
$(function() {
    if (typeof App.admin !== 'undefined' && typeof App.admin.regions !== 'undefined') {
        App.admin.regions.init();
    }
});
</script>
</body>
</html>
