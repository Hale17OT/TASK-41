<%@ page contentType="text/html;charset=UTF-8" language="java" %>
</div><!-- end page-content -->
</main><!-- end app-main -->
</div><!-- end app-layout -->
<div class="global-spinner" id="global-spinner"><div class="spinner"></div></div>
<div class="toast-container" id="toast-container"></div>
<script src="/static/js/vendor/jquery-3.7.1.min.js"></script>
<script src="/static/js/common/util.js"></script>
<script src="/static/js/common/toast.js"></script>
<script src="/static/js/common/api-client.js"></script>
<script src="/static/js/common/auth.js"></script>
<script src="/static/js/common/offline-queue.js"></script>
<script src="/static/js/common/modal.js"></script>
<script src="/static/js/common/shimmer.js"></script>
<script>
$(function(){
    var path=window.location.pathname;
    $('.nav-item').each(function(){if($(this).attr('href')===path)$(this).addClass('active');});
    function pollNotifs(){
        App.api.get('/api/notifications/unread-count').done(function(r){
            if(r&&r.data!=null){var c=r.data.count||r.data||0;if(c>0){$('#notif-badge').text(c).removeClass('hidden')}else{$('#notif-badge').addClass('hidden')}}
        });
    }
    pollNotifs();setInterval(pollNotifs,30000);
    $(document).on('offlineQueue:changed',function(e,q){
        var p=q.filter(function(e){return e.status!=='synced'}).length;
        if(p>0){$('#offline-badge').text(p+' pending').removeClass('hidden')}else{$('#offline-badge').addClass('hidden')}
    });
    $('#global-search').on('keydown',function(e){if(e.key==='Enter'){var q=$(this).val().trim();if(q)window.location.href='/search?q='+encodeURIComponent(q);}});
});
</script>
