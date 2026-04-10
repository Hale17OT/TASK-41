/**
 * DispatchOps -- Auth Utilities
 */
var App = window.App || {};

App.auth = (function () {
    'use strict';

    var currentUser = null;
    var heartbeatInterval = null;
    var HEARTBEAT_MS = 5 * 60 * 1000; // 5 minutes

    function init() {
        // Try loading user from server-serialized JSON embedded in page
        var $userData = $('#current-user-data');
        if ($userData.length && $userData.text().trim()) {
            try {
                currentUser = JSON.parse($userData.text());
            } catch (e) {
                // Inline JSON unavailable or malformed — fetch from API
                $.ajax({
                    url: '/api/auth/me',
                    method: 'GET',
                    dataType: 'json',
                    async: false // blocking: needed before page JS runs
                }).done(function (resp) {
                    if (resp && resp.data) {
                        currentUser = resp.data;
                    }
                });
            }
        }

        // Start session heartbeat
        if (currentUser) {
            startHeartbeat();
        }
    }

    function startHeartbeat() {
        heartbeatInterval = setInterval(function () {
            $.ajax({
                url: '/api/auth/heartbeat',
                method: 'GET',
                dataType: 'json',
                timeout: 10000
            }).done(function (resp) {
                if (resp && resp.data && resp.data.csrfToken) {
                    App.api.updateCsrf(resp.data.csrfToken);
                }
            }).fail(function (xhr) {
                if (xhr.status === 401) {
                    clearInterval(heartbeatInterval);
                    window.location.href = '/login?expired=true';
                }
            });
        }, HEARTBEAT_MS);
    }

    function getCurrentUser() {
        return currentUser;
    }

    function hasRole(role) {
        return currentUser && currentUser.role === role;
    }

    function hasAnyRole(roles) {
        return currentUser && roles.indexOf(currentUser.role) !== -1;
    }

    function logout() {
        $.ajax({
            url: '/api/auth/logout',
            method: 'POST',
            dataType: 'json'
        }).always(function () {
            window.location.href = '/login';
        });
    }

    return {
        init: init,
        getCurrentUser: getCurrentUser,
        hasRole: hasRole,
        hasAnyRole: hasAnyRole,
        logout: logout
    };
})();

$(function () {
    App.auth.init();
});
