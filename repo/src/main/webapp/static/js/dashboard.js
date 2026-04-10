/**
 * DispatchOps -- Dashboard Module
 */
var App = window.App || {};

App.dashboard = (function () {
    'use strict';

    var REFRESH_INTERVAL = 120000; // 2 minutes
    var refreshTimer = null;

    function loadMetrics() {
        App.api.get('/api/dashboard/metrics').done(function (resp) {
            if (resp && resp.data) {
                renderMetrics(resp.data);
            }
        }).fail(function () {
            $('#metric-cards').html('<div class="card"><p class="text-secondary">Unable to load metrics.</p></div>');
        });
    }

    function renderMetrics(data) {
        var user = App.auth.getCurrentUser();
        var html = '';

        if (data.activeJobs != null) {
            html += '<div class="card metric-card"><div class="metric-value">' + data.activeJobs + '</div><div class="metric-label">Active Jobs</div></div>';
        }
        if (data.pendingTasks != null) {
            html += '<div class="card metric-card"><div class="metric-value">' + data.pendingTasks + '</div><div class="metric-label">Pending Tasks</div></div>';
        }
        if (data.exceptionsToday != null) {
            html += '<div class="card metric-card" style="border-left:4px solid var(--fluent-color-status-danger)"><div class="metric-value text-danger">' + data.exceptionsToday + '</div><div class="metric-label">Exceptions Today</div></div>';
        }
        if (data.pendingSettlements != null) {
            html += '<div class="card metric-card"><div class="metric-value">' + data.pendingSettlements + '</div><div class="metric-label">Pending Settlements</div></div>';
        }
        if (data.todayCollections != null) {
            html += '<div class="card metric-card"><div class="metric-value">' + App.util.formatCurrency(data.todayCollections) + '</div><div class="metric-label">Today\'s Collections</div></div>';
        }
        if (data.creditLevel) {
            html += '<div class="card metric-card"><div class="credit-badge credit-' + data.creditLevel.toLowerCase() + '">' + data.creditLevel + '</div><div class="metric-label">Credit Level</div></div>';
        }

        if (!html) {
            html = '<div class="card metric-card"><div class="metric-value">--</div><div class="metric-label">No data</div></div>';
        }

        $('#metric-cards').html(html);
    }

    function loadActivity() {
        App.api.get('/api/dashboard/activity').done(function (resp) {
            if (resp && resp.data && resp.data.length > 0) {
                var html = '';
                resp.data.forEach(function (item) {
                    html += '<div class="flex items-center gap-s mb-s" style="padding:var(--fluent-spacing-xs) 0;border-bottom:1px solid var(--fluent-color-neutral-stroke-2)">' +
                        '<span class="text-caption" style="min-width:120px">' + App.util.formatDateTime(item.timestamp) + '</span>' +
                        '<span>' + App.util.escapeHtml(item.message) + '</span>' +
                        '</div>';
                });
                $('#activity-feed').html(html);
            } else {
                $('#activity-feed').html('<p class="text-secondary">No recent activity.</p>');
            }
        });
    }

    function init() {
        loadMetrics();
        loadActivity();
        refreshTimer = setInterval(function () {
            loadMetrics();
            loadActivity();
        }, REFRESH_INTERVAL);
    }

    function destroy() {
        if (refreshTimer) clearInterval(refreshTimer);
    }

    return { init: init, destroy: destroy };
})();
