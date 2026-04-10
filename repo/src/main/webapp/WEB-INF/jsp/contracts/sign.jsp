<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DispatchOps - Sign Contract</title>
    <link rel="stylesheet" href="/static/css/fluent-tokens.css">
    <link rel="stylesheet" href="/static/css/app.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/layout/_header.jsp" %>

<div class="page-title">
    <h1>Sign Contract</h1>
</div>

<!-- Step Wizard -->
<div class="card mb-m">
    <div class="flex items-center gap-m" id="step-wizard" style="padding:var(--fluent-spacing-s) 0">
        <div class="flex items-center gap-xs step-indicator active" data-step="1">
            <span style="display:inline-flex;align-items:center;justify-content:center;width:28px;height:28px;border-radius:var(--fluent-radius-circular);background:var(--fluent-color-brand-primary);color:white;font-weight:var(--fluent-font-weight-bold);font-size:var(--fluent-font-size-caption)">1</span>
            <span style="font-weight:var(--fluent-font-weight-semibold)">Review</span>
        </div>
        <div style="flex:1;height:2px;background:var(--fluent-color-neutral-stroke-2)" class="step-connector"></div>
        <div class="flex items-center gap-xs step-indicator" data-step="2">
            <span style="display:inline-flex;align-items:center;justify-content:center;width:28px;height:28px;border-radius:var(--fluent-radius-circular);background:var(--fluent-color-neutral-stroke-1);color:var(--fluent-color-neutral-fg-2);font-weight:var(--fluent-font-weight-bold);font-size:var(--fluent-font-size-caption)">2</span>
            <span class="text-secondary">Confirm</span>
        </div>
        <div style="flex:1;height:2px;background:var(--fluent-color-neutral-stroke-2)" class="step-connector"></div>
        <div class="flex items-center gap-xs step-indicator" data-step="3">
            <span style="display:inline-flex;align-items:center;justify-content:center;width:28px;height:28px;border-radius:var(--fluent-radius-circular);background:var(--fluent-color-neutral-stroke-1);color:var(--fluent-color-neutral-fg-2);font-weight:var(--fluent-font-weight-bold);font-size:var(--fluent-font-size-caption)">3</span>
            <span class="text-secondary">Sign</span>
        </div>
    </div>
</div>

<!-- Step 1: Review -->
<div class="step-panel" id="step-review">
    <div class="card">
        <h2 class="mb-m">Review Contract</h2>
        <div id="contract-review-content" style="padding:var(--fluent-spacing-m);border:1px solid var(--fluent-color-neutral-stroke-2);border-radius:var(--fluent-radius-s);background:white;min-height:400px;line-height:1.8">
            <div class="shimmer shimmer-title"></div>
            <div class="shimmer shimmer-text"></div>
            <div class="shimmer shimmer-text" style="width:90%"></div>
            <div class="shimmer shimmer-text" style="width:85%"></div>
            <div class="shimmer shimmer-text" style="width:70%"></div>
            <div class="shimmer shimmer-text"></div>
            <div class="shimmer shimmer-text" style="width:95%"></div>
        </div>
        <div class="mt-m flex justify-between">
            <button class="fluent-btn fluent-btn-secondary" onclick="window.location.href='/contracts'">Cancel</button>
            <button class="fluent-btn fluent-btn-primary" id="btn-step-to-confirm">Continue to Confirm</button>
        </div>
    </div>
</div>

<!-- Step 2: Confirm -->
<div class="step-panel hidden" id="step-confirm">
    <div class="card">
        <h2 class="mb-m">Confirm Details</h2>
        <div class="mb-m" style="padding:var(--fluent-spacing-m);background:var(--fluent-color-status-info-bg);border-radius:var(--fluent-radius-s)">
            <p>By proceeding, you acknowledge that you have read and understood the full contract terms above.
               Your electronic signature will be legally binding.</p>
        </div>
        <div class="form-group">
            <label class="flex items-center gap-s" style="cursor:pointer">
                <input type="checkbox" id="confirm-read" required>
                <span>I have read and understand the contract terms</span>
            </label>
        </div>
        <div class="form-group">
            <label class="flex items-center gap-s" style="cursor:pointer">
                <input type="checkbox" id="confirm-agree" required>
                <span>I agree to be bound by the terms of this contract</span>
            </label>
        </div>
        <div class="form-group">
            <label class="form-label" for="confirm-full-name">Type your full legal name</label>
            <input type="text" class="form-input" id="confirm-full-name" placeholder="e.g. John A. Doe" required>
        </div>
        <div class="mt-m flex justify-between">
            <button class="fluent-btn fluent-btn-secondary" id="btn-step-back-review">Back to Review</button>
            <button class="fluent-btn fluent-btn-primary" id="btn-step-to-sign" disabled>Proceed to Sign</button>
        </div>
    </div>
</div>

<!-- Step 3: Sign -->
<div class="step-panel hidden" id="step-sign">
    <div class="card">
        <h2 class="mb-m">Draw Your Signature</h2>
        <p class="text-caption mb-m">Use your mouse or touchscreen to draw your signature in the area below.</p>
        <div style="border:2px dashed var(--fluent-color-neutral-stroke-1);border-radius:var(--fluent-radius-m);background:white;padding:var(--fluent-spacing-xs)">
            <canvas id="signature-canvas" width="700" height="200" style="width:100%;cursor:crosshair;display:block"></canvas>
        </div>
        <div class="mt-m flex justify-between">
            <div class="flex gap-s">
                <button class="fluent-btn fluent-btn-secondary" id="btn-step-back-confirm">Back</button>
                <button class="fluent-btn fluent-btn-secondary" id="btn-clear-signature">Clear</button>
            </div>
            <button class="fluent-btn fluent-btn-primary" id="btn-submit-signature" data-contract-id="${param.id}">Submit Signature</button>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/layout/_footer.jsp" %>
<script src="/static/js/contracts.js"></script>
<script>
$(function() {
    if (typeof App.contracts !== 'undefined' && typeof App.contracts.sign !== 'undefined') {
        App.contracts.sign.init();
    }
});
</script>
</body>
</html>
