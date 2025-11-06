(function ($) {
  'use strict';

  const APP_ROOT = window.location.origin + '/Ecommerce_Website';

  function toast(msg, ms) {
    ms = ms || 1700;
    let $t = $('#toast');
    if (!$t.length) {
      $('body').append('<div id="toast" style="display:none; position:fixed; right:16px; top:16px; z-index:99999; background:#111827; color:#fff; padding:8px 12px; border-radius:6px;"></div>');
      $t = $('#toast');
    }
    $t.stop(true, true).text(msg).fadeIn(120).delay(ms).fadeOut(180);
  }

  function loadAdmin() {
    $.ajax({
      url: APP_ROOT + '/AdminServlet',
      method: 'GET',
      data: { action: 'getAdmin' },
      dataType: 'json'
    }).done(function (res) {
      if (!res || res.status === 'error' || res.error) {
        toast('Not signed in — redirecting to login...');
        setTimeout(function () {
          window.location.href = APP_ROOT + '/Html/admin_login.html';
        }, 800);
        return;
      }
      const username = res.username ?? (res.user && (res.user.name ?? res.user.username)) ?? '';
      const email = res.email ?? (res.user && res.user.email) ?? '';
      if (username) $('#username').val(username);
      if (email) $('#email').val(email);
      if ($('#adminName').length) $('#adminName').text(username || 'Admin');
    }).fail(function (jqXHR) {
      if (jqXHR.status === 401) {
        toast('Session expired or not signed in — redirecting to login...');
        setTimeout(function () {
          window.location.href = APP_ROOT + '/Html/admin_login.html';
        }, 700);
        return;
      }
      toast('Failed to load admin details');
    });
  }

  function wireNav() {
    $(document).on('click', '#backToDashboard', function (e) {
      e.preventDefault();
      window.location.href = APP_ROOT + '/Html/Admin.html';
    });

    $(document).on('click', '#profileBtn', function (e) {
      e.preventDefault();
      window.location.href = APP_ROOT + '/Html/adminprofile.html';
    });

    $(document).on('click', '#logoutBtn', function (e) {
      e.preventDefault();
      if (!confirm('Logout?')) return;
      $.ajax({
        url: APP_ROOT + '/AdminServlet',
        method: 'POST',
        data: { action: 'logout' },
        timeout: 8000
      }).always(function () {
        window.location.href = APP_ROOT + '/Html/admin_login.html';
      });
    });

    $(document).on('click', '#mobileMenuBtn', function () {
      $('#sidebar').toggleClass('hidden');
    });
  }

  function wireChangePassword() {
    $(document).on('click', '#openChangePassword', function () {
      $('#changePasswordModal').removeClass('modal-hidden').addClass('modal-shown');
    });

    $(document).on('click', '#closeChangePassword', function () {
      $('#changePasswordModal').removeClass('modal-shown').addClass('modal-hidden');
    });

    $(document).on('submit', '#changePasswordForm', function (e) {
      e.preventDefault();
      const newPwd = $('#newPassword').val();
      const confirmPwd = $('#confirmPassword').val();
      if (!newPwd || newPwd.length < 8) {
        toast('Password must be at least 8 chars');
        return;
      }
      if (newPwd !== confirmPwd) {
        toast('Passwords do not match');
        return;
      }

      $.post(APP_ROOT + '/AdminServlet', { action: 'changePassword', newPassword: newPwd }, null, 'json')
        .done(function (resp) {
          if (resp && (resp.status === 'ok' || resp.status === 'success')) {
            toast('Password updated');
            $('#changePasswordModal').removeClass('modal-shown').addClass('modal-hidden');
            $('#newPassword').val('');
            $('#confirmPassword').val('');
            return;
          }
          const msg = resp && (resp.message || resp.error) ? (resp.message || resp.error) : 'Update failed';
          toast(msg);
        })
        .fail(function (jq) {
          if (jq.status === 401) {
            toast('Not authorized — please login again');
            setTimeout(function () {
              window.location.href = APP_ROOT + '/Html/admin_login.html';
            }, 700);
            return;
          }
          toast('Server error while changing password');
        });
    });
  }

  $(function () {
    wireNav();
    wireChangePassword();
    loadAdmin();
  });

})(jQuery);
