<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DispatchOps - Login</title>
    <link rel="stylesheet" href="/static/css/fluent-tokens.css">
    <link rel="stylesheet" href="/static/css/app.css">
</head>
<body>
    <div class="login-page">
        <div class="card login-card">
            <div class="login-title">
                <h1>DispatchOps</h1>
                <p class="text-secondary">Courier Operations & Settlement</p>
            </div>

            <div class="login-error hidden" id="login-error"></div>
            <div class="login-error hidden" id="lockout-message">
                Account is locked. Try again in <span id="lockout-countdown"></span>.
            </div>

            <form id="login-form">
                <div class="form-group">
                    <label class="form-label" for="username">Username</label>
                    <input type="text" class="form-input" id="username" name="username"
                           placeholder="Enter your username" required autofocus>
                </div>
                <div class="form-group">
                    <label class="form-label" for="password">Password</label>
                    <input type="password" class="form-input" id="password" name="password"
                           placeholder="Enter your password" required>
                </div>
                <button type="submit" class="fluent-btn fluent-btn-primary w-full" id="login-btn"
                        style="min-height:40px; margin-top: var(--fluent-spacing-s);">
                    Log In
                </button>
            </form>
        </div>
    </div>

    <div class="toast-container" id="toast-container"></div>

    <script src="/static/js/vendor/jquery-3.7.1.min.js"></script>
    <script src="/static/js/common/util.js"></script>
    <script src="/static/js/common/toast.js"></script>
    <script>
    $(function() {
        var lockoutTimer = null;

        // Check URL for expired session
        if (window.location.search.indexOf('expired=true') !== -1) {
            $('#login-error').text('Your session has expired. Please log in again.').removeClass('hidden');
        }

        $('#login-form').on('submit', function(e) {
            e.preventDefault();
            var username = $('#username').val().trim();
            var password = $('#password').val();

            if (!username || !password) {
                $('#login-error').text('Please enter both username and password.').removeClass('hidden');
                return;
            }

            $('#login-btn').prop('disabled', true).text('Logging in...');
            $('#login-error').addClass('hidden');
            $('#lockout-message').addClass('hidden');

            $.ajax({
                url: '/api/auth/login',
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({ username: username, password: password }),
                dataType: 'json'
            }).done(function(resp) {
                if (resp.code === 200) {
                    if (resp.data && resp.data.mustChangePassword) {
                        window.location.href = '/change-password';
                    } else {
                        window.location.href = '/dashboard';
                    }
                } else {
                    $('#login-error').text(resp.message || 'Login failed.').removeClass('hidden');
                    $('#login-btn').prop('disabled', false).text('Log In');
                }
            }).fail(function(xhr) {
                $('#login-btn').prop('disabled', false).text('Log In');

                if (xhr.status === 423) {
                    // Account locked
                    var resp = xhr.responseJSON;
                    var expiry = resp && resp.data && resp.data.lockoutExpiry;
                    if (expiry) {
                        startLockoutCountdown(new Date(expiry));
                    } else {
                        $('#lockout-message').removeClass('hidden');
                        $('#lockout-countdown').text('15:00');
                    }
                } else if (xhr.status === 401) {
                    var resp = xhr.responseJSON;
                    var msg = resp && resp.message ? resp.message : 'Invalid username or password.';
                    if (resp && resp.data && resp.data.remainingAttempts != null) {
                        msg += ' (' + resp.data.remainingAttempts + ' attempts remaining)';
                    }
                    $('#login-error').text(msg).removeClass('hidden');
                } else {
                    $('#login-error').text('An error occurred. Please try again.').removeClass('hidden');
                }
            });
        });

        function startLockoutCountdown(expiryDate) {
            $('#lockout-message').removeClass('hidden');
            $('#login-error').addClass('hidden');
            $('#login-btn').prop('disabled', true);

            if (lockoutTimer) clearInterval(lockoutTimer);

            lockoutTimer = setInterval(function() {
                var remaining = expiryDate.getTime() - Date.now();
                if (remaining <= 0) {
                    clearInterval(lockoutTimer);
                    $('#lockout-message').addClass('hidden');
                    $('#login-btn').prop('disabled', false).text('Log In');
                    return;
                }
                var min = Math.floor(remaining / 60000);
                var sec = Math.floor((remaining % 60000) / 1000);
                $('#lockout-countdown').text(App.util.padZero(min) + ':' + App.util.padZero(sec));
            }, 1000);
        }
    });
    </script>
</body>
</html>
