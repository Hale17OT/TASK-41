<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DispatchOps - Customer Portal</title>
    <link rel="stylesheet" href="/static/css/fluent-tokens.css">
    <link rel="stylesheet" href="/static/css/app.css">
</head>
<body>
    <div class="login-page">
        <div class="card login-card" style="max-width: 520px;">
            <div class="login-title">
                <h1>DispatchOps</h1>
                <p class="text-secondary">Customer Rating &amp; Appeal Portal</p>
            </div>

            <div class="login-error hidden" id="portal-error"></div>
            <div class="hidden" id="portal-success" style="padding:8px;border-radius:4px;background:var(--fluent-color-status-success-bg);color:var(--fluent-color-status-success-fg);margin-bottom:12px;"></div>

            <!-- Step 1: Lookup -->
            <div id="step-lookup">
                <form id="lookup-form">
                    <div class="form-group">
                        <label class="form-label" for="trackingNumber">Tracking Number</label>
                        <input type="text" class="form-input" id="trackingNumber" name="trackingNumber"
                               placeholder="e.g. DO-1234567890-5678" required autofocus>
                    </div>
                    <div class="form-group">
                        <label class="form-label" for="customerToken">Customer Token</label>
                        <input type="text" class="form-input" id="customerToken" name="customerToken"
                               placeholder="Token from your delivery receipt" required>
                    </div>
                    <button type="submit" class="fluent-btn fluent-btn-primary w-full" id="lookup-btn"
                            style="min-height:40px; margin-top: var(--fluent-spacing-s);">
                        Look Up Delivery
                    </button>
                </form>
            </div>

            <!-- Step 2: Rating / Appeal form (shown after successful lookup) -->
            <div id="step-action" class="hidden">
                <div style="margin-bottom:16px;">
                    <strong>Delivery:</strong> <span id="info-tracking"></span><br>
                    <strong>Receiver:</strong> <span id="info-receiver"></span><br>
                    <strong>Status:</strong> <span id="info-status"></span>
                </div>

                <div id="rating-section">
                    <h3 style="margin-bottom:8px;">Rate this Delivery</h3>
                    <form id="rating-form">
                        <div class="form-group">
                            <label class="form-label">Timeliness (1-5)</label>
                            <input type="number" class="form-input" id="timeliness" min="1" max="5" required>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Attitude (1-5)</label>
                            <input type="number" class="form-input" id="attitude" min="1" max="5" required>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Accuracy (1-5)</label>
                            <input type="number" class="form-input" id="accuracy" min="1" max="5" required>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Comment (optional)</label>
                            <textarea class="form-input" id="comment" rows="3"></textarea>
                        </div>
                        <button type="submit" class="fluent-btn fluent-btn-primary w-full" id="rate-btn"
                                style="min-height:40px;">Submit Rating</button>
                    </form>
                </div>

                <div id="appeal-section" class="hidden" style="margin-top:16px; border-top:1px solid var(--fluent-color-neutral-stroke1); padding-top:16px;">
                    <h3 style="margin-bottom:8px;">File an Appeal</h3>
                    <p class="text-secondary" style="margin-bottom:12px; font-size:0.9em;">
                        Appeals must be filed within 48 hours of the rating or violation.
                    </p>
                    <form id="appeal-form">
                        <div class="form-group">
                            <label class="form-label" for="appeal-rating">Rating to Appeal</label>
                            <select class="form-input" id="appeal-rating" required>
                                <option value="">Select a rating...</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label class="form-label" for="appeal-reason">Reason for Appeal</label>
                            <textarea class="form-input" id="appeal-reason" rows="3"
                                      placeholder="Describe why you are appealing this delivery rating..." required></textarea>
                        </div>
                        <button type="submit" class="fluent-btn fluent-btn-primary w-full" id="appeal-btn"
                                style="min-height:40px;">Submit Appeal</button>
                    </form>
                </div>

                <div style="margin-top:16px;">
                    <button class="fluent-btn fluent-btn-secondary w-full" id="back-btn"
                            style="min-height:36px;">Back to Lookup</button>
                </div>
            </div>
        </div>
    </div>

    <div class="toast-container" id="toast-container"></div>

    <script src="/static/js/vendor/jquery-3.7.1.min.js"></script>
    <script src="/static/js/common/util.js"></script>
    <script src="/static/js/common/toast.js"></script>
    <script>
    $(function() {
        var currentTracking, currentToken, currentReceiver;

        $('#lookup-form').on('submit', function(e) {
            e.preventDefault();
            $('#portal-error').addClass('hidden');
            currentTracking = $('#trackingNumber').val().trim();
            currentToken = $('#customerToken').val().trim();

            $('#lookup-btn').prop('disabled', true).text('Looking up...');

            $.ajax({
                url: '/api/credibility/customer/lookup',
                method: 'GET',
                data: { trackingNumber: currentTracking, customerToken: currentToken }
            }).done(function(resp) {
                if (resp.code === 200 && resp.data) {
                    currentReceiver = resp.data.receiverName;
                    $('#info-tracking').text(resp.data.trackingNumber);
                    $('#info-receiver').text(resp.data.receiverName);
                    $('#info-status').text(resp.data.status);
                    $('#step-lookup').addClass('hidden');
                    $('#step-action').removeClass('hidden');
                    if (!resp.data.canRate) {
                        $('#rating-section').html('<p class="text-secondary">This delivery is not yet eligible for rating.</p>');
                    }
                    if (resp.data.canAppeal && resp.data.appealableRatings && resp.data.appealableRatings.length > 0) {
                        var $sel = $('#appeal-rating').empty().append('<option value="">Select a rating...</option>');
                        $.each(resp.data.appealableRatings, function(i, r) {
                            var label = 'Rating #' + r.id + ' — T:' + r.timeliness + ' A:' + r.attitude + ' Ac:' + r.accuracy + ' (' + r.createdAt + ')';
                            $sel.append($('<option>').val(r.id).text(label));
                        });
                        $('#appeal-section').removeClass('hidden');
                    } else {
                        $('#appeal-section').addClass('hidden');
                    }
                } else {
                    $('#portal-error').text(resp.message || 'Lookup failed.').removeClass('hidden');
                }
                $('#lookup-btn').prop('disabled', false).text('Look Up Delivery');
            }).fail(function(xhr) {
                var msg = xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : 'Unable to find delivery.';
                $('#portal-error').text(msg).removeClass('hidden');
                $('#lookup-btn').prop('disabled', false).text('Look Up Delivery');
            });
        });

        $('#rating-form').on('submit', function(e) {
            e.preventDefault();
            $('#portal-error').addClass('hidden');
            $('#rate-btn').prop('disabled', true).text('Submitting...');

            $.ajax({
                url: '/api/credibility/ratings/customer',
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({
                    trackingNumber: currentTracking,
                    receiverName: currentReceiver,
                    customerToken: currentToken,
                    timeliness: parseInt($('#timeliness').val()),
                    attitude: parseInt($('#attitude').val()),
                    accuracy: parseInt($('#accuracy').val()),
                    comment: $('#comment').val()
                })
            }).done(function(resp) {
                if (resp.code === 200 || resp.code === 201) {
                    $('#portal-success').text('Thank you! Your rating has been submitted.').removeClass('hidden');
                    $('#rating-section').addClass('hidden');
                } else {
                    $('#portal-error').text(resp.message || 'Rating submission failed.').removeClass('hidden');
                }
                $('#rate-btn').prop('disabled', false).text('Submit Rating');
            }).fail(function(xhr) {
                var msg = xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : 'Rating submission failed.';
                $('#portal-error').text(msg).removeClass('hidden');
                $('#rate-btn').prop('disabled', false).text('Submit Rating');
            });
        });

        $('#appeal-form').on('submit', function(e) {
            e.preventDefault();
            $('#portal-error').addClass('hidden');
            $('#appeal-btn').prop('disabled', true).text('Submitting...');

            $.ajax({
                url: '/api/credibility/appeals/customer',
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({
                    trackingNumber: currentTracking,
                    receiverName: currentReceiver,
                    customerToken: currentToken,
                    reason: $('#appeal-reason').val(),
                    ratingId: parseInt($('#appeal-rating').val()) || null
                })
            }).done(function(resp) {
                if (resp.code === 200 || resp.code === 201) {
                    $('#portal-success').text('Your appeal has been submitted and will be reviewed.').removeClass('hidden');
                    $('#appeal-section').addClass('hidden');
                } else {
                    $('#portal-error').text(resp.message || 'Appeal submission failed.').removeClass('hidden');
                }
                $('#appeal-btn').prop('disabled', false).text('Submit Appeal');
            }).fail(function(xhr) {
                var msg = xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : 'Appeal submission failed.';
                $('#portal-error').text(msg).removeClass('hidden');
                $('#appeal-btn').prop('disabled', false).text('Submit Appeal');
            });
        });

        $('#back-btn').on('click', function() {
            $('#step-action').addClass('hidden');
            $('#step-lookup').removeClass('hidden');
            $('#portal-error').addClass('hidden');
            $('#portal-success').addClass('hidden');
            $('#appeal-section').addClass('hidden');
            $('#appeal-reason').val('');
        });
    });
    </script>
</body>
</html>
