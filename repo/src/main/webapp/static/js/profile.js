/**
 * DispatchOps -- Profile Management Module
 */
var App = window.App || {};

App.profile = (function () {
    'use strict';

    function loadProfile() {
        var user = App.auth.getCurrentUser();
        if (!user) return;

        App.api.get('/api/profiles/' + user.id).done(function (resp) {
            if (resp && resp.data) {
                var p = resp.data;
                $('#profile-display-name').val(p.displayName || '');
                $('#profile-email').val(p.email || '');
                $('#profile-phone').val(p.phone || '');
                $('#profile-bio').val(p.bio || '');
                if (p.avatarPath) {
                    $('#avatar-image').attr('src', p.avatarPath).show();
                    $('#avatar-placeholder').hide();
                }
            }
        });

        // Load persisted per-field visibility settings
        App.api.get('/api/profiles/' + user.id + '/visibility').done(function (resp) {
            if (resp && resp.data) {
                resp.data.forEach(function (setting) {
                    var fieldName = setting.fieldName;
                    var tier = setting.visibilityTier;
                    var $select = $('.vis-tier-select[data-field="' + fieldName + '"]');
                    if ($select.length) {
                        $select.val(tier);
                    }
                });
            }
        });
    }

    function saveProfile() {
        var user = App.auth.getCurrentUser();
        if (!user) return;

        var data = {
            displayName: $('#profile-display-name').val(),
            email: $('#profile-email').val(),
            phone: $('#profile-phone').val(),
            bio: $('#profile-bio').val()
        };

        App.api.put('/api/profiles/' + user.id, data).done(function () {
            App.toast.success('Profile updated');
        }).fail(function (xhr) {
            App.toast.error(xhr.responseJSON ? xhr.responseJSON.message : 'Update failed');
        });
    }

    function uploadAvatar() {
        var fileInput = document.getElementById('avatar-file');
        if (!fileInput.files || !fileInput.files[0]) {
            App.toast.warning('Please select a file.');
            return;
        }

        var file = fileInput.files[0];

        // Client-side checks (aligned with backend: JPEG/PNG up to 5MB, PDF up to 10MB)
        var allowedTypes = ['image/jpeg', 'image/png', 'application/pdf'];
        if (allowedTypes.indexOf(file.type) === -1) {
            App.toast.error('Only JPEG, PNG, and PDF files are allowed.');
            return;
        }
        var maxSize = file.type === 'application/pdf' ? 10 * 1024 * 1024 : 5 * 1024 * 1024;
        if (file.size > maxSize) {
            App.toast.error('File exceeds size limit (' + (file.type === 'application/pdf' ? '10MB' : '5MB') + ').');
            return;
        }

        var formData = new FormData();
        formData.append('file', file);

        var user = App.auth.getCurrentUser();
        App.api.upload('/api/profiles/' + user.id + '/avatar', formData).done(function () {
            App.toast.success('Avatar uploaded');
            // Preview
            var reader = new FileReader();
            reader.onload = function (e) {
                $('#avatar-image').attr('src', e.target.result).show();
                $('#avatar-placeholder').hide();
            };
            reader.readAsDataURL(file);
        }).fail(function (xhr) {
            App.toast.error(xhr.responseJSON ? xhr.responseJSON.message : 'Upload failed');
        });
    }

    function init() {
        loadProfile();

        $(document).on('click', '#btn-save-profile', saveProfile);
        $('#profile-form').on('submit', function(e) { e.preventDefault(); saveProfile(); });
        $(document).on('click', '#btn-upload-avatar', function() { $('#avatar-file').click(); });
        $('#avatar-file').on('change', uploadAvatar);

        // Per-field visibility tier selectors
        $(document).on('change', '.vis-tier-select', function () {
            var fieldName = $(this).data('field');
            var tier = parseInt($(this).val());
            var currentUser = App.auth.getCurrentUser();
            if (!currentUser || !fieldName) return;

            App.api.put('/api/profiles/' + currentUser.id + '/visibility', {
                field: fieldName,
                tier: tier
            }).done(function () {
                App.toast.success('Visibility updated for ' + fieldName);
            }).fail(function () {
                App.toast.error('Failed to update visibility');
            });
        });
    }

    return { init: init };
})();
