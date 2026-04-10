<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DispatchOps - Contract Preview</title>
    <link rel="stylesheet" href="/static/css/fluent-tokens.css">
    <link rel="stylesheet" href="/static/css/app.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/layout/_header.jsp" %>

<div class="page-title flex justify-between items-center">
    <h1>Contract Preview</h1>
    <div class="flex gap-s">
        <button class="fluent-btn fluent-btn-secondary" id="btn-back-contracts" onclick="window.location.href='/contracts'">Back to Templates</button>
        <button class="fluent-btn fluent-btn-primary" id="btn-send-for-signing">Send for Signing</button>
    </div>
</div>

<div class="flex gap-l" style="min-height:600px">
    <!-- Left Panel: Rendered Preview -->
    <div style="flex:1;min-width:0">
        <div class="card" style="min-height:100%">
            <h2 class="mb-m">Rendered Document</h2>
            <div id="contract-preview-content" style="padding:var(--fluent-spacing-m);border:1px solid var(--fluent-color-neutral-stroke-2);border-radius:var(--fluent-radius-s);background:white;min-height:480px;line-height:1.8;font-size:var(--fluent-font-size-body)">
                <div class="shimmer shimmer-title"></div>
                <div class="shimmer shimmer-text"></div>
                <div class="shimmer shimmer-text" style="width:90%"></div>
                <div class="shimmer shimmer-text" style="width:85%"></div>
                <div class="shimmer shimmer-text" style="width:70%"></div>
                <div class="shimmer shimmer-text"></div>
                <div class="shimmer shimmer-text" style="width:95%"></div>
                <div class="shimmer shimmer-text" style="width:60%"></div>
            </div>
        </div>
    </div>

    <!-- Right Panel: Placeholder Values Form -->
    <div style="width:360px;flex-shrink:0">
        <div class="card" style="min-height:100%">
            <h2 class="mb-m">Placeholder Values</h2>
            <p class="text-caption mb-m">Fill in the values for each template placeholder. The preview will update in real time.</p>

            <form id="placeholder-form">
                <div id="placeholder-fields">
                    <!-- Placeholder fields rendered by JS based on template -->
                    <div class="shimmer shimmer-text mb-m"></div>
                    <div class="shimmer shimmer-text mb-m" style="width:70%"></div>
                    <div class="shimmer shimmer-text mb-m"></div>
                </div>

                <div class="mt-l">
                    <button type="button" class="fluent-btn fluent-btn-secondary w-full" id="btn-refresh-preview">Refresh Preview</button>
                </div>
            </form>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/layout/_footer.jsp" %>
<script src="/static/js/contracts.js"></script>
<script>
$(function() {
    if (typeof App.contracts !== 'undefined' && typeof App.contracts.preview !== 'undefined') {
        App.contracts.preview.init();
    }
});
</script>
</body>
</html>
