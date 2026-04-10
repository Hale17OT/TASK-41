/**
 * DispatchOps -- Contract Management Module
 */
var App = window.App || {};

App.contracts = (function () {
    'use strict';

    function loadTemplates() {
        App.shimmer.show('#contracts-body', 'list', 3);
        App.api.get('/api/contracts/templates', { page: 0, size: 50 }).done(function (resp) {
            if (resp && resp.data) {
                renderTemplates(resp.data.content || resp.data);
            }
        });
    }

    function renderTemplates(templates) {
        var $body = $('#contracts-body');
        $body.empty();

        if (!templates || templates.length === 0) {
            $body.html('<tr><td colspan="4" class="text-secondary" style="text-align:center;padding:var(--fluent-spacing-l)">No templates found</td></tr>');
            return;
        }

        templates.forEach(function (tpl) {
            var html = '<tr>' +
                '<td><strong>' + App.util.escapeHtml(tpl.name) + '</strong>' +
                '<div class="text-caption">' + App.util.escapeHtml(tpl.description || '') + '</div></td>' +
                '<td>' + App.util.formatDateTime(tpl.createdAt) + '</td>' +
                '<td><span class="status-badge ' + (tpl.active ? 'status-delivered' : 'status-exception') + '">' +
                (tpl.active ? 'Active' : 'Inactive') + '</span></td>' +
                '<td>' +
                '<button class="fluent-btn fluent-btn-sm fluent-btn-secondary btn-versions" data-id="' + tpl.id + '">Versions</button> ' +
                '<button class="fluent-btn fluent-btn-sm fluent-btn-primary btn-generate" data-id="' + tpl.id + '">Generate</button>' +
                '</td></tr>';
            $body.append(html);
        });
    }

    function init() {
        loadTemplates();

        $(document).on('click', '#btn-create-template', function () {
            $('#template-modal').addClass('active');
            $('#template-modal-title').text('Create Template');
            $('#template-form')[0].reset();
        });

        $(document).on('click', '#btn-submit-template', function () {
            var name = $('#template-name').val();
            var body = $('#template-body').val();
            if (!name || !body) {
                App.toast.warning('Name and body are required.');
                return;
            }
            App.api.post('/api/contracts/templates', {
                name: name,
                description: '',
                body: body
            }).done(function () {
                App.toast.success('Template created');
                $('#template-modal').removeClass('active');
                loadTemplates();
            }).fail(function (xhr) {
                App.toast.error(xhr.responseJSON ? xhr.responseJSON.message : 'Failed to create template');
            });
        });

        // Close modal
        $(document).on('click', '.modal-close', function () {
            $(this).closest('.modal-overlay').removeClass('active');
        });

        // View versions for a template
        $(document).on('click', '.btn-versions', function () {
            var templateId = $(this).data('id');
            window.location.href = '/contracts/preview?templateId=' + templateId;
        });

        // Generate document from template
        $(document).on('click', '.btn-generate', function () {
            var templateId = $(this).data('id');
            App.modal.confirm({
                title: 'Generate Document',
                message: '<p class="mb-m">This will create a new contract instance from the latest template version.</p>' +
                    '<div class="form-group"><label class="form-label">Signer IDs (comma-separated)</label>' +
                    '<input class="form-input" id="gen-signer-ids" placeholder="e.g. 4,5"></div>' +
                    '<div class="form-group"><label class="form-label">Linked Job ID (optional)</label>' +
                    '<input class="form-input" id="gen-job-id" type="number"></div>',
                confirmText: 'Generate'
            }).done(function () {
                var signerStr = $('#gen-signer-ids').val() || '';
                var signerIds = signerStr.split(',').map(function (s) { return parseInt(s.trim()); }).filter(function (n) { return !isNaN(n); });
                var jobId = parseInt($('#gen-job-id').val()) || null;

                // First get the latest version ID for this template
                App.api.get('/api/contracts/templates/' + templateId + '/versions').done(function (resp) {
                    if (!resp || !resp.data || resp.data.length === 0) {
                        App.toast.error('No versions found for this template.');
                        return;
                    }
                    var latestVersion = resp.data[resp.data.length - 1];
                    App.api.post('/api/contracts/instances', {
                        templateVersionId: latestVersion.id,
                        placeholderValues: {},
                        signerIds: signerIds,
                        jobId: jobId
                    }).done(function (instResp) {
                        App.toast.success('Contract document generated (ID: ' + instResp.data.id + ')');
                        if (signerIds.length > 0) {
                            App.toast.info('Signing workflow initiated. First signer notified.');
                        }
                    }).fail(function (xhr) {
                        App.toast.error(xhr.responseJSON ? xhr.responseJSON.message : 'Generation failed');
                    });
                });
            });
        });
    }

    return { init: init, loadTemplates: loadTemplates };
})();

