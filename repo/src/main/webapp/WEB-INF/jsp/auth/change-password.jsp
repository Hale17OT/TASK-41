<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DispatchOps - Change Password</title>
    <link rel="stylesheet" href="/static/css/fluent-tokens.css">
    <link rel="stylesheet" href="/static/css/app.css">
    <style>
        .change-pw-container { max-width: 420px; margin: 80px auto; }
        .change-pw-card { padding: var(--fluent-spacing-xl); }
        .pw-requirements { font-size: var(--fluent-font-size-caption); color: var(--fluent-color-neutral-fg-2); margin-top: var(--fluent-spacing-xs); }
    </style>
</head>
<body style="background: var(--fluent-color-neutral-bg-canvas);">
<div class="change-pw-container">
    <div class="card change-pw-card">
        <h2 style="margin-bottom: var(--fluent-spacing-m);">Change Your Password</h2>
        <p class="text-secondary" style="margin-bottom: var(--fluent-spacing-l);">
            Your password must be changed before you can continue.
        </p>

        <div class="status-badge status-danger hidden" id="pw-error" style="margin-bottom: var(--fluent-spacing-m); padding: var(--fluent-spacing-s); width: 100%;"></div>
        <div class="status-badge status-success hidden" id="pw-success" style="margin-bottom: var(--fluent-spacing-m); padding: var(--fluent-spacing-s); width: 100%;"></div>

        <form id="change-pw-form">
            <div class="form-group">
                <label class="form-label" for="new-password">New Password</label>
                <input type="password" class="form-input" id="new-password" name="newPassword" required minlength="8" autocomplete="new-password">
                <p class="pw-requirements">Minimum 8 characters.</p>
            </div>

            <div class="form-group">
                <label class="form-label" for="confirm-password">Confirm Password</label>
                <input type="password" class="form-input" id="confirm-password" name="confirmPassword" required minlength="8" autocomplete="new-password">
            </div>

            <button type="submit" class="fluent-btn fluent-btn-primary w-full" id="pw-submit-btn">Change Password</button>
        </form>
    </div>
</div>

<meta name="_csrf" content="<%= session != null && session.getAttribute("_csrfToken") != null ? session.getAttribute("_csrfToken") : "" %>">
<script src="/static/js/vendor/jquery-3.7.1.min.js"></script>
<script>
$(function() {
    var userId = '${userId}';
    var csrfToken = $('meta[name="_csrf"]').attr('content') || '';

    $('#change-pw-form').on('submit', function(e) {
        e.preventDefault();
        var newPw = $('#new-password').val();
        var confirmPw = $('#confirm-password').val();
        $('#pw-error').addClass('hidden');
        $('#pw-success').addClass('hidden');

        if (newPw !== confirmPw) {
            $('#pw-error').text('Passwords do not match.').removeClass('hidden');
            return;
        }
        if (newPw.length < 8) {
            $('#pw-error').text('Password must be at least 8 characters.').removeClass('hidden');
            return;
        }

        $('#pw-submit-btn').prop('disabled', true).text('Changing...');

        $.ajax({
            url: '/api/users/' + userId + '/password',
            method: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify({ newPassword: newPw }),
            dataType: 'json',
            beforeSend: function(xhr) {
                if (csrfToken) xhr.setRequestHeader('X-CSRF-TOKEN', csrfToken);
            }
        }).done(function(resp) {
            if (resp.code === 200) {
                $('#pw-success').text('Password changed successfully. Redirecting...').removeClass('hidden');
                setTimeout(function() { window.location.href = '/dashboard'; }, 1500);
            } else {
                $('#pw-error').text(resp.message || 'Failed to change password.').removeClass('hidden');
                $('#pw-submit-btn').prop('disabled', false).text('Change Password');
            }
        }).fail(function(xhr) {
            var msg = 'Failed to change password.';
            if (xhr.responseJSON && xhr.responseJSON.message) msg = xhr.responseJSON.message;
            $('#pw-error').text(msg).removeClass('hidden');
            $('#pw-submit-btn').prop('disabled', false).text('Change Password');
        });
    });
});
</script>
</body>
</html>
