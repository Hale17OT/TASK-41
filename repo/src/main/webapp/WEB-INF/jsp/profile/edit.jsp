<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DispatchOps - My Profile</title>
    <link rel="stylesheet" href="/static/css/fluent-tokens.css">
    <link rel="stylesheet" href="/static/css/app.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/layout/_header.jsp" %>

<div class="page-title">
    <h1>My Profile</h1>
</div>

<div class="flex gap-l">
    <!-- Left Column: Profile Form -->
    <div style="flex:1">
        <!-- Avatar Upload Section -->
        <div class="card mb-m">
            <h2 class="mb-m">Avatar</h2>
            <div class="flex items-center gap-m">
                <div id="avatar-preview" style="width:80px;height:80px;border-radius:var(--fluent-radius-circular);background:var(--fluent-color-neutral-bg-subtle);display:flex;align-items:center;justify-content:center;font-size:32px;color:var(--fluent-color-neutral-fg-2);overflow:hidden">
                    <span id="avatar-placeholder">&#9787;</span>
                    <img id="avatar-image" src="" alt="Avatar" style="width:100%;height:100%;object-fit:cover;display:none">
                </div>
                <div>
                    <input type="file" id="avatar-file" name="avatar" accept=".jpg,.jpeg,.png,.pdf" style="display:none">
                    <button class="fluent-btn fluent-btn-secondary" id="btn-upload-avatar">Upload Photo</button>
                    <p class="text-caption mt-s">JPG or PNG (max 5MB). PDF documents (max 10MB).</p>
                </div>
            </div>
        </div>

        <!-- Profile Details Form -->
        <div class="card">
            <h2 class="mb-m">Profile Details</h2>
            <form id="profile-form">
                <div class="form-group">
                    <label class="form-label" for="profile-display-name">Display Name</label>
                    <input type="text" class="form-input" id="profile-display-name" name="displayName"
                           value="${currentUser.displayName}" required>
                </div>
                <div class="form-group">
                    <label class="form-label" for="profile-email">Email</label>
                    <input type="email" class="form-input" id="profile-email" name="email"
                           placeholder="you@example.com">
                </div>
                <div class="form-group">
                    <label class="form-label" for="profile-phone">Phone</label>
                    <input type="tel" class="form-input" id="profile-phone" name="phone"
                           placeholder="+1 (555) 000-0000">
                </div>
                <div class="form-group">
                    <label class="form-label" for="profile-bio">Bio</label>
                    <textarea class="form-textarea" id="profile-bio" name="bio" rows="4"
                              placeholder="Tell others about yourself..."></textarea>
                </div>
                <button type="submit" class="fluent-btn fluent-btn-primary" id="btn-save-profile">Save Changes</button>
            </form>
        </div>
    </div>

    <!-- Right Column: Visibility Settings -->
    <div style="width:320px;flex-shrink:0">
        <div class="card">
            <h2 class="mb-m">Visibility Settings</h2>
            <p class="text-caption mb-m">Control who can see your profile information.</p>

            <div class="form-group">
                <label class="form-label">Email visibility</label>
                <select class="form-select vis-tier-select" data-field="email" id="vis-email">
                    <option value="1">Public (everyone)</option>
                    <option value="2" selected>Internal (staff)</option>
                    <option value="3">Management only</option>
                    <option value="4">Admin only</option>
                </select>
            </div>
            <div class="form-group">
                <label class="form-label">Phone visibility</label>
                <select class="form-select vis-tier-select" data-field="phone" id="vis-phone">
                    <option value="1">Public (everyone)</option>
                    <option value="2">Internal (staff)</option>
                    <option value="3" selected>Management only</option>
                    <option value="4">Admin only</option>
                </select>
            </div>
            <div class="form-group">
                <label class="form-label">Bio visibility</label>
                <select class="form-select vis-tier-select" data-field="bio" id="vis-bio">
                    <option value="1" selected>Public (everyone)</option>
                    <option value="2">Internal (staff)</option>
                    <option value="3">Management only</option>
                    <option value="4">Admin only</option>
                </select>
            </div>
            <div class="form-group">
                <label class="form-label">Address visibility</label>
                <select class="form-select vis-tier-select" data-field="address" id="vis-address">
                    <option value="1">Public (everyone)</option>
                    <option value="2">Internal (staff)</option>
                    <option value="3" selected>Management only</option>
                    <option value="4">Admin only</option>
                </select>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/layout/_footer.jsp" %>
<script src="/static/js/profile.js"></script>
<script>
$(function() {
    if (typeof App.profile !== 'undefined') {
        App.profile.init();
    }
});
</script>
</body>
</html>
