<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DispatchOps - Credibility &amp; Performance</title>
    <link rel="stylesheet" href="/static/css/fluent-tokens.css">
    <link rel="stylesheet" href="/static/css/app.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/layout/_header.jsp" %>

<div class="page-title">
    <h1>Credibility &amp; Performance</h1>
</div>

<div class="flex gap-l">
    <!-- Left Column: Rating & Credit -->
    <div style="flex:1">
        <!-- Rating Submission Form -->
        <div class="card mb-m">
            <h2 class="mb-m">Submit Rating</h2>
            <form id="rating-form">
                <div class="form-group">
                    <label class="form-label" for="rating-courier">Courier</label>
                    <select class="form-select" id="rating-courier" name="courierId">
                        <option value="">Select courier</option>
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label" for="rating-order">Order</label>
                    <select class="form-select" id="rating-order" name="orderId">
                        <option value="">Select order</option>
                    </select>
                </div>

                <div class="form-group">
                    <label class="form-label">Timeliness</label>
                    <div class="star-rating" data-field="timeliness">
                        <span class="star" data-value="1">&#9733;</span>
                        <span class="star" data-value="2">&#9733;</span>
                        <span class="star" data-value="3">&#9733;</span>
                        <span class="star" data-value="4">&#9733;</span>
                        <span class="star" data-value="5">&#9733;</span>
                    </div>
                    <input type="hidden" name="timeliness" id="rating-timeliness" value="0">
                </div>

                <div class="form-group">
                    <label class="form-label">Attitude</label>
                    <div class="star-rating" data-field="attitude">
                        <span class="star" data-value="1">&#9733;</span>
                        <span class="star" data-value="2">&#9733;</span>
                        <span class="star" data-value="3">&#9733;</span>
                        <span class="star" data-value="4">&#9733;</span>
                        <span class="star" data-value="5">&#9733;</span>
                    </div>
                    <input type="hidden" name="attitude" id="rating-attitude" value="0">
                </div>

                <div class="form-group">
                    <label class="form-label">Accuracy</label>
                    <div class="star-rating" data-field="accuracy">
                        <span class="star" data-value="1">&#9733;</span>
                        <span class="star" data-value="2">&#9733;</span>
                        <span class="star" data-value="3">&#9733;</span>
                        <span class="star" data-value="4">&#9733;</span>
                        <span class="star" data-value="5">&#9733;</span>
                    </div>
                    <input type="hidden" name="accuracy" id="rating-accuracy" value="0">
                </div>

                <div class="form-group">
                    <label class="form-label" for="rating-comment">Comment</label>
                    <textarea class="form-textarea" id="rating-comment" name="comment" rows="3" placeholder="Optional feedback..."></textarea>
                </div>

                <button type="submit" class="fluent-btn fluent-btn-primary" id="btn-submit-rating">Submit Rating</button>
            </form>
        </div>

        <!-- Credit Level Display -->
        <div class="card">
            <h2 class="mb-m">Credit Level</h2>
            <div class="flex items-center gap-m" id="credit-level-display">
                <div class="credit-badge credit-a" id="credit-badge">A</div>
                <div>
                    <div class="h3" id="credit-level-name">Loading...</div>
                    <p class="text-caption" id="credit-level-desc">Calculating credit level...</p>
                </div>
            </div>
            <div class="mt-m">
                <p class="text-caption">Average Rating: <strong id="avg-rating">--</strong> / 5.0</p>
                <p class="text-caption">Total Ratings: <strong id="total-ratings">--</strong></p>
            </div>
        </div>
    </div>

    <!-- Right Column: Violations & Appeals -->
    <div style="flex:1">
        <!-- Violations Table -->
        <div class="card mb-m">
            <h2 class="mb-m">Violations</h2>
            <div class="data-table-wrapper">
                <table class="dataTable w-full" id="violations-table">
                    <thead>
                        <tr>
                            <th>Date</th>
                            <th>Type</th>
                            <th>Description</th>
                            <th>Penalty</th>
                            <th>Status</th>
                        </tr>
                    </thead>
                    <tbody id="violations-body">
                        <tr>
                            <td colspan="5">
                                <div class="shimmer shimmer-text"></div>
                            </td>
                        </tr>
                        <tr>
                            <td colspan="5">
                                <div class="shimmer shimmer-text"></div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>

        <!-- Appeal Section -->
        <div class="card">
            <h2 class="mb-m">Submit Appeal</h2>
            <form id="appeal-form">
                <div class="form-group">
                    <label class="form-label" for="appeal-violation">Violation</label>
                    <select class="form-select" id="appeal-violation" name="violationId">
                        <option value="">Select violation to appeal</option>
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label" for="appeal-reason">Reason for Appeal</label>
                    <textarea class="form-textarea" id="appeal-reason" name="reason" rows="4" placeholder="Explain why this violation should be reconsidered..." required></textarea>
                </div>
                <button type="submit" class="fluent-btn fluent-btn-secondary" id="btn-submit-appeal">Submit Appeal</button>
            </form>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/layout/_footer.jsp" %>
<script src="/static/js/credibility.js"></script>
<script>
$(function() {
    if (typeof App.credibility !== 'undefined') {
        App.credibility.init();
    }
});
</script>
</body>
</html>
