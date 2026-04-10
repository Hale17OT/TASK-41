/**
 * DispatchOps -- Toast Notification System
 */
var App = window.App || {};

App.toast = (function () {
    'use strict';

    var $container;
    var AUTO_DISMISS_MS = 5000;

    function ensureContainer() {
        if (!$container || !$container.length) {
            $container = $('<div class="toast-container" id="toast-container"></div>').appendTo('body');
        }
    }

    function show(message, type) {
        ensureContainer();
        var $toast = $('<div class="toast toast-' + type + '">' +
            '<span class="toast-msg">' + App.util.escapeHtml(message) + '</span>' +
            '<button class="toast-close" style="background:none;border:none;cursor:pointer;margin-left:auto;font-size:16px;">&times;</button>' +
            '</div>');

        $container.append($toast);

        $toast.find('.toast-close').on('click', function () {
            $toast.fadeOut(200, function () { $toast.remove(); });
        });

        setTimeout(function () {
            $toast.fadeOut(300, function () { $toast.remove(); });
        }, AUTO_DISMISS_MS);
    }

    return {
        success: function (msg) { show(msg, 'success'); },
        error: function (msg) { show(msg, 'error'); },
        warning: function (msg) { show(msg, 'warning'); },
        info: function (msg) { show(msg, 'info'); }
    };
})();
