function toast(msg, ms = 1400) {
  $('#toast').stop(true, true).text(msg).fadeIn(200).delay(ms).fadeOut(300);
}

function renderOrders(orders) {
  if (!orders || orders.length === 0) {
    $('#order-history-container').html(
      '<p class="text-center text-gray-500">You have no previous orders.</p>'
    );
    return;
  }

  let html = '';
  orders.forEach(order => {
    html += ''
      + '<div class="bg-white rounded-lg shadow-md order-card overflow-hidden">'
      + '  <div class="flex justify-between items-center p-4 order-summary">'
      + '    <div>'
      + `      <h2 class="font-semibold text-lg text-gray-800">Order #${order.orderId}</h2>`
      + `      <p class="text-gray-600 text-sm">Placed on: ${new Date(order.orderDate).toLocaleDateString()}</p>`
      + `      <p class="text-gray-700 font-bold mt-1 text-lg">Total: ₹${order.totalAmount}</p>`
      + '    </div>'
      + '    <button class="view-details bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700">'
      + '      View Details'
      + '    </button>'
      + '  </div>'
      + '  <div class="order-details mt-0 hidden">'
      + '    <h3 class="font-semibold text-gray-800 mb-3">Items in this order:</h3>'
      + '    <div class="space-y-2">';

    html += order.items.map(item => {
      return ''
        + '      <div class="flex justify-between p-2 rounded bg-white shadow-sm">'
        + `        <div><p class="font-medium text-gray-700">${item.productName}</p>`
        + `        <p class="text-gray-500 text-sm">Qty: ${item.quantity}</p></div>`
        + `        <p class="font-semibold text-gray-800">₹${item.price}</p>`
        + '      </div>';
    }).join('');

    html += ''
      + '    </div>'
      + '    <div class="mt-4 flex justify-end">'
      + `      <button class="reorder-btn bg-yellow-500 hover:bg-yellow-600 text-white px-4 py-2 rounded" data-order-id="${order.orderId}">`
      + '        Reorder'
      + '      </button>'
      + '    </div>'
      + '  </div>'
      + '</div>';
  });

  $('#order-history-container').html(html);
}

function loadOrderHistory() {
  $.ajax({
    url: 'http://localhost:8080/Ecommerce_Website/OrderHistoryServlet',
    type: 'GET',
    dataType: 'json',
    success: function (data) {
      renderOrders(data);
    },
    error: function () {
      $('#order-history-container').html(
        '<p class="text-center text-red-500">Error loading order history.</p>'
      );
    }
  });
}

function refreshBadge() {
  const user = localStorage.getItem("user");
  if (!user) {
    $('#cart-badge').text(0);
    return;
  }
  $.getJSON("http://localhost:8080/Ecommerce_Website/CartServlet")
    .done(function (data) {
      const total = Array.isArray(data) ? data.reduce(function (s, it) {
        return s + (it.qty ?? 0);
      }, 0) : 0;
      $('#cart-badge').text(total);
    })
    .fail(function () {
      $('#cart-badge').text(0);
    });
}

function updateAuthUI() {
  const logged = !!localStorage.getItem('user');

  if (logged) {
    $('#nav-login, #nav-register').hide();
    $('#nav-profile, #nav-logout, #cart-link, #nav-back').show();
  } else {
    $('#nav-login, #nav-register').show();
    $('#nav-profile, #nav-logout').hide();
    $('#cart-badge').text(0);
  }
}

$(document).ready(function () {
  updateAuthUI();
  loadOrderHistory();
  refreshBadge();

  $("#nav-login").on('click', function () {
    const user = localStorage.getItem("user");
    if (user) {
      alert("You are already logged in!");
      window.location.href = "index.html";
    } else {
      window.location.href = "login.html";
    }
  });

  $("#nav-register").on('click', function () {
    const user = localStorage.getItem("user");
    if (user) {
      alert("You are already registered and logged in!");
      window.location.href = "index.html";
    } else {
      window.location.href = "register.html";
    }
  });

  $("#cart-link").on('click', function () {
    const user = localStorage.getItem("user");
    if (!user) {
      alert("Please login first to view your cart!");
      window.location.href = "login.html";
    } else {
      window.location.href = "cart.html";
    }
  });

  $("#nav-profile").on('click', function () {
    const user = localStorage.getItem("user");
    if (!user) {
      alert("Please login first to view your profile!");
      window.location.href = "login.html";
    } else {
      window.location.href = "profile.html";
    }
  });

  $("#nav-back").on('click', function () {
    window.location.href = "navbar.html";
  });

  $('#nav-logout').off('click').on('click', function () {
    if (!confirm('Logout?')) return;
    localStorage.clear();
    localStorage.removeItem("buyNowItem");
    $('#cart-badge').text(0);
    updateAuthUI();
    toast('Logged out', 800);
    setTimeout(function () {
      window.location.href = "login.html";
    }, 600);
  });



  $(document).on('click', '.view-details', function () {
    const details = $(this).closest('.order-card').find('.order-details');
    details.slideToggle();
  });

  $(document).on('click', '.reorder-btn', function () {
    const orderId = $(this).data('order-id');
    const user = localStorage.getItem("user");
    if (!user) {
      alert('Please login to reorder.');
      window.location.href = 'login.html';
      return;
    }

    $.post('http://localhost:8080/Ecommerce_Website/ReorderServlet', { orderId: orderId })
      .done(function (response) {
        if (response && (response.status === 'ok' || response.status === 'success')) {
          toast('Items added to cart', 1000);
          setTimeout(function () {
            window.location.href = 'cart.html';
          }, 700);
        } 
        else {
          alert('Could not reorder: ' + (response && response.message ? response.message : 'Unknown error'));
        }
      })
      .fail(function () {
        alert('Failed to reorder — server error.');
      });
  });
});
