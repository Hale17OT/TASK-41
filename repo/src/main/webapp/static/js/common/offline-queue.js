/**
 * DispatchOps -- Offline Queue for Payments
 * Stores pending payments in localStorage when offline.
 * Syncs sequentially when connectivity returns.
 * Uses UUID idempotency keys to prevent double-posting.
 */
var App = window.App || {};

App.offlineQueue = (function () {
    'use strict';

    var STORAGE_KEY = 'dispatchops_offline_queue';
    var MAX_QUEUE_SIZE = 200;
    var MAX_RETRY_ATTEMPTS = 5;
    var syncLock = false;

    function getQueue() {
        try {
            return JSON.parse(localStorage.getItem(STORAGE_KEY)) || [];
        } catch (e) {
            return [];
        }
    }

    function saveQueue(queue) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(queue));
    }

    /**
     * Add a request to the offline queue.
     * Returns the generated idempotency key.
     */
    function enqueue(url, method, data) {
        var queue = getQueue();
        if (queue.length >= MAX_QUEUE_SIZE) {
            App.toast.error('Offline queue is full (' + MAX_QUEUE_SIZE + ' items). Please connect to sync.');
            return null;
        }

        var idempotencyKey = data._idempotencyKey || data.idempotencyKey || App.util.generateUUID();
        data.idempotencyKey = idempotencyKey;

        var entry = {
            id: idempotencyKey,
            url: url,
            method: method,
            data: data,
            status: 'pending',
            createdAt: new Date().toISOString(),
            attempts: 0
        };

        queue.push(entry);
        saveQueue(queue);
        $(document).trigger('offlineQueue:changed', [queue]);
        return idempotencyKey;
    }

    /**
     * Synchronize all pending/failed items sequentially.
     */
    function syncAll() {
        if (syncLock) return;
        syncLock = true;

        var queue = getQueue();
        var pending = queue.filter(function (e) {
            return e.status === 'pending' || e.status === 'failed';
        });

        if (pending.length === 0) {
            syncLock = false;
            return;
        }

        var chain = $.Deferred().resolve();
        pending.forEach(function (entry) {
            chain = chain.then(function () {
                entry.status = 'syncing';
                entry.attempts += 1;
                saveQueue(queue);
                $(document).trigger('offlineQueue:changed', [queue]);

                return App.api[entry.method.toLowerCase()](entry.url, entry.data)
                    .done(function () {
                        entry.status = 'synced';
                        entry.syncedAt = new Date().toISOString();
                    })
                    .fail(function (xhr) {
                        if (xhr.status === 409) {
                            // Server already processed this idempotency key
                            entry.status = 'synced';
                            entry.syncedAt = new Date().toISOString();
                        } else if (entry.attempts >= MAX_RETRY_ATTEMPTS) {
                            entry.status = 'dead_letter';
                            App.toast.error('Payment sync failed after ' + MAX_RETRY_ATTEMPTS + ' attempts. Please handle manually.');
                        } else {
                            entry.status = 'failed';
                        }
                    })
                    .always(function () {
                        saveQueue(queue);
                        $(document).trigger('offlineQueue:changed', [queue]);
                    });
            });
        });

        chain.always(function () {
            syncLock = false;
            // Clean up synced items older than 24 hours
            cleanSynced();
        });
    }

    function cleanSynced() {
        var queue = getQueue();
        var cutoff = Date.now() - (24 * 60 * 60 * 1000);
        var cleaned = queue.filter(function (e) {
            if (e.status === 'synced' && e.syncedAt) {
                return new Date(e.syncedAt).getTime() > cutoff;
            }
            return true;
        });
        saveQueue(cleaned);
    }

    function getPendingCount() {
        return getQueue().filter(function (e) {
            return e.status !== 'synced';
        }).length;
    }

    function clearSynced() {
        var queue = getQueue().filter(function (e) { return e.status !== 'synced'; });
        saveQueue(queue);
        $(document).trigger('offlineQueue:changed', [queue]);
    }

    // Listen for connectivity changes
    $(window).on('online', function () {
        $('.connectivity-banner').removeClass('visible');
        App.toast.info('Connection restored. Syncing pending items...');
        syncAll();
    });

    $(window).on('offline', function () {
        App.toast.warning('You are offline. Actions will be queued locally.');
        $('.connectivity-banner').addClass('visible');
    });

    return {
        enqueue: enqueue,
        syncAll: syncAll,
        getQueue: getQueue,
        getPendingCount: getPendingCount,
        clearSynced: clearSynced
    };
})();
