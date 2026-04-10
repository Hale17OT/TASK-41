/**
 * DispatchOps -- Admin Module
 */
var App = window.App || {};

App.admin = (function () {
    'use strict';

    function loadUsers() {
        App.shimmer.show('#users-body', 'list', 5);
        App.api.get('/api/users', { page: 0, size: 50 }).done(function (resp) {
            if (resp && resp.data) {
                renderUsers(resp.data.content || resp.data);
            }
        });
    }

    function renderUsers(users) {
        var $body = $('#users-body');
        $body.empty();

        users.forEach(function (u) {
            var html = '<tr>' +
                '<td>' + App.util.escapeHtml(u.username) + '</td>' +
                '<td>' + App.util.escapeHtml(u.displayName) + '</td>' +
                '<td><span class="status-badge">' + u.role + '</span></td>' +
                '<td>' + (u.role === 'COURIER' ? '<span class="text-caption">-</span>' : '<span class="text-disabled">N/A</span>') + '</td>' +
                '<td>' + (u.active ? '<span class="text-success">Active</span>' : '<span class="text-danger">Inactive</span>') + '</td>' +
                '<td><span class="text-caption">' + (u.updatedAt ? App.util.formatDateTime(u.updatedAt) : '-') + '</span></td>' +
                '<td>' +
                '<button class="fluent-btn fluent-btn-sm fluent-btn-secondary btn-edit-user" data-id="' + u.id +
                    '" data-username="' + App.util.escapeHtml(u.username) +
                    '" data-displayname="' + App.util.escapeHtml(u.displayName) +
                    '" data-role="' + u.role +
                    '" data-email="' + App.util.escapeHtml(u.email || '') + '">Edit</button> ' +
                (u.lockoutExpiry ? '<button class="fluent-btn fluent-btn-sm fluent-btn-primary btn-unlock-user" data-id="' + u.id + '">Unlock</button> ' : '') +
                '<button class="fluent-btn fluent-btn-sm btn-deactivate-user" style="color:var(--fluent-color-status-danger)" data-id="' + u.id + '">Deactivate</button>' +
                '</td></tr>';
            $body.append(html);
        });
    }

    function showEditUserModal(btn) {
        var id = $(btn).data('id');
        App.modal.confirm({
            title: 'Edit User',
            message: '<div class="form-group"><label class="form-label">Username</label><input class="form-input" id="edit-username" value="' + App.util.escapeHtml($(btn).data('username')) + '"></div>' +
                '<div class="form-group"><label class="form-label">Display Name</label><input class="form-input" id="edit-displayname" value="' + App.util.escapeHtml($(btn).data('displayname')) + '"></div>' +
                '<div class="form-group"><label class="form-label">Role</label><select class="form-select" id="edit-role">' +
                '<option value="ADMIN"' + ($(btn).data('role') === 'ADMIN' ? ' selected' : '') + '>Administrator</option>' +
                '<option value="OPS_MANAGER"' + ($(btn).data('role') === 'OPS_MANAGER' ? ' selected' : '') + '>Operations Manager</option>' +
                '<option value="DISPATCHER"' + ($(btn).data('role') === 'DISPATCHER' ? ' selected' : '') + '>Dispatcher</option>' +
                '<option value="COURIER"' + ($(btn).data('role') === 'COURIER' ? ' selected' : '') + '>Courier</option>' +
                '<option value="AUDITOR"' + ($(btn).data('role') === 'AUDITOR' ? ' selected' : '') + '>Auditor</option>' +
                '</select></div>' +
                '<div class="form-group"><label class="form-label">Email</label><input class="form-input" id="edit-email" type="email" value="' + App.util.escapeHtml($(btn).data('email')) + '"></div>' +
                '<div class="form-group"><label class="form-label">New Password <span class="text-caption">(leave blank to keep)</span></label><input class="form-input" id="edit-password" type="password"></div>',
            confirmText: 'Save Changes'
        }).done(function () {
            App.api.put('/api/users/' + id, {
                username: $('#edit-username').val(),
                displayName: $('#edit-displayname').val(),
                role: $('#edit-role').val(),
                email: $('#edit-email').val(),
                password: $('#edit-password').val() || null
            }).done(function () {
                App.toast.success('User updated');
                loadUsers();
            }).fail(function (xhr) {
                App.toast.error(xhr.responseJSON ? xhr.responseJSON.message : 'Failed to update user');
            });
        });
    }

    function showCreateUserModal() {
        App.modal.confirm({
            title: 'Add User',
            message: '<div class="form-group"><label class="form-label">Username</label><input class="form-input" id="new-username"></div>' +
                '<div class="form-group"><label class="form-label">Password</label><input class="form-input" id="new-password" type="password"></div>' +
                '<div class="form-group"><label class="form-label">Display Name</label><input class="form-input" id="new-displayname"></div>' +
                '<div class="form-group"><label class="form-label">Role</label><select class="form-select" id="new-role">' +
                '<option value="ADMIN">Administrator</option>' +
                '<option value="OPS_MANAGER">Operations Manager</option>' +
                '<option value="DISPATCHER" selected>Dispatcher</option>' +
                '<option value="COURIER">Courier</option>' +
                '<option value="AUDITOR">Auditor</option>' +
                '</select></div>' +
                '<div class="form-group"><label class="form-label">Email</label><input class="form-input" id="new-email" type="email"></div>',
            confirmText: 'Create User'
        }).done(function () {
            App.api.post('/api/users', {
                username: $('#new-username').val(),
                password: $('#new-password').val(),
                displayName: $('#new-displayname').val(),
                role: $('#new-role').val(),
                email: $('#new-email').val()
            }).done(function () {
                App.toast.success('User created');
                loadUsers();
            }).fail(function (xhr) {
                App.toast.error(xhr.responseJSON ? xhr.responseJSON.message : 'Failed to create user');
            });
        });
    }

    function init() {
        loadUsers();

        $(document).on('click', '#btn-add-user', showCreateUserModal);

        $(document).on('click', '.btn-edit-user', function () {
            showEditUserModal(this);
        });

        $(document).on('click', '.btn-deactivate-user', function () {
            var id = $(this).data('id');
            App.modal.confirm({ title: 'Deactivate User', message: 'Are you sure?', danger: true }).done(function () {
                App.api.put('/api/users/' + id + '/deactivate').done(function () {
                    App.toast.success('User deactivated');
                    loadUsers();
                });
            });
        });

        $(document).on('click', '.btn-unlock-user', function () {
            var id = $(this).data('id');
            App.api.put('/api/users/' + id + '/unlock').done(function () {
                App.toast.success('User unlocked');
                loadUsers();
            });
        });
    }

    return { init: init };
})();

