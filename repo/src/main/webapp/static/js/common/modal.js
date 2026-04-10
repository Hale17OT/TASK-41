/**
 * DispatchOps -- Reusable Modal/Dialog System
 * Returns jQuery Deferred promises.
 */
var App = window.App || {};

App.modal = (function () {
    'use strict';

    var $overlay = null;

    function ensureOverlay() {
        if (!$overlay || !$overlay.length) {
            $overlay = $('<div class="modal-overlay" id="modal-overlay"></div>').appendTo('body');
        }
    }

    function closeModal($modal) {
        if ($modal) $modal.remove();
        if ($overlay && $overlay.children().length === 0) {
            $overlay.removeClass('active');
        }
    }

    /**
     * Simple confirm dialog. Returns Deferred -- .done() on confirm, .fail() on cancel.
     */
    function confirm(options) {
        var opts = $.extend({
            title: 'Confirm',
            message: 'Are you sure?',
            confirmText: 'Confirm',
            cancelText: 'Cancel',
            confirmClass: 'fluent-btn-primary',
            danger: false
        }, options);

        if (opts.danger) opts.confirmClass = 'fluent-btn-danger';

        var dfd = $.Deferred();
        ensureOverlay();

        var html =
            '<div class="modal-dialog">' +
            '  <div class="modal-header"><h3>' + App.util.escapeHtml(opts.title) + '</h3>' +
            '    <button class="modal-close">&times;</button></div>' +
            '  <div class="modal-body"><p>' + opts.message + '</p></div>' +
            '  <div class="modal-footer">' +
            '    <button class="fluent-btn fluent-btn-secondary modal-cancel">' + App.util.escapeHtml(opts.cancelText) + '</button>' +
            '    <button class="fluent-btn ' + opts.confirmClass + ' modal-confirm">' + App.util.escapeHtml(opts.confirmText) + '</button>' +
            '  </div>' +
            '</div>';

        var $modal = $(html).appendTo($overlay);
        $overlay.addClass('active');

        $modal.on('click', '.modal-confirm', function () { dfd.resolve(); closeModal($modal); });
        $modal.on('click', '.modal-cancel, .modal-close', function () { dfd.reject(); closeModal($modal); });

        return dfd.promise();
    }

    /**
     * Prompt with mandatory comment. Returns Deferred resolved with comment text.
     */
    function promptComment(options) {
        var opts = $.extend({
            title: 'Add Comment',
            placeholder: 'Enter your comment (min 10 characters)...',
            minLength: 10,
            submitText: 'Submit'
        }, options);

        var dfd = $.Deferred();
        ensureOverlay();

        var html =
            '<div class="modal-dialog">' +
            '  <div class="modal-header"><h3>' + App.util.escapeHtml(opts.title) + '</h3>' +
            '    <button class="modal-close">&times;</button></div>' +
            '  <div class="modal-body">' +
            '    <textarea class="form-textarea modal-comment-input" rows="4" placeholder="' + App.util.escapeHtml(opts.placeholder) + '"></textarea>' +
            '    <div class="form-error modal-comment-error hidden">Minimum ' + opts.minLength + ' characters required.</div>' +
            '  </div>' +
            '  <div class="modal-footer">' +
            '    <button class="fluent-btn fluent-btn-secondary modal-cancel">Cancel</button>' +
            '    <button class="fluent-btn fluent-btn-primary modal-submit">' + App.util.escapeHtml(opts.submitText) + '</button>' +
            '  </div>' +
            '</div>';

        var $modal = $(html).appendTo($overlay);
        $overlay.addClass('active');
        $modal.find('.modal-comment-input').focus();

        $modal.on('click', '.modal-submit', function () {
            var text = $modal.find('.modal-comment-input').val().trim();
            if (text.length < opts.minLength) {
                $modal.find('.modal-comment-error').removeClass('hidden');
                $modal.find('.modal-comment-input').addClass('error').focus();
                return;
            }
            dfd.resolve(text);
            closeModal($modal);
        });

        $modal.on('click', '.modal-cancel, .modal-close', function () { dfd.reject(); closeModal($modal); });

        return dfd.promise();
    }

    /**
     * Merge conflict dialog for optimistic locking failures.
     */
    function mergeConflict(options) {
        var opts = $.extend({
            title: 'Conflict Detected',
            message: 'This record was modified by another user. Please review the changes.',
            currentData: {},
            serverData: {}
        }, options);

        var dfd = $.Deferred();
        ensureOverlay();

        var html =
            '<div class="modal-dialog" style="max-width:600px">' +
            '  <div class="modal-header"><h3>' + App.util.escapeHtml(opts.title) + '</h3>' +
            '    <button class="modal-close">&times;</button></div>' +
            '  <div class="modal-body">' +
            '    <p class="mb-m">' + App.util.escapeHtml(opts.message) + '</p>' +
            '    <div class="flex gap-m">' +
            '      <div class="card" style="flex:1"><h3>Your Version</h3><pre style="white-space:pre-wrap;font-size:12px">' + App.util.escapeHtml(JSON.stringify(opts.currentData, null, 2)) + '</pre></div>' +
            '      <div class="card" style="flex:1"><h3>Server Version</h3><pre style="white-space:pre-wrap;font-size:12px">' + App.util.escapeHtml(JSON.stringify(opts.serverData, null, 2)) + '</pre></div>' +
            '    </div>' +
            '  </div>' +
            '  <div class="modal-footer">' +
            '    <button class="fluent-btn fluent-btn-secondary modal-cancel">Cancel</button>' +
            '    <button class="fluent-btn fluent-btn-primary modal-use-server">Use Server Version</button>' +
            '    <button class="fluent-btn fluent-btn-danger modal-force">Force My Changes</button>' +
            '  </div>' +
            '</div>';

        var $modal = $(html).appendTo($overlay);
        $overlay.addClass('active');

        $modal.on('click', '.modal-use-server', function () { dfd.resolve('server'); closeModal($modal); });
        $modal.on('click', '.modal-force', function () { dfd.resolve('force'); closeModal($modal); });
        $modal.on('click', '.modal-cancel, .modal-close', function () { dfd.reject(); closeModal($modal); });

        return dfd.promise();
    }

    return {
        confirm: confirm,
        promptComment: promptComment,
        mergeConflict: mergeConflict,
        close: function () {
            if ($overlay) { $overlay.empty().removeClass('active'); }
        }
    };
})();
