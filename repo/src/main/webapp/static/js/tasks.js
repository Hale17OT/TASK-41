/**
 * DispatchOps -- Task Collaboration Module
 */
var App = window.App || {};

App.tasks = (function () {
    'use strict';

    var currentTab = 'TODO';

    var tabListMap = { 'TODO': '#task-list', 'DONE': '#task-list-done', 'CC': '#task-list-cc' };

    function loadTasks(inboxType) {
        currentTab = inboxType || 'TODO';
        var targetList = tabListMap[currentTab] || '#task-list';

        // Show/hide panels
        $('.tab-panel').addClass('hidden');
        $('#tab-' + currentTab.toLowerCase()).removeClass('hidden');

        App.shimmer.show(targetList, 'list', 5);

        App.api.get('/api/tasks', { inbox: currentTab, page: 0, size: 50 }).done(function (resp) {
            if (resp && resp.data) {
                renderTasks(resp.data.content || resp.data, targetList);
            }
        });
    }

    function renderTasks(tasks, targetSelector) {
        var $list = $(targetSelector || '#task-list');
        $list.empty();

        if (!tasks || tasks.length === 0) {
            $list.html('<div class="empty-state"><div class="empty-icon">&#9745;</div><div class="empty-title">No tasks</div></div>');
            return;
        }

        tasks.forEach(function (task) {
            var statusClass = 'status-' + (task.status || 'todo').toLowerCase().replace('_', '-');
            var html = '<div class="card mb-s task-card" data-id="' + task.id + '" data-version="' + (task.version || 1) + '">' +
                '<div class="flex justify-between items-center">' +
                '  <div><strong>' + App.util.escapeHtml(task.title) + '</strong></div>' +
                '  <span class="status-badge ' + statusClass + '">' + (task.status || 'TODO') + '</span>' +
                '</div>' +
                '<div class="text-caption mt-s">' +
                (task.dueTime ? 'Due: ' + App.util.formatDateTime(task.dueTime) + ' | ' : '') +
                'Assigned to: ' + App.util.escapeHtml(task.assigneeName || 'Unknown') +
                (task.jobId ? ' | Job #' + task.jobId : '') +
                '</div>' +
                '<div class="flex gap-xs mt-s flex-wrap">' +
                (task.status !== 'DONE' && task.status !== 'EXCEPTION' ? '<button class="fluent-btn fluent-btn-sm fluent-btn-primary btn-transition" data-status="IN_PROGRESS">Start</button>' : '') +
                (task.status === 'IN_PROGRESS' ? '<button class="fluent-btn fluent-btn-sm fluent-btn-success btn-transition" data-status="DONE">Complete</button>' : '') +
                (task.status !== 'DONE' && task.status !== 'BLOCKED' && task.status !== 'EXCEPTION' ? '<button class="fluent-btn fluent-btn-sm fluent-btn-danger btn-transition" data-status="BLOCKED">Block</button>' : '') +
                (task.status !== 'DONE' && task.status !== 'EXCEPTION' ? '<button class="fluent-btn fluent-btn-sm fluent-btn-danger btn-transition" data-status="EXCEPTION">Exception</button>' : '') +
                ' <button class="fluent-btn fluent-btn-sm fluent-btn-secondary btn-comment" data-id="' + task.id + '">Comment</button>' +
                '</div>' +
                '</div>';
            $list.append(html);
        });
    }

    function transitionTask($card, newStatus) {
        var taskId = $card.data('id');
        var version = $card.data('version');

        function doTransition(comment) {
            App.api.put('/api/tasks/' + taskId + '/status', {
                status: newStatus,
                comment: comment || '',
                version: version
            }).done(function () {
                App.toast.success('Task updated to ' + newStatus);
                loadTasks(currentTab);
            }).fail(function (xhr) {
                if (xhr.status === 409) {
                    var resp = xhr.responseJSON;
                    App.modal.mergeConflict({
                        title: 'Task Conflict',
                        message: 'This task was modified by another user.',
                        serverData: resp.data || {}
                    }).done(function (choice) {
                        if (choice === 'server') loadTasks(currentTab);
                    });
                } else if (xhr.status === 422) {
                    App.toast.error(xhr.responseJSON.message || 'Validation error');
                }
            });
        }

        if (newStatus === 'BLOCKED' || newStatus === 'EXCEPTION') {
            App.modal.promptComment({
                title: newStatus === 'BLOCKED' ? 'Block Reason Required' : 'Exception Reason Required',
                placeholder: 'Explain the reason (min 10 characters)...',
                minLength: 10
            }).done(doTransition);
        } else {
            doTransition('');
        }
    }

    function showCreateTaskModal() {
        App.modal.confirm({
            title: 'Create Task',
            message: '<div class="form-group"><label class="form-label">Title</label><input class="form-input" id="task-title"></div>' +
                '<div class="form-group"><label class="form-label">Description</label><textarea class="form-textarea" id="task-body"></textarea></div>' +
                '<div class="form-group"><label class="form-label">Assignee ID</label><input class="form-input" id="task-assignee" type="number"></div>' +
                '<div class="form-group"><label class="form-label">CC User IDs (comma-separated)</label><input class="form-input" id="task-cc" placeholder="e.g. 4,5"></div>' +
                '<div class="form-group"><label class="form-label">Linked Job ID (optional)</label><input class="form-input" id="task-job" type="number"></div>' +
                '<div class="form-group"><label class="form-label">Due Time <span class="text-danger">*</span></label><input class="form-input" id="task-due" type="datetime-local" required></div>' +
                '<div class="form-group"><label class="flex items-center gap-xs" style="cursor:pointer"><input type="checkbox" id="task-show-cal" checked> Show on calendar</label></div>',
            confirmText: 'Create'
        }).done(function () {
            if (!$('#task-title').val() || !$('#task-assignee').val() || !$('#task-due').val()) {
                App.toast.warning('Title, assignee, and due time are required.');
                return;
            }
            var ccStr = $('#task-cc').val() || '';
            var ccIds = ccStr.split(',').map(function(s){return parseInt(s.trim())}).filter(function(n){return !isNaN(n)});
            App.api.post('/api/tasks', {
                title: $('#task-title').val(),
                body: $('#task-body').val(),
                assigneeId: parseInt($('#task-assignee').val()),
                ccUserIds: ccIds.length > 0 ? ccIds : null,
                jobId: parseInt($('#task-job').val()) || null,
                dueTime: $('#task-due').val(),
                showOnCalendar: $('#task-show-cal').is(':checked')
            }).done(function () {
                App.toast.success('Task created');
                loadTasks(currentTab);
            });
        });
    }

    function init() {
        loadTasks('TODO');

        $(document).on('click', '.tab-item', function () {
            var tab = $(this).data('tab').toUpperCase();
            $('.tab-item').removeClass('active');
            $(this).addClass('active');
            loadTasks(tab);
        });

        $(document).on('click', '.btn-transition', function (e) {
            e.stopPropagation();
            var $card = $(this).closest('.task-card');
            transitionTask($card, $(this).data('status'));
        });

        $(document).on('click', '#btn-create-task, #btn-create-task-cal', showCreateTaskModal);

        // Comment with @mention support
        $(document).on('click', '.btn-comment', function (e) {
            e.stopPropagation();
            var taskId = $(this).data('id');
            App.modal.promptComment({
                title: 'Add Comment',
                placeholder: 'Type your comment... Use @username to mention someone.',
                minLength: 1,
                submitText: 'Post Comment'
            }).done(function (text) {
                App.api.post('/api/tasks/' + taskId + '/comments', { body: text }).done(function () {
                    App.toast.success('Comment posted');
                }).fail(function (xhr) {
                    App.toast.error(xhr.responseJSON ? xhr.responseJSON.message : 'Failed to post comment');
                });
            });
        });

        // Calendar view toggle
        $(document).on('click', '#btn-calendar-view', function () {
            window.location.href = '/tasks/calendar';
        });
    }

    return { init: init };
})();

