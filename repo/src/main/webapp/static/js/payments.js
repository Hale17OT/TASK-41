/**
 * DispatchOps -- Payment & Settlement Module
 * Handles online/offline payment recording, settlement, refunds, reconciliation.
 */
var App = window.App || {};

App.payments = (function () {
    'use strict';

    function recordPayment() {
        var data = {
            idempotencyKey: App.util.generateUUID(),
            jobId: parseInt($('#payment-order').val()),
            amount: parseFloat($('#payment-amount').val()),
            method: $('input[name="method"]:checked').val(),
            checkNumber: $('#payment-check').val() || null,
            notes: $('#payment-notes').val() || ''
        };

        if (!data.jobId || !data.amount || !data.method) {
            App.toast.warning('Please fill all required fields.');
            return;
        }

        if (!navigator.onLine) {
            var queueId = App.offlineQueue.enqueue('/api/payments', 'POST', data);
            if (queueId) {
                App.toast.warning('Payment saved offline. Will sync when connected.');
                resetPaymentForm();
            }
            return;
        }

        App.api.post('/api/payments', data).done(function (resp) {
            App.toast.success('Payment recorded: ' + App.util.formatCurrency(data.amount));
            resetPaymentForm();
            loadHistory();
        }).fail(function (xhr) {
            if (xhr.status === 0 || xhr.statusText === 'timeout') {
                App.offlineQueue.enqueue('/api/payments', 'POST', data);
                App.toast.warning('Network lost. Payment queued for sync.');
                resetPaymentForm();
            } else if (xhr.status === 422) {
                App.toast.error(xhr.responseJSON ? xhr.responseJSON.message : 'Validation error');
            }
        });
    }

    function resetPaymentForm() {
        $('#payment-order').val('');
        $('#payment-amount').val('');
        $('#payment-check').val('');
        $('#payment-notes').val('');
    }

    function loadHistory() {
        App.api.get('/api/payments/list', { page: 0, size: 50 }).done(function (resp) {
            if (resp && resp.data) {
                renderHistory(resp.data.content || resp.data);
            }
        }).fail(function () {
            $('#payment-history-body').html('<tr><td colspan="7" class="text-secondary" style="text-align:center">Unable to load payment history</td></tr>');
        });
    }

    function renderHistory(payments) {
        var $body = $('#payment-history-body');
        if (!$body.length) return;
        $body.empty();

        if (!payments || payments.length === 0) {
            $body.html('<tr><td colspan="7" class="text-secondary" style="text-align:center">No payments</td></tr>');
            return;
        }

        payments.forEach(function (p) {
            var statusClass = 'status-' + (p.status || '').toLowerCase().replace(/_/g, '-');
            var html = '<tr>' +
                '<td>' + p.id + '</td>' +
                '<td>' + App.util.formatDateTime(p.createdAt) + '</td>' +
                '<td>' + (p.jobId || '--') + '</td>' +
                '<td>' + App.util.formatCurrency(p.amount) + '</td>' +
                '<td>' + App.util.escapeHtml(p.method) + '</td>' +
                '<td><span class="status-badge ' + statusClass + '">' + App.util.escapeHtml(p.status) + '</span></td>' +
                '<td>' +
                (p.status === 'SETTLED' ? '<button class="fluent-btn fluent-btn-sm fluent-btn-danger btn-refund" data-id="' + p.id + '">Refund</button>' : '') +
                '</td></tr>';
            $body.append(html);
        });
    }

    function processRefund(paymentId) {
        App.modal.confirm({
            title: 'Process Refund',
            message: '<div class="form-group"><label class="form-label">Refund Amount ($)</label><input class="form-input" id="refund-amount" type="number" step="0.01"></div>' +
                '<div class="form-group"><label class="form-label">Reason</label><textarea class="form-textarea" id="refund-reason" placeholder="Reason for refund..."></textarea></div>',
            confirmText: 'Process Refund',
            danger: true
        }).done(function () {
            App.api.post('/api/payments/' + paymentId + '/refund', {
                amount: parseFloat($('#refund-amount').val()),
                reason: $('#refund-reason').val()
            }).done(function () {
                App.toast.success('Refund processed');
                loadHistory();
            }).fail(function (xhr) {
                App.toast.error(xhr.responseJSON ? xhr.responseJSON.message : 'Refund failed');
            });
        });
    }

    function exportReconciliation() {
        var from = $('#recon-date-from').val();
        var to = $('#recon-date-to').val();
        if (!from || !to) {
            App.toast.warning('Please select date range.');
            return;
        }
        window.location.href = '/api/payments/reconciliation/export?from=' + from + '&to=' + to;
    }

    function init() {
        // Payment form submission
        $(document).on('click', '#btn-submit-payment', recordPayment);
        $('#payment-form').on('submit', function (e) { e.preventDefault(); recordPayment(); });

        // Refund
        $(document).on('click', '.btn-refund', function () {
            processRefund($(this).data('id'));
        });

        // Reconciliation: run report — fetch payment list for date range and compute stats
        $(document).on('click', '#btn-run-recon', function () {
            var from = $('#recon-date-from').val();
            var to = $('#recon-date-to').val();
            if (!from || !to) { App.toast.warning('Please select date range.'); return; }
            App.api.get('/api/payments/list', { from: from, to: to, page: 0, size: 1000 }).done(function (resp) {
                var payments = (resp && resp.data && resp.data.content) || [];
                var totalCollected = 0, totalSettled = 0;
                payments.forEach(function (p) {
                    var amt = parseFloat(p.amount) || 0;
                    totalCollected += amt;
                    if (p.status === 'SETTLED') totalSettled += amt;
                });
                $('#recon-total-collected').text('$' + totalCollected.toFixed(2));
                $('#recon-total-settled').text('$' + totalSettled.toFixed(2));
                App.toast.success('Report loaded: ' + payments.length + ' payments found.');
            }).fail(function () {
                App.toast.error('Failed to load reconciliation data.');
            });
        });

        // Reconciliation export
        $(document).on('click', '#btn-export-csv', exportReconciliation);

        // Method toggle: show check number field only for CHECK
        $(document).on('change', 'input[name="method"]', function () {
            if ($(this).val() === 'CHECK') {
                $('#check-number-group').removeClass('hidden');
            } else {
                $('#check-number-group').addClass('hidden');
            }
        });

        // Tab switching using JSP tab IDs
        $(document).on('click', '#payment-tabs .tab-item', function () {
            var tab = $(this).data('tab');
            $('#payment-tabs .tab-item').removeClass('active');
            $(this).addClass('active');
            // Show/hide panels by tab IDs from JSP
            $('#tab-record, #tab-history, #tab-reconciliation').addClass('hidden');
            $('#tab-' + tab).removeClass('hidden');

            if (tab === 'history') loadHistory();
        });

        // Offline queue badge
        $(document).on('offlineQueue:changed', function (e, queue) {
            var pending = queue.filter(function (e) { return e.status !== 'synced'; }).length;
            if (pending > 0) {
                $('#payment-sync-badge').text(pending + ' pending sync').removeClass('hidden');
            } else {
                $('#payment-sync-badge').addClass('hidden');
            }
        });
    }

    return { init: init, loadHistory: loadHistory };
})();
