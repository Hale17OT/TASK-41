/**
 * DispatchOps -- Search Module
 */
var App = window.App || {};

App.search = (function () {
    'use strict';

    function performSearch() {
        var query = $('#search-query').val() || getUrlParam('q') || '';
        if (!query.trim()) return;

        var entityType = $('input[name="type"]:checked').map(function () { return $(this).val(); }).get().join(',');
        var dateFrom = $('#filter-date-from').val() || '';
        var dateTo = $('#filter-date-to').val() || '';
        var status = $('#filter-status').val() || '';
        var sortBy = $('#filter-sort').val() || '';
        var author = $('#filter-author').val() || '';
        var params = { q: query, type: entityType, page: 0, size: 25 };
        if (dateFrom) params.dateFrom = dateFrom;
        if (dateTo) params.dateTo = dateTo;
        if (status) params.status = status;
        if (sortBy) params.sort = sortBy;
        if (author) params.author = author;

        App.shimmer.show('#search-results', 'list', 5);

        App.api.get('/api/search', params).done(function (resp) {
            if (resp && resp.data) {
                if ((resp.data.content || resp.data).length === 0) {
                    loadSuggestions(query);
                } else {
                    renderResults(resp.data.content || resp.data);
                }
            }
        });
    }

    function renderResults(results) {
        var $container = $('#search-results');
        $container.empty();
        $('#no-results').addClass('hidden');

        results.forEach(function (item) {
            var html = '<div class="card mb-s">' +
                '<div class="flex items-center gap-s">' +
                '<span class="status-badge status-info">' + App.util.escapeHtml(item.entityType) + '</span>' +
                '<strong>' + App.util.escapeHtml(item.title) + '</strong>' +
                '</div>' +
                '<p class="text-caption mt-xs">' + App.util.escapeHtml(App.util.truncate(item.description || '', 200)) + '</p>' +
                (item.tags ? '<div class="mt-xs">' + item.tags.split(',').map(function (t) { return '<span class="status-badge" style="margin-right:4px">' + App.util.escapeHtml(t.trim()) + '</span>'; }).join('') + '</div>' : '') +
                '</div>';
            $container.append(html);
        });
    }

    function loadSuggestions(query) {
        $('#search-results').empty();
        App.api.get('/api/search/suggest', { q: query }).done(function (resp) {
            if (resp && resp.data) {
                var d = resp.data;
                var html = '<div class="empty-state"><div class="empty-icon">&#128269;</div>' +
                    '<div class="empty-title">No results for "' + App.util.escapeHtml(query) + '"</div>';
                if (d.synonyms && d.synonyms.length > 0) {
                    html += '<p>Did you mean: ' + d.synonyms.map(function (s) {
                        return '<a href="/search?q=' + encodeURIComponent(s) + '" style="color:var(--fluent-color-brand-primary)">' + App.util.escapeHtml(s) + '</a>';
                    }).join(', ') + '?</p>';
                }
                if (d.relatedCategories && d.relatedCategories.length > 0) {
                    html += '<p class="mt-s">Try browsing: ' + d.relatedCategories.map(function (cat) {
                        return '<a href="/search?q=' + encodeURIComponent(query) + '&type=' + encodeURIComponent(cat) +
                            '" style="color:var(--fluent-color-brand-primary);margin-right:var(--fluent-spacing-s)">' +
                            App.util.escapeHtml(cat) + '</a>';
                    }).join('') + '</p>';
                }
                html += '</div>';
                $('#search-results').html(html);
            }
        });
    }

    function loadTrending() {
        App.api.get('/api/search/trending', { topN: 10 }).done(function (resp) {
            if (resp && resp.data && resp.data.length > 0) {
                var html = '<h3 class="mb-s">Trending</h3>';
                resp.data.forEach(function (t, i) {
                    html += '<div class="mb-xs"><a href="/search?q=' + encodeURIComponent(t.term) + '" style="color:var(--fluent-color-brand-primary);text-decoration:none">' +
                        (i + 1) + '. ' + App.util.escapeHtml(t.term) +
                        '</a> <span class="text-caption">(' + t.searchCount + ')</span></div>';
                });
                $('#trending-list').html(html);
            }
        });
    }

    function getUrlParam(name) {
        var params = new URLSearchParams(window.location.search);
        return params.get(name);
    }

    function init() {
        var initialQuery = getUrlParam('q');
        var initialType = getUrlParam('type');

        // Preselect entity type checkboxes from URL (for category suggestion links)
        if (initialType) {
            var types = initialType.split(',');
            $('input[name="type"]').each(function () {
                $(this).prop('checked', types.indexOf($(this).val()) >= 0);
            });
        }

        if (initialQuery) {
            $('#search-query').val(initialQuery);
            performSearch();
        }

        loadTrending();

        $('#search-query').on('keydown', function (e) {
            if (e.key === 'Enter') performSearch();
        });

        $('#search-btn').on('click', performSearch);

        $('input[name="type"]').on('change', performSearch);
        $('#btn-apply-filters').on('click', performSearch);
    }

    return { init: init };
})();