/**
 * Task calendar sub-module.
 */
App.tasks.calendar = (function () {
    'use strict';

    function init() {
        var calendarEl = document.getElementById('task-calendar');
        if (!calendarEl) return;

        // Check if FullCalendar is available
        if (typeof FullCalendar === 'undefined') {
            calendarEl.innerHTML = '<div class="empty-state"><div class="empty-title">Calendar library not loaded</div><p class="text-secondary">FullCalendar assets are required for this view.</p></div>';
            return;
        }

        var calendar = new FullCalendar.Calendar(calendarEl, {
            initialView: 'dayGridMonth',
            headerToolbar: {
                left: 'prev,next today',
                center: 'title',
                right: 'dayGridMonth,timeGridWeek,listWeek'
            },
            events: function (info, successCallback, failureCallback) {
                var from = info.startStr.substring(0, 10);
                var to = info.endStr.substring(0, 10);
                App.api.get('/api/tasks/calendar', { from: from, to: to }).done(function (resp) {
                    if (resp && resp.data) {
                        var events = (resp.data.content || resp.data).map(function (task) {
                            return {
                                id: task.id,
                                title: task.title,
                                start: task.dueTime || task.createdAt,
                                color: task.status === 'DONE' ? '#107C10' :
                                       task.status === 'BLOCKED' ? '#D13438' : '#0078D4'
                            };
                        });
                        successCallback(events);
                    }
                }).fail(failureCallback);
            },
            eventClick: function (info) {
                window.location.href = '/tasks?id=' + info.event.id;
            },
            height: 'auto'
        });
        calendar.render();
    }

    return { init: init };
})();
