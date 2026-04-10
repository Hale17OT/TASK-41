/**
 * DispatchOps -- Shimmer Loading Placeholders
 * Shows skeleton loading states during async data fetches.
 */
var App = window.App || {};

App.shimmer = (function () {
    'use strict';

    var templates = {
        card: '<div class="shimmer shimmer-card"></div>',
        text: '<div class="shimmer shimmer-text"></div><div class="shimmer shimmer-text" style="width:60%"></div>',
        title: '<div class="shimmer shimmer-title"></div>',
        table: '<div class="shimmer shimmer-text"></div>'.repeat(5),
        list: function (count) {
            var html = '';
            for (var i = 0; i < (count || 3); i++) {
                html += '<div class="shimmer shimmer-card" style="height:60px"></div>';
            }
            return html;
        },
        cards: function (count) {
            var html = '<div class="card-grid">';
            for (var i = 0; i < (count || 3); i++) {
                html += '<div class="shimmer shimmer-card"></div>';
            }
            html += '</div>';
            return html;
        }
    };

    /**
     * Show shimmer placeholders inside a container.
     * @param {jQuery|string} container - Selector or jQuery element
     * @param {string} type - Template type: 'card', 'text', 'title', 'table', 'list', 'cards'
     * @param {number} count - Number of items for list/cards types
     */
    function show(container, type, count) {
        var $el = $(container);
        var tmpl = templates[type];
        if (typeof tmpl === 'function') {
            $el.html(tmpl(count));
        } else if (tmpl) {
            $el.html(tmpl);
        } else {
            $el.html(templates.text);
        }
    }

    /**
     * Remove shimmer from a container.
     */
    function hide(container) {
        $(container).find('.shimmer').remove();
    }

    return {
        show: show,
        hide: hide
    };
})();
