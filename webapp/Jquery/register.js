$(document).ready(function () {
  try {
    const raw = localStorage.getItem('user');
    if (raw) {
      window.location.replace('navbar.html');
      return;
    }
  } catch(e) {}

  $.getJSON("../Jquery/register.json", function (data) {
    renderForm(data.fields);
  });

  function renderForm(fields) {
  const $form = $("#registrationForm").empty();
  $form.addClass("grid grid-cols-1 md:grid-cols-2 gap-6");

  fields.forEach(field => {
    let fieldHtml = "";

    switch (field.inputType) {
      case "text":
      case "email":
      case "password":
      case "tel":
      case "number":
        fieldHtml = `
          <div class="col-span-1">
            <label for="${field.id}" class="block text-sm font-medium text-gray-700">
              ${field.title}${field.mandatory ? ' <span class="text-red-500">*</span>' : ''}
            </label>
            <input 
              type="${field.inputType}" 
              id="${field.id}" 
              name="${field.id}" 
              ${field.inputType === "tel" ? 'inputmode="numeric" pattern="[0-9]*" maxlength="10"' : ''} 
              class="mt-1 block w-full border border-gray-300 rounded px-3 py-2 focus:ring-2 focus:ring-indigo-500"
              placeholder="${field.placeholder || ''}"
              ${field.mandatory ? "required" : ""}
            >
            <p id="${field.id}-error" class="text-red-500 text-sm hidden"></p>
          </div>
        `;
        break;

      case "select":
        fieldHtml = `
          <div class="col-span-1">
            <label for="${field.id}" class="block text-sm font-medium text-gray-700">${field.title}</label>
            <select 
              id="${field.id}" 
              name="${field.id}" 
              class="mt-1 block w-full border border-gray-300 rounded px-3 py-2 focus:ring-2 focus:ring-indigo-500"
              ${field.mandatory ? "required" : ""}
            >
              ${field.options.map(opt => `<option value="${opt.value}">${opt.text}</option>`).join("")}
            </select>
          </div>
        `;
        break;

      case "textarea":
        // Address or large text spans both columns
        fieldHtml = `
          <div class="col-span-2">
            <label for="${field.id}" class="block text-sm font-medium text-gray-700">${field.title}</label>
            <textarea 
              id="${field.id}" 
              name="${field.id}" 
              rows="3"
              class="mt-1 block w-full border border-gray-300 rounded px-3 py-2 focus:ring-2 focus:ring-indigo-500"
              placeholder="${field.placeholder || ''}"
              ${field.mandatory ? "required" : ""}
            ></textarea>
          </div>
        `;
        break;

      case "button":
        fieldHtml = `
          <div class="col-span-2">
            <button 
              type="submit" 
              id="${field.id}" 
              class="w-full bg-green-600 text-white py-2 rounded-lg hover:bg-green-700"
            >
              ${field.title}
            </button>
          </div>
        `;
        break;
    }

    $form.append(fieldHtml);
  });



    $(document).off("input", "#fname");
    $(document).on("input", "#fname", function () {
      const val = this.value;
      const $err = $("#fname-error");
      if (!/^[a-zA-Z\s]*$/.test(val)) {
        $err.text("Full name must contain only letters and spaces.").removeClass("hidden");
      } else if (val.trim().length > 0 && val.trim().length < 3) {
        $err.text("Full name must be at least 3 characters long.").removeClass("hidden");
      } else {
        $err.addClass("hidden");
      }
    });

    $(document).off("input", "#email");
    $(document).on("input", "#email", function () {
      const val = $(this).val();
      const $err = $("#email-error");
      if (val.length === 0) return $err.addClass("hidden");
      if (/^[A-Z]/.test(val)) {
        $err.text("Email must not start with an uppercase letter.").removeClass("hidden");
      } else if (!/^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$/i.test(val)) {
        $err.text("Please enter a valid email address.").removeClass("hidden");
      } else {
        $err.addClass("hidden");
      }
    });

    function setActiveNav(page) {
      $(".navbar-btn").removeClass("bg-blue-600 text-white");
      $("#nav-" + page).addClass("bg-blue-600 text-white");
    }
    $("#nav-login").click(function () { window.location.href = "login.html"; });
    $("#nav-register").click(function () { window.location.href = "register.html"; });
    $("#nav-products").click(function () { window.location.href = "index.html"; });
    $("#nav-back").click(function () { window.location.href="navbar.html" });
    if (window.location.pathname.includes("login.html")) setActiveNav("login");
    else if (window.location.pathname.includes("register.html")) setActiveNav("register");
    else if (window.location.pathname.includes("index.html")) setActiveNav("products");

    $("#back-btn, #navbar-back-btn").click(function () {
      window.location.href = "navbar.html";
    });
    $("#home-btn").click(() => window.location.href='navbar.html');

    $(document).off("input", "#fname");
    $(document).on("input", "#fname", function () {
      const cleaned = this.value.replace(/[^a-zA-Z\s]/g, "");
      if (this.value !== cleaned) this.value = cleaned;
    });

    $(document).off("input", "#phone");
    $(document).on("input", "#phone", function () {
      let digits = this.value.replace(/\D/g, "");
      if (digits.length > 10) digits = digits.slice(0, 10);
      this.value = digits;
    });

    $(document).off("input", "#email");
    $(document).on("input", "#email", function () {
      const val = $(this).val();
      const $err = $("#email-error");
      if (val.length > 0 && /^[A-Z]/.test(val.charAt(0))) {
        $err.text("Email must not start with an uppercase letter").removeClass("hidden");
      } else {
        $err.addClass("hidden");
      }
    });

    $(document).off("click", ".toggle-pwd");
    $(document).on("click", ".toggle-pwd", function () {
      const target = $(this).data("target");
      const $inp = $("#" + target);
      if (!$inp.length) return;
      if ($inp.attr("type") === "password") {
        $inp.attr("type", "text");
        $(this).find(".eye-open").addClass("hidden");
        $(this).find(".eye-closed").removeClass("hidden");
      } else {
        $inp.attr("type", "password");
        $(this).find(".eye-open").removeClass("hidden");
        $(this).find(".eye-closed").addClass("hidden");
      }
    });

    fields.forEach(field => {
      if (field.validation) {
        $(`#${field.id}`).on("input", function () {
          let regex = null;
          try {
            regex = new RegExp(field.validation.pattern);
          } catch (err) {
            console.error("Invalid regex for field", field.id, field.validation.pattern, err);
            $(`#${field.id}-error`).addClass("hidden");
            return;
          }

          let val = $(this).val();
          if (field.id === "pincode") {
            if (!/^[0-9]{6}$/.test(val)) {
              $(`#${field.id}-error`).text("Pincode must be a 6-digit number").removeClass("hidden");
            } else {
              $(`#${field.id}-error`).addClass("hidden");
            }
            return;
          }

          if (!regex.test(val)) {
            $(`#${field.id}-error`).text(field.validation.message).removeClass("hidden");
          } else {
            $(`#${field.id}-error`).addClass("hidden");
          }
        });
      }
    });

    $(document).off("input", "#confirm_password");
    $(document).on("input", "#confirm_password", function () {
      const password = $("#password").val();
      const confirm = $(this).val();
      if (password != confirm) {
        $(`#confirm_password-error`).text("Passwords do not match").removeClass("hidden");
      } else {
        $(`#confirm_password-error`).addClass("hidden");
      }
    });
    $(document).off("input", "#password");
    $(document).on("input", "#password", function () {
      $("#confirm_password").trigger("input");
    });

    $form.off("submit");
    $form.on("submit", function (e) {
      e.preventDefault();
      try {
        const student = {};
        let valid = true;

        fields.forEach(field => {
          if (field.inputType === "button") return;

          let value;
          if (field.inputType === "radio") {
            value = $(`input[name="${field.id}"]:checked`).val() || "";
          } else if (field.inputType === "multiselect") {
            value = $(`#${field.id}`).val() || [];
          } else {
            value = $(`#${field.id}`).val();
            if (typeof value === "string") value = value.trim();
          }

          if (field.id === "fname") {
            if (!/^[a-zA-Z\s]+$/.test(value)) {
              alert("Full name must contain letters and spaces only.");
              valid = false;
              return false;
            }
          }

          if (field.id === "phone") {
            if (!/^[0-9]{10}$/.test(value)) {
              alert("Phone must be a 10-digit number.");
              valid = false;
              return false;
            }
          }

          if (field.id === "email") {
            if (value.length > 0 && /^[A-Z]/.test(value.charAt(0))) {
              alert("Email must not start with an uppercase letter.");
              valid = false;
              return false;
            }
          }

          if (field.id === "confirm_password") {
            const password = $("#password").val().trim();
            if (password !== value) {
              valid = false;
              alert("Passwords do not match.");
              return false;
            }
          }

          if (field.mandatory && (!value || value.length === 0)) {
            alert(`${field.title} is required`);
            valid = false;
            return false;
          }

          if (field.id === "pincode" && !/^[0-9]{6}$/.test(value)) {
            alert("Pincode must be a 6-digit number");
            valid = false;
            return false;
          }

          if (field.validation) {
            let regex = null;
            try {
              regex = new RegExp(field.validation.pattern);
            } catch (err) {
              console.error("Invalid regex on submit for", field.id, field.validation.pattern, err);
              alert("Validation configuration error. Check console.");
              valid = false;
              return false;
            }
            if (!regex.test(value)) {
              alert(field.validation.message);
              valid = false;
              return false;
            }
          }
          student[field.id] = value;
        });

        if (!valid) return;

        $.ajax({
          url: "http://localhost:8080/Ecommerce_Website/RegisterServlet",
          type: "POST",
          data: $form.serialize(),
          dataType: "json",
          success: function (response) {
            console.log(response);
            if (response.status === "success") {
              Swal.fire({
                icon: "success",
                title: "Success!",
                text: response.message || "Registered successfully!",
                showConfirmButton: false,
                timer: 2000
              }).then(() => {
                window.location.href = "login.html";
              });
            } else if (response.status === "exists") {
              Swal.fire({
                icon: "warning",
                title: "User Already Exists",
                text: response.message || "An account with this email already exists!",
                confirmButtonText: "OK"
              });
            } else {
              Swal.fire({
                icon: "error",
                title: "Oops...",
                text: response.message || "Registration failed! Please try again later."
              });
            }
          },
          error: function (xhr, status, err) {
            console.error("AJAX error:", status, err, xhr && xhr.responseText);
            Swal.fire({
              icon: "error",
              title: "Server Error",
              text: "Something went wrong. Try again later!"
            });
          }
        });

      } catch (error) {
        console.error("Submit error:", error);
        alert("Error while saving the student.");
      }
    });
  }
});
