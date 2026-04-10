/**
 * DispatchOps -- Central API Client
 * All REST communication goes through App.api.
 * Handles CSRF, auth redirects, global error toasts, loading spinner.
 */
var App = window.App || {};

App.api = (function () {
    'use strict';

    var csrfToken = null;
    var csrfHeader = null;

    function init() {
        csrfToken = $("meta[name='_csrf']").attr("content") || '';
        csrfHeader = $("meta[name='_csrf_header']").attr("content") || 'X-CSRF-TOKEN';
    }

    // Attach CSRF header to every mutating request
    $(document).ajaxSend(function (e, xhr, options) {
        if (options.type && options.type !== 'GET' && options.type !== 'HEAD') {
            if (csrfToken && csrfHeader) {
                xhr.setRequestHeader(csrfHeader, csrfToken);
            }
        }
    });

    // Global loading spinner
    $(document).ajaxStart(function () {
        $('#global-spinner').addClass('active');
    }).ajaxStop(function () {
        $('#global-spinner').removeClass('active');
    });

    function request(method, url, data, options) {
        var isFormData = (typeof FormData !== 'undefined' && data instanceof FormData);
        var settings = $.extend({
            url: url,
            method: method,
            contentType: isFormData ? false : 'application/json; charset=UTF-8',
            processData: isFormData ? false : true,
            dataType: 'json',
            timeout: 30000,
            data: isFormData ? data : ((method === 'GET') ? data : JSON.stringify(data))
        }, options || {});

        var deferred = $.ajax(settings);

        deferred.fail(function (xhr) {
            if (xhr.status === 401) {
                window.location.href = '/login?expired=true';
                return;
            }
            if (xhr.status === 403) {
                App.toast.error('You do not have permission for this action.');
                return;
            }
            if (xhr.status === 409) {
                // Conflict -- let caller handle
                return;
            }
            if (xhr.status === 422) {
                // Validation -- let caller handle
                return;
            }
            if (xhr.status === 423) {
                App.toast.warning('Account is locked. Please try again later.');
                return;
            }
            if (xhr.status >= 500) {
                App.toast.error('Server error. Please try again later.');
            }
        });

        return deferred;
    }

    return {
        init: init,
        get: function (url, params, opts) { return request('GET', url, params, opts); },
        post: function (url, data, opts) { return request('POST', url, data, opts); },
        put: function (url, data, opts) { return request('PUT', url, data, opts); },
        del: function (url, data, opts) { return request('DELETE', url, data, opts); },
        upload: function (url, formData) {
            return request('POST', url, formData, {
                contentType: false,
                processData: false,
                dataType: 'json'
            });
        },
        updateCsrf: function (token) {
            csrfToken = token;
            $("meta[name='_csrf']").attr("content", token);
        }
    };
})();

$(function () {
    App.api.init();
});
