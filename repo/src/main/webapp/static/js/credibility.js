/**
 * DispatchOps -- Credibility Management Module
 */
var App = window.App || {};

App.credibility = (function () {
    'use strict';

    function loadCreditLevel(courierId) {
        App.api.get('/api/credibility/credit/' + courierId).done(function (resp) {
            if (resp && resp.data) {
                var d = resp.data;
                var lvl = (d.level || 'D').toLowerCase();
                $('#credit-badge').attr('class', 'credit-badge credit-' + lvl).text(d.level || 'D');
                $('#credit-level-name').text('Max ' + (d.maxConcurrent || 1) + ' concurrent jobs');
                $('#credit-level-desc').text('30-day avg: ' + (d.avgRating30d != null ? parseFloat(d.avgRating30d).toFixed(1) : 'N/A'));
            }
        });
    }

    function loadViolations(courierId) {
        App.api.get('/api/credibility/violations/courier/' + courierId).done(function (resp) {
            if (resp && resp.data) {
                renderViolations(resp.data);
            }
        });
    }

    function renderViolations(violations) {
        var $table = $('#violations-body');
        $table.empty();

        if (!violations || violations.length === 0) {
            $table.html('<tr><td colspan="5" class="text-secondary" style="text-align:center;padding:var(--fluent-spacing-l)">No violations</td></tr>');
            return;
        }

        violations.forEach(function (v) {
            var canAppeal = !v.appealed && (Date.now() - new Date(v.createdAt).getTime()) < (48 * 60 * 60 * 1000);
            var html = '<tr>' +
                '<td>' + App.util.formatDateTime(v.createdAt) + '</td>' +
                '<td>' + App.util.escapeHtml(v.violationType) + '</td>' +
                '<td>' + App.util.escapeHtml(v.description || '') + '</td>' +
                '<td>' + App.util.formatDate(v.penaltyEnd) + '</td>' +
                '<td>' + (canAppeal
                    ? '<button class="fluent-btn fluent-btn-sm fluent-btn-secondary btn-appeal" data-id="' + v.id + '">Appeal</button>'
                    : '<span class="text-disabled">Window expired</span>') + '</td>' +
                '</tr>';
            $table.append(html);
        });
    }

    function initStarRating() {
        $(document).on('click', '.star-rating .star', function () {
            var $row = $(this).closest('.star-rating');
            var val = parseInt($(this).data('value'));
            $row.find('.star').each(function () {
                $(this).toggleClass('filled', parseInt($(this).data('value')) <= val);
            });
            $row.data('rating', val);
        });
    }

    function submitRating() {
        var jobId = parseInt($('#rating-order').val());
        var timeliness = $('.star-rating[data-field="timeliness"]').data('rating') || 0;
        var attitude = $('.star-rating[data-field="attitude"]').data('rating') || 0;
        var accuracy = $('.star-rating[data-field="accuracy"]').data('rating') || 0;
        var comment = $('#rating-comment').val();

        if (!jobId || timeliness < 1 || attitude < 1 || accuracy < 1) {
            App.toast.warning('Please select a job and rate all dimensions (1-5).');
            return;
        }

        App.api.post('/api/credibility/ratings', {
            jobId: jobId,
            timeliness: timeliness,
            attitude: attitude,
            accuracy: accuracy,
            comment: comment
        }).done(function () {
            App.toast.success('Rating submitted');
            $('#rating-order').val('');
            $('#rating-comment').val('');
            $('.star-rating .star').removeClass('filled');
        }).fail(function (xhr) {
            if (xhr.status === 422) {
                App.toast.error(xhr.responseJSON.message || 'Invalid rating');
            }
        });
    }

    function loadDeliveredJobsForRating(courierId) {
        var $order = $('#rating-order');
        $order.empty().append('<option value="">Loading...</option>');
        // Fetch jobs assigned to this courier that are DELIVERED (ratable)
        App.api.get('/api/jobs', { status: 'DELIVERED', page: 0, size: 100 }).done(function (resp) {
            $order.empty().append('<option value="">Select delivered job</option>');
            if (resp && resp.data && resp.data.content) {
                resp.data.content.forEach(function (job) {
                    // Only show jobs assigned to the selected courier
                    if (!courierId || String(job.courierId) === String(courierId)) {
                        $order.append('<option value="' + job.id + '">' +
                            App.util.escapeHtml(job.trackingNumber) + ' (' + App.util.formatDate(job.createdAt) + ')</option>');
                    }
                });
            }
        });
    }

    function loadCourierOptions() {
        App.api.get('/api/users', { role: 'COURIER', page: 0, size: 100 }).done(function (resp) {
            var $select = $('#rating-courier');
            $select.empty().append('<option value="">Select courier</option>');
            if (resp && resp.data && resp.data.content) {
                resp.data.content.forEach(function (u) {
                    $select.append('<option value="' + u.id + '">' + App.util.escapeHtml(u.displayName) + '</option>');
                });
            }
        });
    }

    function populateAppealViolations(violations) {
        var $select = $('#appeal-violation');
        $select.empty().append('<option value="">Select violation to appeal</option>');
        if (violations && violations.length) {
            violations.forEach(function (v) {
                var canAppeal = !v.appealed && (Date.now() - new Date(v.createdAt).getTime()) < (48 * 60 * 60 * 1000);
                if (canAppeal) {
                    $select.append('<option value="' + v.id + '">' +
                        App.util.escapeHtml(v.violationType) + ' - ' + App.util.formatDate(v.createdAt) + '</option>');
                }
            });
        }
    }

    function init() {
        initStarRating();

        var user = App.auth.getCurrentUser();

        if (user && user.role === 'COURIER') {
            loadCreditLevel(user.id);
            loadViolations(user.id);
            // Populate appeal form violation select when violations load
            App.api.get('/api/credibility/violations/courier/' + user.id).done(function (resp) {
                if (resp && resp.data) populateAppealViolations(resp.data);
            });
            // Hide rating form for couriers (they don't rate, they get rated)
            $('#rating-form').closest('.card').hide();
        }

        // Dispatcher/OPS/Admin: populate courier selector for ratings
        if (user && user.role !== 'COURIER') {
            loadCourierOptions();
            // When courier is selected, load their delivered jobs
            $(document).on('change', '#rating-courier', function () {
                var courierId = $(this).val();
                if (courierId) {
                    loadDeliveredJobsForRating(courierId);
                } else {
                    $('#rating-order').empty().append('<option value="">Select delivered job</option>');
                }
            });
            // Hide appeal section for non-couriers
            $('#appeal-form').closest('.card').hide();
        }

        $(document).on('click', '#btn-submit-rating', submitRating);

        // Appeal form submit
        $(document).on('submit', '#appeal-form', function (e) {
            e.preventDefault();
            var violationId = $('#appeal-violation').val();
            var reason = $('#appeal-reason').val();
            if (!violationId) { App.toast.warning('Select a violation to appeal'); return; }
            if (!reason || reason.length < 10) { App.toast.warning('Appeal reason must be at least 10 characters'); return; }
            App.api.post('/api/credibility/appeals', {
                violationId: parseInt(violationId),
                reason: reason
            }).done(function () {
                App.toast.success('Appeal submitted');
                $('#appeal-form')[0].reset();
                if (user) loadViolations(user.id);
            }).fail(function (xhr) {
                var msg = (xhr.responseJSON && xhr.responseJSON.message) || 'Failed to submit appeal';
                App.toast.error(msg);
            });
        });

        // Row-level appeal button (from violations table)
        $(document).on('click', '.btn-appeal', function () {
            var violationId = $(this).data('id');
            App.modal.promptComment({
                title: 'File Appeal',
                placeholder: 'Describe your appeal reason...',
                minLength: 10,
                submitText: 'Submit Appeal'
            }).done(function (reason) {
                App.api.post('/api/credibility/appeals', {
                    violationId: violationId,
                    reason: reason
                }).done(function () {
                    App.toast.success('Appeal submitted');
                    if (user) loadViolations(user.id);
                });
            });
        });
    }

    return { init: init, loadCreditLevel: loadCreditLevel };
})();
