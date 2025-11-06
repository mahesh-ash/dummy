$(document).ready(function () {
  function showToast(type, message) {
    const bgColor = type === "success" ? "bg-green-500" : "bg-red-500";
    const $toast = $(`
      <div class="fixed top-5 right-5 z-50 px-4 py-3 text-white text-sm font-semibold rounded-lg shadow-lg ${bgColor} transition-all duration-500 opacity-0 translate-x-10">
        ${message}
      </div>
    `);
    $("body").append($toast);
    setTimeout(() => $toast.removeClass("opacity-0 translate-x-10"), 100);
    setTimeout(() => {
      $toast.addClass("opacity-0 translate-x-10");
      setTimeout(() => $toast.remove(), 500);
    }, 3000);
  }

  function makeToken() {
    try {
      const a = new Uint32Array(4);
      window.crypto.getRandomValues(a);
      return Array.from(a).map(n => n.toString(36)).join('-') + '-' + Date.now().toString(36);
    } catch (e) {
      return Math.random().toString(36).slice(2) + '-' + Date.now().toString(36);
    }
  }

  function setActiveSession(userEmail) {
    const token = makeToken();
    const payload = { token: token, userEmail: userEmail || null, ts: Date.now() };
    try {
      sessionStorage.setItem('currentSessionToken', token);
      localStorage.setItem('activeSession', JSON.stringify(payload));
    } catch (e) { 
      console.warn('Failed to persist activeSession', e);
     }
  }

  function clearActiveSessionIfMine() {
    try {
      const active = JSON.parse(localStorage.getItem('activeSession') || 'null');
      const myToken = sessionStorage.getItem('currentSessionToken');
      if (active && myToken && active.token === myToken) {
        localStorage.removeItem('activeSession');
      }
      sessionStorage.removeItem('currentSessionToken');
    } catch (e) {
      try { 
        sessionStorage.removeItem('currentSessionToken'); 
      } catch(e) {

      }
    }
  }

  function getActiveSession() {
    try { return JSON.parse(localStorage.getItem('activeSession') || 'null');

     } catch (e) {
       return null;
       }
  }

  function alreadyLoggedInRedirect() {
    try {
      const rawUser = localStorage.getItem('user');
      if (rawUser) { window.location.replace('navbar.html'); return true; }
      const active = getActiveSession();
      if (active && active.userEmail) {
         window.location.replace('navbar.html'); 
         return true;
         }
    } catch (e) {

    }
    return false;
  }

  window.addEventListener('storage', function (ev) {
    if (ev.key === 'activeSession') {
      const newVal = ev.newValue;
      if (newVal) {
        if (window.location.pathname.includes('login.html')) {
          try { window.location.replace('navbar.html'); } catch (_) {}
        }
      }
    }
    if (ev.key === 'user') {
      if (ev.newValue) {
        if (window.location.pathname.includes('login.html')) {
          try { 
            window.location.replace('navbar.html'); 
          } catch(e) {
            return null;
          }
        }
      }
    }
  });

  window.addEventListener('beforeunload', function () { clearActiveSessionIfMine(); });

  if (alreadyLoggedInRedirect()) return;


  $.getJSON("../Jquery/Login.json", function (data) {
    renderStudentsData(data.Inputfields);
  });

  function validateField(field) {
    const $input = $("#" + field.id);
    const value = $input.val().trim();
    let isValid = true;
    $input.next(".text-red-500").remove();

    if (field.mandatory && field.enable) {
      if (!value) {
        isValid = false;
        $input.addClass("border-red-500").removeClass("border-indigo-500");
        $input.after(`<p class="text-red-500 text-sm mt-1">${field.title} is required.</p>`);
      } 
      else if (field.inputType === "email") {
		const emailPattern = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
        if (!emailPattern.test(value)) {
          isValid = false;
          $input.addClass("border-red-500").removeClass("border-indigo-500");
          $input.after('<p class="text-red-500 text-sm mt-1">Please enter a valid email address.</p>');
        }
      } 
      else if (field.id == "password") {
        const passwordPattern = /^(?=.*[A-Z])(?=.*[0-9])(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{6,}$/;
        if (!passwordPattern.test(value)) {
          isValid = false;
          $input.addClass("border-red-500").removeClass("border-indigo-500");
          $input.after('<p class="text-red-500 text-sm mt-1">Password must contain at least 6 characters, one uppercase, one number, and one special character</p>');
        }
      }
    }

    if (isValid) { 
      $input.removeClass("border-red-500").addClass("border-indigo-500"); 

    }
    return isValid;
  }

  function renderStudentsData(fields) {
    const $form = $("#registrationForm").empty();

    fields.forEach((field) => {
      let fieldHtml = "";
      const requiredAttr = field.mandatory ? "required" : "";
      const disabledAttr = !field.enable ? "disabled" : "";
      const commonClasses = `w-full px-4 py-2 border rounded-lg shadow-sm focus:outline-none transition ${!field.enable ? "bg-gray-200 cursor-not-allowed" : "bg-white"} border-gray-300`;

      switch (field.inputType) {
        case "text":
        case "email":
          fieldHtml = `
            <div class="relative">
              <label class="block text-gray-700 font-semibold mb-1">${field.title}${field.mandatory ? '<span class="text-red-500">*</span>' : ""}</label>
              <input type="${field.inputType}" id="${field.id}" name="${field.id}" class="${commonClasses}" placeholder="Enter your ${field.title}" ${requiredAttr} ${disabledAttr} />
            </div>`;
          break;
        case "button":
          fieldHtml = `<button id="${field.id}" type="submit" class="w-full bg-indigo-600 text-white font-semibold py-3 rounded-lg shadow hover:bg-indigo-700 transition duration-300" ${disabledAttr}>${field.title}</button>`;
          break;
      }
      $form.append(`<div class="mb-6">${fieldHtml}</div>`);
    });

    fields.forEach((field) => {
      if (["text", "email", "textarea", "phone", "password"].includes(field.inputType)) {
        $("#" + field.id).on("input change blur", function () {
          validateField(field);
        });
      }
    });

    function setActiveNav(page) {
      $(".navbar-btn").removeClass("bg-blue-600 text-white");
      $("#nav-" + page).addClass("bg-blue-600 text-white");
    }

    $("#nav-login").click(() => window.location.href = "login.html");
    $("#nav-register").click(() => window.location.href = "register.html");
    $("#nav-back").click(() => window.location.href = "navbar.html");
    $("#home-btn").click(() => window.location.href = "navbar.html");

    if (window.location.pathname.includes("login.html")) setActiveNav("login");
    else if (window.location.pathname.includes("register.html")) setActiveNav("register");
    else if (window.location.pathname.includes("navbar.html")) setActiveNav("products");

    $("#back-btn").click(() => window.location.href = "navbar.html");

    $form.on("submit", function (e) {
      e.preventDefault();
      let isValid = true;
      $(".text-red-500").remove();

      fields.forEach((field) => {
        if (!validateField(field)) isValid = false;
      });

      if (!isValid) return;

      const Data = $form.serialize();

      $.ajax({
        url: "http://localhost:8080/Ecommerce_Website/LoginServlet",
        type: "POST",
        data: Data,
        dataType: "json",
        success: function (response) {
          if (response && response.status === "success") {
            let storedUser = null;
            if (response.user && typeof response.user === "object") {
              try {
                localStorage.setItem("user", JSON.stringify(response.user));
                storedUser = response.user;
              } catch (e) {
                console.error("Failed to save user in localStorage", e);
              }
            }
            const userEmail = (storedUser && (storedUser.email || storedUser.userEmail)) || (new URLSearchParams(Data).get('email')) || null;
            setActiveSession(userEmail);

            showToast("success", "Login successful! Redirecting...");
            window.location.href = response.redirectUrl || "navbar.html";
          } else {
            showToast("error", response && response.message ? response.message : "Check your email and password!");
            if (response && response.redirectUrl) window.location.href = response.redirectUrl;
          }
        },
        error: function () { showToast("error", "Server error. Please try again later."); }
      });

      $form[0].reset();
    });
  }


  function buildForgotUi() {
    const $form = $('#forgotPasswordForm');
    $form.empty();

    const step1 = `
      <div id="fp-step1" class="space-y-3">
        <input type="email" id="fpEmail" class="w-full border rounded-lg px-4 py-2" placeholder="Enter your registered email" required>
        <div class="flex gap-2">
          <button id="sendOtpBtn" type="button" class="w-1/2 bg-indigo-600 text-white py-2 rounded-lg">Send OTP</button>
          <button id="cancelForgot" type="button" class="w-1/2 bg-gray-400 text-white py-2 rounded-lg">Cancel</button>
        </div>
      </div>
    `;
    const step2 = `
      <div id="fp-step2" class="hidden space-y-3">
        <input type="text" id="fpOtp" class="w-full border rounded-lg px-4 py-2" placeholder="Enter OTP" required>
        <input type="password" id="fpNewPassword" class="w-full border rounded-lg px-4 py-2" placeholder="New Password" required>
        <input type="password" id="fpConfirmPassword" class="w-full border rounded-lg px-4 py-2" placeholder="Confirm Password" required>
        <div class="flex gap-2">
          <button id="resetPwdBtn" type="button" class="w-1/2 bg-indigo-600 text-white py-2 rounded-lg">Reset Password</button>
          <button id="cancelForgot2" type="button" class="w-1/2 bg-gray-400 text-white py-2 rounded-lg">Cancel</button>
        </div>
      </div>
    `;
    $form.append(step1).append(step2);
  }

  $('#forgot-password-link').on('click', function () {
    $('#registrationForm').hide();
    $('#forgot-password-link').hide();
    buildForgotUi();
    $('#forgotPasswordWrapper').removeClass('hidden');
  });


  $(document).on('click', '#cancelForgot, #cancelForgot2', function () {
    $('#forgotPasswordWrapper').addClass('hidden');
    $('#forgotPasswordForm').empty();
    $('#registrationForm').show();
    $('#forgot-password-link').show();
  });


  $(document).on('click', '#sendOtpBtn', function (e) {
    e.preventDefault();
    const email = $('#fpEmail').val().trim();
    if (!email) { showToast('error', 'Please enter your registered email'); return; }
    $.ajax({
      url: 'http://localhost:8080/Ecommerce_Website/ForgotPasswordServlet',
      type: 'POST',
      dataType: 'json',
      data: { action: 'sendOtp', email: email },
      success: function (res) {
        try { if (typeof res === 'string') res = JSON.parse(res); } catch(e){}
        if (res && (res.status === 'ok' || res.status === 'success')) {
          showToast('success', (res.message || 'OTP sent (or test mode). Check email or use global OTP.'));
          $('#fp-step1').addClass('hidden');
          $('#fp-step2').removeClass('hidden');
          $('#fpOtp').focus();
        } else {
          showToast('error', (res && res.message) ? res.message : 'Failed to send OTP');
        }
      },
      error: function () { showToast('error', 'Server error sending OTP'); }
    });
  });


  $(document).on('click', '#resetPwdBtn', function (e) {
    e.preventDefault();
    const email = $('#fpEmail').val().trim();
    const otp = $('#fpOtp').val().trim();
    const newPassword = $('#fpNewPassword').val().trim();
    const confirmPassword = $('#fpConfirmPassword').val().trim();
    if (!email || !otp || !newPassword || !confirmPassword) { showToast('error', 'All fields are required'); return; }
    if (newPassword !== confirmPassword) { showToast('error', 'Passwords do not match'); return; }

    $.ajax({
      url: 'http://localhost:8080/Ecommerce_Website/ForgotPasswordServlet',
      type: 'POST',
      dataType: 'json',
      data: { action: 'reset', email: email, otp: otp, newPassword: newPassword, confirmPassword: confirmPassword },
      success: function (res) {
        try { if (typeof res === 'string') res = JSON.parse(res); } catch(e){}
        if (res && res.status === 'success') {
          showToast('success', res.message || 'Password updated');
          setTimeout(function(){
            $('#cancelForgot').click();

          }, 900);
        } else {
          showToast('error', (res && res.message) ? res.message : 'Reset failed');
        }
      },
      error: function () { showToast('error', 'Server error while resetting password'); }
    });
  });


  $('#forgotPasswordForm').on('submit', function(e) {
    e.preventDefault();
    const email = $('#fpEmail').val() ? $('#fpEmail').val().trim() : '';
    const newPassword = $('#fpNewPassword').val() ? $('#fpNewPassword').val().trim() : '';
    const confirmPassword = $('#fpConfirmPassword').val() ? $('#fpConfirmPassword').val().trim() : '';
    const otp = $('#fpOtp').val() ? $('#fpOtp').val().trim() : '';

    if (!email || !newPassword || !confirmPassword || !otp) { showToast('error', 'All fields required'); return; }
    if (newPassword !== confirmPassword) { showToast('error', 'Passwords do not match'); return; }

    $.ajax({
      url: 'http://localhost:8080/Ecommerce_Website/ForgotPasswordServlet',
      type: 'POST',
      dataType: 'json',
      data: {
         action: 'reset', email: email, otp: otp, newPassword: newPassword, confirmPassword: confirmPassword },
      success: function (res) {
        try { if (typeof res === 'string') res = JSON.parse(res); } catch(e){}
        if (res && res.status === 'success') {
          showToast('success', res.message || 'Password updated');
          setTimeout(function(){ $('#cancelForgot').click(); }, 900);
        } else {
          showToast('error', (res && res.message) ? res.message : 'Reset failed');
        }
      },
      error: function () { showToast('error', 'Server error while resetting password'); }
    });
  });


  $('#cancelForgot').on('click', function () {
    $('#forgotPasswordWrapper').addClass('hidden');
    $('#forgotPasswordForm').empty();
    $('#registrationForm').show();
    $('#forgot-password-link').show();
  });

});