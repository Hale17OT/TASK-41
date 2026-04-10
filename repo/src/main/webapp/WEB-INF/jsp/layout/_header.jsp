<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  This partial is included at the top of every page body.
  It renders the sidebar + header chrome.
  The page content follows this include.
--%>
<div class="connectivity-banner" id="connectivity-banner">You are offline. Actions will be queued locally.</div>
<div class="app-layout">
<aside class="app-sidebar">
    <div class="sidebar-logo">DispatchOps</div>
    <nav class="sidebar-nav">
        <a href="/dashboard" class="nav-item" data-page="dashboard"><span>&#9632;</span> Dashboard</a>
        <c:if test="${currentUser.role == 'ADMIN' || currentUser.role == 'OPS_MANAGER' || currentUser.role == 'DISPATCHER'}">
        <a href="/fulfillment" class="nav-item" data-page="fulfillment"><span>&#9654;</span> Fulfillment</a>
        </c:if>
        <c:if test="${currentUser.role != 'COURIER'}">
        <a href="/tasks" class="nav-item" data-page="tasks"><span>&#9745;</span> Tasks</a>
        </c:if>
        <a href="/credibility" class="nav-item" data-page="credibility"><span>&#9733;</span> Credibility</a>
        <c:if test="${currentUser.role == 'ADMIN' || currentUser.role == 'OPS_MANAGER'}">
        <a href="/contracts" class="nav-item" data-page="contracts"><span>&#9998;</span> Contracts</a>
        </c:if>
        <c:if test="${currentUser.role != 'COURIER'}">
        <a href="/payments" class="nav-item" data-page="payments"><span>&#36;</span> Payments</a>
        </c:if>
        <a href="/profile" class="nav-item" data-page="profile"><span>&#9787;</span> Profile</a>
        <c:if test="${currentUser.role == 'ADMIN'}">
        <div style="border-top:1px solid var(--fluent-color-neutral-stroke-2);margin:var(--fluent-spacing-s) 0"></div>
        <a href="/admin/users" class="nav-item" data-page="admin"><span>&#9881;</span> Admin</a>
        </c:if>
        <c:if test="${currentUser.role == 'AUDITOR' || currentUser.role == 'ADMIN'}">
        <a href="/payments?tab=reconciliation" class="nav-item" data-page="reconciliation"><span>&#128200;</span> Reconciliation</a>
        </c:if>
    </nav>
</aside>
<main class="app-main">
<header class="app-header">
    <div class="header-search">
        <input type="text" id="global-search" placeholder="Search..." autocomplete="off">
    </div>
    <div class="header-actions">
        <span class="status-badge sync-pending hidden" id="offline-badge"></span>
        <div class="notification-bell" id="notification-bell" onclick="window.location.href='/notifications'">
            <span style="font-size:18px">&#128276;</span>
            <span class="notification-badge hidden" id="notif-badge">0</span>
        </div>
        <div class="user-menu">
            <span id="user-display-name"><c:out value="${currentUser.displayName}"/></span>
            <span class="text-caption">(<c:out value="${currentUser.role}"/>)</span>
        </div>
        <button class="fluent-btn fluent-btn-secondary fluent-btn-sm" onclick="App.auth.logout()">Logout</button>
    </div>
</header>
<div class="page-content">

<%-- CSRF token for JS api-client --%>
<meta name="_csrf" content="<%= session != null ? session.getAttribute("_csrfToken") != null ? session.getAttribute("_csrfToken") : "" : "" %>">
<meta name="_csrf_header" content="X-CSRF-TOKEN">

<%-- Current user JSON for JS - serialized server-side to avoid HTML entity mangling --%>
<script type="application/json" id="current-user-data">
<c:if test="${currentUserJson != null}">${currentUserJson}</c:if>
</script>
