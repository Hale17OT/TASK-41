/**
 * DispatchOps -- Common Utilities
 * Date formatting uses 24-hour format throughout.
 */
var App = window.App || {};

App.util = (function () {
    'use strict';

    function padZero(num) {
        return num < 10 ? '0' + num : '' + num;
    }

    /**
     * Format ISO date string to "YYYY-MM-DD HH:mm" (24h).
     */
    function formatDateTime(isoString) {
        if (!isoString) return '--';
        var d = new Date(isoString);
        return d.getFullYear() + '-' +
            padZero(d.getMonth() + 1) + '-' +
            padZero(d.getDate()) + ' ' +
            padZero(d.getHours()) + ':' +
            padZero(d.getMinutes());
    }

    /**
     * Format ISO date string to "HH:mm" (24h).
     */
    function formatTime24(isoString) {
        if (!isoString) return '--';
        var d = new Date(isoString);
        return padZero(d.getHours()) + ':' + padZero(d.getMinutes());
    }

    /**
     * Format ISO date string to "YYYY-MM-DD".
     */
    function formatDate(isoString) {
        if (!isoString) return '--';
        var d = new Date(isoString);
        return d.getFullYear() + '-' + padZero(d.getMonth() + 1) + '-' + padZero(d.getDate());
    }

    /**
     * Relative time: "2 min ago", "1 hour ago", etc.
     */
    function timeAgo(isoString) {
        if (!isoString) return '--';
        var now = Date.now();
        var then = new Date(isoString).getTime();
        var diffMs = now - then;
        var diffSec = Math.floor(diffMs / 1000);
        var diffMin = Math.floor(diffSec / 60);
        var diffHr = Math.floor(diffMin / 60);
        var diffDay = Math.floor(diffHr / 24);

        if (diffSec < 60) return 'just now';
        if (diffMin < 60) return diffMin + ' min ago';
        if (diffHr < 24) return diffHr + ' hr ago';
        if (diffDay < 30) return diffDay + ' day' + (diffDay > 1 ? 's' : '') + ' ago';
        return formatDate(isoString);
    }

    /**
     * Format cents to currency string: "$1,234.56"
     */
    function formatCurrency(amount) {
        if (amount == null) return '$0.00';
        var num = parseFloat(amount);
        return '$' + num.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    }

    /**
     * Escape HTML to prevent XSS.
     */
    function escapeHtml(str) {
        if (!str) return '';
        var div = document.createElement('div');
        div.appendChild(document.createTextNode(str));
        return div.innerHTML;
    }

    /**
     * Truncate string to maxLen chars with ellipsis.
     */
    function truncate(str, maxLen) {
        if (!str) return '';
        if (str.length <= maxLen) return str;
        return str.substring(0, maxLen - 3) + '...';
    }

    /**
     * Debounce a function.
     */
    function debounce(fn, delay) {
        var timer;
        return function () {
            var context = this;
            var args = arguments;
            clearTimeout(timer);
            timer = setTimeout(function () {
                fn.apply(context, args);
            }, delay);
        };
    }

    /**
     * Generate UUID v4.
     */
    function generateUUID() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
            var r = Math.random() * 16 | 0;
            return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
        });
    }

    /**
     * Get elapsed minutes since a given ISO timestamp.
     */
    function elapsedMinutes(isoString) {
        if (!isoString) return 0;
        return Math.floor((Date.now() - new Date(isoString).getTime()) / 60000);
    }

    /**
     * Determine idle warning level based on elapsed minutes.
     * Returns: null, 'yellow', 'orange', or 'red'
     */
    function idleLevel(minutes) {
        if (minutes >= 60) return 'red';
        if (minutes >= 40) return 'orange';
        if (minutes >= 20) return 'yellow';
        return null;
    }

    return {
        formatDateTime: formatDateTime,
        formatTime24: formatTime24,
        formatDate: formatDate,
        timeAgo: timeAgo,
        formatCurrency: formatCurrency,
        escapeHtml: escapeHtml,
        truncate: truncate,
        debounce: debounce,
        generateUUID: generateUUID,
        elapsedMinutes: elapsedMinutes,
        idleLevel: idleLevel,
        padZero: padZero
    };
})();
