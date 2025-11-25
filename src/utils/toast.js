import Swal from 'sweetalert2';

export const showToast = (message, type = 'success', duration = 1400) => {
  const Toast = Swal.mixin({
    toast: true,
    position: 'top-end',
    showConfirmButton: false,
    timer: duration,
    timerProgressBar: true,
    didOpen: (toast) => {
      toast.addEventListener('mouseenter', Swal.stopTimer);
      toast.addEventListener('mouseleave', Swal.resumeTimer);
    },
  });

  Toast.fire({
    icon: type,
    title: message,
  });
};

export const showError = (message) => {
  Swal.fire({
    icon: 'error',
    title: 'Error',
    text: message || 'Something went wrong',
  });
};

export const showSuccess = (message, callback) => {
  Swal.fire({
    icon: 'success',
    title: 'Success',
    text: message || 'Operation completed successfully',
    timer: 1600,
    showConfirmButton: false,
  }).then(() => {
    if (callback) callback();
  });
};

export const showConfirm = (message, onConfirm) => {
  Swal.fire({
    title: 'Are you sure?',
    text: message,
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#10b981',
    cancelButtonColor: '#ef4444',
    confirmButtonText: 'Yes',
  }).then((result) => {
    if (result.isConfirmed && onConfirm) {
      onConfirm();
    }
  });
};