/**
 * Admin Regions sub-module.
 */
App.admin.regions = (function () {
    'use strict';

    function loadTemplates() {
        App.api.get('/api/shipping/templates', { page: 0, size: 50 }).done(function (resp) {
            if (resp && resp.data) {
                var templates = resp.data.content || resp.data;
                var $tree = $('#region-tree');
                $tree.find('ul').empty();
                templates.forEach(function (t) {
                    $tree.find('ul').append(
                        '<li class="nav-item region-item" data-id="' + t.id + '" style="cursor:pointer;padding:var(--fluent-spacing-xs) var(--fluent-spacing-s)">' +
                        App.util.escapeHtml(t.name) + '</li>'
                    );
                });
            }
        });
    }

    function loadRules(templateId) {
        $('#rules-region-label').text('Template #' + templateId);
        App.api.get('/api/shipping/templates/' + templateId + '/rules').done(function (resp) {
            if (resp && resp.data) {
                renderRules(resp.data);
            }
        });
    }

    var currentTemplateId = null;

    function renderRules(rules) {
        var $body = $('#rules-body');
        $body.empty();
        if (!rules || rules.length === 0) {
            $body.html('<tr><td colspan="6" class="text-secondary" style="text-align:center">No rules</td></tr>');
            return;
        }
        rules.forEach(function (r) {
            $body.append('<tr data-rule-id="' + r.id + '">' +
                '<td>' + App.util.escapeHtml(r.stateCode || '*') + '</td>' +
                '<td>' + App.util.escapeHtml(r.zipRangeStart || '*') + ' - ' + App.util.escapeHtml(r.zipRangeEnd || '*') + '</td>' +
                '<td>' + (r.maxWeightLbs || 'N/A') + '</td>' +
                '<td>' + (r.maxOrderAmount || 'N/A') + '</td>' +
                '<td>' + (r.allowed ? 'Allow' : 'Block') + '</td>' +
                '<td>' + r.priority + '</td>' +
                '<td><button class="fluent-btn fluent-btn-secondary fluent-btn-sm btn-edit-rule" data-rule=\'' +
                    JSON.stringify(r).replace(/'/g, '&#39;') + '\'>Edit</button> ' +
                '<button class="fluent-btn fluent-btn-sm btn-delete-rule" style="color:var(--fluent-color-status-danger)" data-id="' + r.id + '">Delete</button></td></tr>');
        });
    }

    function openRuleModal(rule) {
        $('#rule-modal-title').text(rule ? 'Edit Rule' : 'Add Rule');
        $('#rule-id').val(rule ? rule.id : '');
        $('#rule-state').val(rule ? rule.stateCode || '' : '');
        $('#rule-zip-start').val(rule ? rule.zipRangeStart || '' : '');
        $('#rule-zip-end').val(rule ? rule.zipRangeEnd || '' : '');
        $('#rule-max-weight').val(rule ? rule.maxWeightLbs || '' : '');
        $('#rule-max-order').val(rule ? rule.maxOrderAmount || '' : '');
        $('#rule-allowed').val(rule ? String(rule.allowed) : 'true');
        $('#rule-priority').val(rule ? rule.priority : 10);
        $('#rule-modal').addClass('active');
    }

    function submitRule() {
        var ruleId = $('#rule-id').val();
        var data = {
            stateCode: $('#rule-state').val() || null,
            zipRangeStart: $('#rule-zip-start').val() || null,
            zipRangeEnd: $('#rule-zip-end').val() || null,
            maxWeightLbs: $('#rule-max-weight').val() ? parseFloat($('#rule-max-weight').val()) : null,
            maxOrderAmount: $('#rule-max-order').val() ? parseFloat($('#rule-max-order').val()) : null,
            allowed: $('#rule-allowed').val() === 'true',
            priority: parseInt($('#rule-priority').val()) || 10
        };

        var promise;
        if (ruleId) {
            promise = App.api.put('/api/shipping/rules/' + ruleId, data);
        } else {
            promise = App.api.post('/api/shipping/templates/' + currentTemplateId + '/rules', data);
        }

        promise.done(function (resp) {
            if (resp && resp.code < 300) {
                $('#rule-modal').removeClass('active');
                App.toast.success(ruleId ? 'Rule updated' : 'Rule created');
                loadRules(currentTemplateId);
            }
        }).fail(function () {
            App.toast.error('Failed to save rule');
        });
    }

    function deleteRule(ruleId) {
        if (!confirm('Delete this shipping rule?')) return;
        App.api.del('/api/shipping/rules/' + ruleId).done(function () {
            App.toast.success('Rule deleted');
            loadRules(currentTemplateId);
        }).fail(function () {
            App.toast.error('Failed to delete rule');
        });
    }

    function init() {
        loadTemplates();

        $(document).on('click', '.region-item', function () {
            $('.region-item').removeClass('active');
            $(this).addClass('active');
            currentTemplateId = $(this).data('id');
            loadRules(currentTemplateId);
        });

        // Add region (shipping template)
        $(document).on('click', '#btn-add-region', function () {
            $('#region-modal-title').text('Add Region');
            $('#region-id').val('');
            $('#region-name').val('');
            $('#region-code').val('');
            $('#region-modal').addClass('active');
        });

        // Submit region form
        $(document).on('click', '#btn-submit-region', function () {
            var regionId = $('#region-id').val();
            var data = { name: $('#region-name').val(), code: $('#region-code').val() };
            if (!data.name) { App.toast.warning('Region name is required'); return; }
            var promise = regionId
                ? App.api.put('/api/shipping/templates/' + regionId, data)
                : App.api.post('/api/shipping/templates', data);
            promise.done(function () {
                $('#region-modal').removeClass('active');
                App.toast.success(regionId ? 'Region updated' : 'Region created');
                loadTemplates();
            }).fail(function () {
                App.toast.error('Failed to save region');
            });
        });

        // Add rule
        $(document).on('click', '#btn-add-rule', function () {
            if (!currentTemplateId) { App.toast.warning('Select a region first'); return; }
            openRuleModal(null);
        });

        // Edit rule
        $(document).on('click', '.btn-edit-rule', function () {
            var rule = JSON.parse($(this).attr('data-rule'));
            openRuleModal(rule);
        });

        // Delete rule
        $(document).on('click', '.btn-delete-rule', function () {
            deleteRule($(this).data('id'));
        });

        // Submit rule form
        $(document).on('click', '#btn-submit-rule', function () {
            submitRule();
        });

        // Close modals
        $(document).on('click', '[data-dismiss="modal"]', function () {
            $(this).closest('.modal-overlay').removeClass('active');
        });
    }

    return { init: init };
})();

/**
 * Admin Settings sub-module.
 */
App.admin.settings = (function () {
    'use strict';

    function init() {
        // Settings page is read-only; inputs are disabled in JSP.
        // No save handler needed.
    }

    return { init: init };
})();