/**
 * Contract preview sub-module.
 */
App.contracts.preview = (function () {
    'use strict';

    function init() {
        var params = new URLSearchParams(window.location.search);
        var templateId = params.get('templateId') || params.get('versionId') || params.get('id');
        if (!templateId) {
            $('#contract-preview-content').html('<p class="text-secondary">No template selected. Go back and select a template.</p>');
            return;
        }

        // Load template versions
        App.api.get('/api/contracts/templates/' + templateId + '/versions').done(function (resp) {
            if (resp && resp.data && resp.data.length > 0) {
                var latest = resp.data[resp.data.length - 1];
                $('#contract-preview-content').text(latest.bodyText || '');

                // Build placeholder form from schema
                if (latest.placeholderSchema) {
                    try {
                        var schema = JSON.parse(latest.placeholderSchema);
                        var $form = $('#placeholder-form');
                        $form.empty();
                        schema.forEach(function (field) {
                            $form.append(
                                '<div class="form-group">' +
                                '<label class="form-label">' + App.util.escapeHtml(field.label || field.key) + '</label>' +
                                '<input class="form-input placeholder-field" data-key="' + App.util.escapeHtml(field.key) + '" placeholder="Enter ' + App.util.escapeHtml(field.key) + '">' +
                                '</div>'
                            );
                        });
                    } catch (e) { /* ignore parse errors */ }
                }
            }
        });

        // Live preview refresh
        $(document).on('click', '#btn-refresh-preview', refreshPreview);
        $(document).on('input', '.placeholder-field', App.util.debounce(refreshPreview, 300));

        // Send for signing: generate instance with current placeholder values and redirect to sign page
        $(document).on('click', '#btn-send-for-signing', function () {
            App.modal.confirm({
                title: 'Send for Signing',
                message: '<div class="form-group"><label class="form-label">Signer IDs (comma-separated)</label>' +
                    '<input class="form-input" id="sign-signer-ids" placeholder="e.g. 4,5"></div>',
                confirmText: 'Send'
            }).done(function () {
                var signerStr = $('#sign-signer-ids').val() || '';
                var signerIds = signerStr.split(',').map(function (s) { return parseInt(s.trim()); }).filter(function (n) { return !isNaN(n); });
                var placeholders = {};
                $('.placeholder-field').each(function () {
                    placeholders[$(this).data('key')] = $(this).val() || '';
                });

                App.api.get('/api/contracts/templates/' + templateId + '/versions').done(function (resp) {
                    if (!resp || !resp.data || resp.data.length === 0) { App.toast.error('No versions.'); return; }
                    var latestVersion = resp.data[resp.data.length - 1];
                    App.api.post('/api/contracts/instances', {
                        templateVersionId: latestVersion.id,
                        placeholderValues: placeholders,
                        signerIds: signerIds,
                        jobId: null
                    }).done(function (instResp) {
                        App.toast.success('Contract sent for signing');
                        window.location.href = '/contracts/sign?id=' + instResp.data.id;
                    }).fail(function (xhr) {
                        App.toast.error(xhr.responseJSON ? xhr.responseJSON.message : 'Failed to generate');
                    });
                });
            });
        });
    }

    function refreshPreview() {
        var $content = $('#contract-preview-content');
        var originalText = $content.data('original') || $content.text();
        $content.data('original', originalText);
        var rendered = originalText;
        $('.placeholder-field').each(function () {
            var key = $(this).data('key');
            var val = $(this).val() || '{{' + key + '}}';
            rendered = rendered.split('{{' + key + '}}').join(val);
        });
        $content.text(rendered);
    }

    return { init: init };
})();

