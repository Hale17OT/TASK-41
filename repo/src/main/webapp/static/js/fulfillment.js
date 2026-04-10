/**
 * DispatchOps -- Fulfillment Board Module
 * Kanban board with idle warnings, polling, pick/sort list generation.
 */
var App = window.App || {};

App.fulfillment = (function () {
    'use strict';

    var POLL_INTERVAL = 60000; // 60 seconds
    var pollTimer = null;

    function loadJobs() {
        App.api.get('/api/jobs', { size: 200 }).done(function (resp) {
            if (resp && resp.data) {
                var jobs = resp.data.content || resp.data;
                renderBoard(jobs);
            }
        });
    }

    function renderBoard(jobs) {
        var columns = {
            CREATED: [], PICKED: [], IN_TRANSIT: [], DELIVERED: [], EXCEPTION: [], MANUAL_VALIDATION: []
        };

        jobs.forEach(function (job) {
            var status = job.status || 'CREATED';
            if (columns[status]) columns[status].push(job);
        });

        ['created', 'picked', 'in-transit', 'delivered', 'exception'].forEach(function (colId) {
            var status = colId.replace(/-/g, '_').toUpperCase();
            var $col = $('#col-' + colId);
            if (!$col.length) return;

            var items = columns[status] || [];
            $col.find('.count-badge').text(items.length);
            var $list = $col.find('.kanban-list');
            $list.empty();

            if (items.length === 0) {
                $list.html('<div class="empty-state" style="padding:var(--fluent-spacing-l)"><p class="text-secondary">No jobs</p></div>');
                return;
            }

            items.forEach(function (job) {
                var idleMin = App.util.elapsedMinutes(job.lastEventAt || job.createdAt);
                var level = App.util.idleLevel(idleMin);
                var warningClass = level ? 'idle-warning-' + level : '';

                var actions = '';
                if (job.status === 'CREATED' && !job.courierId) {
                    actions = '<button class="fluent-btn fluent-btn-sm fluent-btn-primary btn-assign" data-id="' + job.id + '" style="margin-top:4px">Assign Courier</button>';
                }
                if (job.status === 'CREATED' || job.status === 'PICKED' || job.status === 'IN_TRANSIT') {
                    var nextStatus = job.status === 'CREATED' ? 'PICKED' : job.status === 'PICKED' ? 'IN_TRANSIT' : 'DELIVERED';
                    actions += ' <button class="fluent-btn fluent-btn-sm fluent-btn-secondary btn-advance" data-id="' + job.id + '" data-status="' + nextStatus + '" data-version="' + (job.version || 1) + '" style="margin-top:4px">' + nextStatus + '</button>';
                }

                var html = '<div class="job-card ' + warningClass + '" data-id="' + job.id + '" data-last-update="' + (job.lastEventAt || job.createdAt) + '">' +
                    '<div class="job-tracking">#' + App.util.escapeHtml(job.trackingNumber) + '</div>' +
                    '<div class="job-meta">' +
                    (job.courierId ? 'Courier #' + job.courierId : '<em>Unassigned</em>') +
                    '</div>' +
                    '<div class="job-meta">' + App.util.truncate(job.receiverAddress || '', 40) + '</div>' +
                    '<div class="job-meta">' + App.util.timeAgo(job.lastEventAt || job.createdAt) + '</div>' +
                    (actions ? '<div>' + actions + '</div>' : '') +
                    '</div>';
                $list.append(html);
            });
        });

        applyIdleWarnings();
    }

    function applyIdleWarnings() {
        var now = Date.now();
        $('.job-card[data-last-update]').each(function () {
            var $card = $(this);
            var lastUpdate = new Date($card.data('last-update')).getTime();
            var elapsedMin = Math.floor((now - lastUpdate) / 60000);

            $card.removeClass('idle-warning-yellow idle-warning-orange idle-warning-red');
            var level = App.util.idleLevel(elapsedMin);
            if (level) $card.addClass('idle-warning-' + level);
        });
    }

    function showCreateJobModal() {
        var html =
            '<div class="form-group"><label class="form-label">Receiver Name</label><input class="form-input" id="job-receiver-name"></div>' +
            '<div class="form-group"><label class="form-label">Receiver Address</label><input class="form-input" id="job-receiver-address"></div>' +
            '<div class="flex gap-m">' +
            '  <div class="form-group" style="flex:1"><label class="form-label">State</label><input class="form-input" id="job-receiver-state" maxlength="2"></div>' +
            '  <div class="form-group" style="flex:1"><label class="form-label">ZIP</label><input class="form-input" id="job-receiver-zip"></div>' +
            '</div>' +
            '<div class="flex gap-m">' +
            '  <div class="form-group" style="flex:1"><label class="form-label">Weight (lbs)</label><input class="form-input" id="job-weight" type="number" step="0.01"></div>' +
            '  <div class="form-group" style="flex:1"><label class="form-label">Order Amount ($)</label><input class="form-input" id="job-amount" type="number" step="0.01"></div>' +
            '</div>' +
            '<div class="form-group"><label class="form-label">Sender Name</label><input class="form-input" id="job-sender-name"></div>' +
            '<div class="form-group"><label class="form-label">Sender Address</label><input class="form-input" id="job-sender-address"></div>';

        App.modal.confirm({
            title: 'Create Delivery Job',
            message: html,
            confirmText: 'Create Job',
            cancelText: 'Cancel'
        }).done(function () {
            var data = {
                receiverName: $('#job-receiver-name').val(),
                receiverAddress: $('#job-receiver-address').val(),
                receiverState: $('#job-receiver-state').val(),
                receiverZip: $('#job-receiver-zip').val(),
                weightLbs: parseFloat($('#job-weight').val()),
                orderAmount: parseFloat($('#job-amount').val()),
                senderName: $('#job-sender-name').val(),
                senderAddress: $('#job-sender-address').val()
            };
            App.api.post('/api/jobs', data).done(function (resp) {
                App.toast.success('Job created: #' + resp.data.trackingNumber);
                loadJobs();
            }).fail(function (xhr) {
                if (xhr.status === 422) {
                    var err = xhr.responseJSON;
                    App.toast.error(err.message || 'Validation failed');
                }
            });
        });
    }

    function init() {
        loadJobs();
        pollTimer = setInterval(function () {
            loadJobs();
        }, POLL_INTERVAL);

        // Also re-apply idle warnings every 30s without full reload
        setInterval(applyIdleWarnings, 30000);

        $(document).on('click', '#btn-create-job', showCreateJobModal);

        // Assign courier to a job
        $(document).on('click', '.btn-assign', function (e) {
            e.stopPropagation();
            var jobId = $(this).data('id');
            App.modal.confirm({
                title: 'Assign Courier',
                message: '<div class="form-group"><label class="form-label">Courier User ID</label>' +
                    '<input class="form-input" id="assign-courier-id" type="number" placeholder="e.g. 4"></div>',
                confirmText: 'Assign'
            }).done(function () {
                var courierId = parseInt($('#assign-courier-id').val());
                if (!courierId) { App.toast.warning('Enter a valid courier ID'); return; }
                App.api.put('/api/jobs/' + jobId + '/assign?courierId=' + courierId, {}).done(function () {
                    App.toast.success('Courier assigned');
                    loadJobs();
                }).fail(function (xhr) {
                    App.toast.error(xhr.responseJSON ? xhr.responseJSON.message : 'Assignment failed');
                });
            });
        });

        // Advance job status
        $(document).on('click', '.btn-advance', function (e) {
            e.stopPropagation();
            var jobId = $(this).data('id');
            var nextStatus = $(this).data('status');
            var version = $(this).data('version');
            App.api.put('/api/jobs/' + jobId + '/status', {
                status: nextStatus, comment: '', version: version
            }).done(function () {
                App.toast.success('Status updated to ' + nextStatus);
                loadJobs();
            }).fail(function (xhr) {
                App.toast.error(xhr.responseJSON ? xhr.responseJSON.message : 'Status update failed');
            });
        });

        // Pick list generation
        $(document).on('click', '#btn-pick-list', function () {
            var today = new Date().toISOString().slice(0, 10);
            App.api.post('/api/jobs/picklist?runDate=' + today, {}).done(function (resp) {
                if (resp && resp.data && resp.data.length > 0) {
                    var lines = resp.data.map(function (j) { return j.trackingNumber + ' | ' + (j.receiverName || '') + ' | ' + (j.receiverZip || ''); });
                    App.modal.confirm({ title: 'Pick List (' + resp.data.length + ' jobs)', message: '<pre style="max-height:400px;overflow:auto">' + App.util.escapeHtml(lines.join('\n')) + '</pre>' });
                } else {
                    App.toast.info('No jobs in CREATED status for today.');
                }
            });
        });

        // Sort list generation
        $(document).on('click', '#btn-sort-list', function () {
            var today = new Date().toISOString().slice(0, 10);
            App.api.post('/api/jobs/sortlist?runDate=' + today, {}).done(function (resp) {
                if (resp && resp.data && resp.data.length > 0) {
                    var lines = resp.data.map(function (j) { return j.trackingNumber + ' | ' + (j.receiverName || '') + ' | ' + (j.receiverZip || ''); });
                    App.modal.confirm({ title: 'Sort List (' + resp.data.length + ' jobs)', message: '<pre style="max-height:400px;overflow:auto">' + App.util.escapeHtml(lines.join('\n')) + '</pre>' });
                } else {
                    App.toast.info('No jobs in PICKED status for today.');
                }
            });
        });
    }

    function destroy() {
        if (pollTimer) clearInterval(pollTimer);
    }

    return { init: init, destroy: destroy, loadJobs: loadJobs };
})();
