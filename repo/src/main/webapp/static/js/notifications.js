/**
 * DispatchOps -- Notification Center Module
 */
var App = window.App || {};

App.notifications = (function () {
    'use strict';

    function loadInbox() {
        App.shimmer.show('#notification-list', 'list', 5);

        App.api.get('/api/notifications', { page: 0, size: 50 }).done(function (resp) {
            if (resp && resp.data) {
                renderNotifications(resp.data.content || resp.data);
            }
        });
    }

    function renderNotifications(notifications) {
        var $list = $('#notification-list');
        $list.empty();

        if (!notifications || notifications.length === 0) {
            $list.html('<div class="empty-state"><div class="empty-icon">&#128276;</div><div class="empty-title">No notifications</div></div>');
            return;
        }

        notifications.forEach(function (n) {
            var readClass = n.isRead ? 'text-secondary' : '';
            var dotHtml = n.isRead ? '' : '<span style="width:8px;height:8px;border-radius:50%;background:var(--fluent-color-brand-primary);display:inline-block"></span>';

            var html = '<div class="card mb-s notif-item ' + readClass + '" data-id="' + n.id + '" style="cursor:pointer">' +
                '<div class="flex items-center gap-s">' +
                dotHtml +
                '<div style="flex:1">' +
                '<div class="flex justify-between">' +
                '<strong>' + App.util.escapeHtml(n.title) + '</strong>' +
                '<span class="text-caption">' + App.util.formatDateTime(n.createdAt) + '</span>' +
                '</div>' +
                (n.body ? '<p class="text-caption mt-xxs">' + App.util.escapeHtml(App.util.truncate(n.body, 120)) + '</p>' : '') +
                '</div></div></div>';
            $list.append(html);
        });
    }

    function markAsRead(id) {
        App.api.put('/api/notifications/' + id + '/read').done(function () {
            $('[data-id="' + id + '"]').addClass('text-secondary').find('span[style*="brand-primary"]').remove();
        });
    }

    function markAllRead() {
        App.api.put('/api/notifications/read-all').done(function () {
            App.toast.success('All marked as read');
            loadInbox();
        });
    }

    function init() {
        loadInbox();

        $(document).on('click', '.notif-item', function () {
            var id = $(this).data('id');
            markAsRead(id);
        });

        $(document).on('click', '#btn-mark-all-read', markAllRead);
    }

    return { init: init };
})();