/**
 * Contract signing sub-module.
 */
App.contracts.sign = (function () {
    'use strict';

    var canvas, ctx, isDrawing = false;

    function initCanvas() {
        canvas = document.getElementById('signature-canvas');
        if (!canvas) return;
        ctx = canvas.getContext('2d');
        ctx.strokeStyle = '#242424';
        ctx.lineWidth = 2;
        ctx.lineCap = 'round';

        canvas.addEventListener('mousedown', function (e) {
            isDrawing = true;
            ctx.beginPath();
            ctx.moveTo(e.offsetX, e.offsetY);
        });
        canvas.addEventListener('mousemove', function (e) {
            if (!isDrawing) return;
            ctx.lineTo(e.offsetX, e.offsetY);
            ctx.stroke();
        });
        canvas.addEventListener('mouseup', function () { isDrawing = false; });
        canvas.addEventListener('mouseleave', function () { isDrawing = false; });
    }

    function clearSignature() {
        if (ctx && canvas) {
            ctx.clearRect(0, 0, canvas.width, canvas.height);
        }
    }

    function getSignatureData() {
        if (!canvas) return '';
        return canvas.toDataURL('image/png');
    }

    function init() {
        initCanvas();

        // Load contract content for review step
        var contractId = new URLSearchParams(window.location.search).get('id');
        if (contractId) {
            App.api.get('/api/contracts/instances/' + contractId).done(function (resp) {
                if (resp && resp.data) {
                    $('#contract-review-content').text(resp.data.renderedText || resp.data.snapshotBodyText || 'No content');
                }
            });
        }

        // Enable/disable sign button based on confirm checkboxes and name
        function updateSignButtonState() {
            var allChecked = $('#confirm-read').is(':checked') && $('#confirm-agree').is(':checked');
            var nameEntered = ($('#confirm-full-name').val() || '').trim().length >= 2;
            $('#btn-step-to-sign').prop('disabled', !(allChecked && nameEntered));
        }
        $(document).on('change', '#confirm-read, #confirm-agree', updateSignButtonState);
        $(document).on('input', '#confirm-full-name', updateSignButtonState);

        // Step wizard navigation
        $(document).on('click', '#btn-step-to-confirm', function () {
            $('#step-review').addClass('hidden');
            $('#step-confirm').removeClass('hidden');
        });
        $(document).on('click', '#btn-step-back-review', function () {
            $('#step-confirm').addClass('hidden');
            $('#step-review').removeClass('hidden');
        });
        $(document).on('click', '#btn-step-to-sign', function () {
            if (!$('#confirm-read').is(':checked') || !$('#confirm-agree').is(':checked')) {
                App.toast.warning('Please confirm both checkboxes before proceeding.');
                return;
            }
            $('#step-confirm').addClass('hidden');
            $('#step-sign').removeClass('hidden');
            initCanvas();
        });
        $(document).on('click', '#btn-step-back-confirm', function () {
            $('#step-sign').addClass('hidden');
            $('#step-confirm').removeClass('hidden');
        });
        $(document).on('click', '#btn-clear-signature', clearSignature);

        // Submit signature
        $(document).on('click', '#btn-submit-signature', function () {
            var sigData = getSignatureData();
            var contractId = $(this).data('contract-id') || new URLSearchParams(window.location.search).get('id');
            if (!contractId) {
                App.toast.error('No contract ID specified.');
                return;
            }
            if (!sigData || sigData === 'data:,') {
                App.toast.warning('Please draw your signature before submitting.');
                return;
            }
            App.api.post('/api/contracts/instances/' + contractId + '/sign', {
                signatureData: sigData
            }).done(function () {
                App.toast.success('Signature recorded successfully');
                setTimeout(function () { window.location.href = '/contracts'; }, 1500);
            }).fail(function (xhr) {
                App.toast.error(xhr.responseJSON ? xhr.responseJSON.message : 'Signing failed');
            });
        });
    }

    return { init: init, clearSignature: clearSignature };
})();
