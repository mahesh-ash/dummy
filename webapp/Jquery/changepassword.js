
$(function () {
  function showError(message) {
    Swal.fire({
      icon: 'error',
      title: 'Error',
      text: message || 'Something went wrong'
    });
  }

  function showSuccess(message, redirectUrl) {
    Swal.fire({
      icon: 'success',
      title: 'Success',
      text: message || 'Updated successfully',
      timer: 1600,
      showConfirmButton: false,
      willClose: () => {
        if (redirectUrl) window.location.href = redirectUrl;
      }
    });
  }

  $('#update-password-btn').on('click', function () {
    const currentPass = $('#current-password').val().trim();
    const newPass = $('#new-password').val().trim();
    const confirmPass = $('#confirm-password').val().trim();


    if (currentPass === '' || newPass === '' || confirmPass === '') {
      showError('Please fill all fields.');
      return;
    }

    if (newPass.length < 6) {
      showError('Password must be at least 6 characters.');
      return;
    }

    if (newPass !== confirmPass) {
      $('#password-error').removeClass('hidden');
      return;
    }

    $('#password-error').addClass('hidden');


    $.ajax({
      url: 'http://localhost:8080/Ecommerce_Website/ChangeServlet',
      type: 'POST',
      data: {
        currentPassword: currentPass,
        newPassword: newPass,
        confirmPassword: confirmPass
      },
      dataType: 'json',
      success: function (res) {
        if (res && res.status && res.status.toLowerCase() === 'ok') {

          showSuccess(res.message || 'Password updated successfully!', '../Html/profile.html');
        } else {

          const msg = (res && res.message) ? res.message : 'Could not update password';
          showError(msg);
        }
      },
      error: function (xhr) {
        let msg = 'Server error updating password.';
        try {

          if (xhr.responseText) {
            const parsed = JSON.parse(xhr.responseText);
            if (parsed && parsed.message) msg = parsed.message;
          }
        } catch (e) {
          if (xhr.responseText && xhr.responseText.trim()) msg = xhr.responseText.trim();
        }


        if (xhr.status === 403) {
          showError(msg || 'Current password is incorrect');
        } else if (xhr.status === 400) {
          showError(msg || 'Invalid request');
        } else {
          showError(msg);
        }
      }
    });
  });

  $('#back-btn').on('click', function () {
    window.location.href = '../Html/profile.html';
  });
});
