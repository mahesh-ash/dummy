$(document).ready(function () {


  let cartItems = [];

  function normalize(it) {
    return {
      productId: it.productId ?? it.id ?? it.product_id,
      name: it.name ?? it.productName ?? '',
      price: parseFloat(it.price ?? 0) || 0,
      qty: parseInt(it.qty ?? 1, 10) || 1,
      image: it.image ?? it.imageUrl ?? '',
      checked: !!it.checked
    };
  }

  function updateTotalsAndHeaderCheckbox() {
    const selectedTotal = cartItems.reduce((sum, it) => sum + (it.checked ? it.price * it.qty : 0), 0);
    $('#selectedTotal').text(selectedTotal.toFixed(2));
    const grandTotal = cartItems.reduce((sum, it) => sum + it.price * it.qty, 0);
    $('#grandTotal').text(grandTotal.toFixed(2));
    if (cartItems.length <= 1) {
      $('#select-all').hide();
    } 
	else {
      $('#select-all').show();
      const allChecked = cartItems.every(it => it.checked);
      $('#select-all').prop('checked', allChecked);
    }
  }

  function renderCartFull(itemsArg) {
    if (Array.isArray(itemsArg)) cartItems = itemsArg;
    if (!cartItems || cartItems.length === 0) {
      $('#cart-container').html('<p class="muted">Your cart is empty.</p>');
      updateTotalsAndHeaderCheckbox();
      return;
    }

    let html = '<table class="w-full"><thead><tr class="border-b">';
    html += '<th class="p-2 text-center"><input type="checkbox" id="select-all" class="cart-check"></th>';
    html += '<th class="text-left p-2">Product</th><th class="p-2">Price</th><th class="p-2">Qty</th><th class="p-2">Total</th><th class="p-2"></th>';
    html += '</tr></thead><tbody>';

    cartItems.forEach(it => {
      const pid = it.productId;
      const total = (it.price * it.qty) || 0;
      const image = it.image || 'https://via.placeholder.com/60';
      const checkedAttr = it.checked ? 'checked' : '';
      html += `<tr class="border-b cart-row" data-id="${pid}">
        <td class="p-2 text-center"><input type="checkbox" class="cart-check item-check" data-id="${pid}" ${checkedAttr}></td>
        <td class="p-2 flex items-center"><img src="${image}" style="width:60px;height:60px" class="mr-3 rounded">${it.name || 'Unknown'}</td>
        <td class="p-2">₹${it.price.toFixed(2)}</td>
        <td class="p-2 flex items-center justify-center space-x-2">
          <button class="qty-decr qty-btn" data-id="${pid}">−</button>
          <span class="qty-val" data-id="${pid}">${it.qty}</span>
          <button class="qty-incr qty-btn" data-id="${pid}">+</button>
        </td>
        <td class="p-2">₹${total.toFixed(2)}</td>
        <td class="p-2"><button class="remove bg-red-500 text-white px-3 py-1 rounded hover:bg-red-600" data-id="${pid}">Remove</button></td>
      </tr>`;
    });

    html += '</tbody></table>';
    $('#cart-container').html(html);
    updateTotalsAndHeaderCheckbox();
    updateCartBadge();
  }

  function loadCart() {
    $.getJSON('http://localhost:8080/Ecommerce_Website/CartServlet')
      .done(function (data) {
        cartItems = Array.isArray(data) ? data.map(normalize) : [];
        const buyNowItem = localStorage.getItem('buyNowItem');
        if (buyNowItem) {
          const buyObj = JSON.parse(buyNowItem);
          cartItems.forEach(it => {
            it.checked = (String(it.productId) === String(buyObj.productId));
          });
          localStorage.removeItem('buyNowItem');
        }
        renderCartFull(cartItems);
      })
      .fail(function () {
        $('#cart-container').html('<p class="text-red-500">Failed to load cart.</p>');
      });
  }

  function saveCartLocal(cartItems) {
    const localCart = cartItems.map(it => ({ productId: it.productId, name: it.name, qty: it.qty }));
    localStorage.setItem('cart', JSON.stringify(localCart));
    updateCartBadge();
  }

  function updateCartBadge() {
    $.getJSON('http://localhost:8080/Ecommerce_Website/CartServlet')
      .done(function (serverData) {
        const serverCount = Array.isArray(serverData) ? serverData.reduce((sum, item) => sum + (item.qty || 1), 0) : 0;
        $('#cart-badge').text(serverCount);
      })
      .fail(function () { $('#cart-badge').text('0'); });
  }

  const user = localStorage.getItem("user");
  if (!user) {
    alert("Please login to view your cart");
    window.location.href = "../Html/login.html";
    return;
  }

  loadCart();

  $(document).on('click', '.qty-incr', function (e) {
    e.stopPropagation();
    const pid = $(this).data('id');
    const idx = cartItems.findIndex(x => String(x.productId) === String(pid));
    if (idx < 0) return;
    cartItems[idx].qty += 1;
    renderCartFull(cartItems);
    saveCartLocal(cartItems);
    $.post('http://localhost:8080/Ecommerce_Website/CartServlet', {
       action: 'update',
        productId: pid, 
        qty: cartItems[idx].qty
       });
  });

  $(document).on('click', '.qty-decr', function (e) {
    e.stopPropagation();
    const pid = $(this).data('id');
    const idx = cartItems.findIndex(x => String(x.productId) === String(pid));
    if (idx < 0) return;
    cartItems[idx].qty = Math.max(1, cartItems[idx].qty - 1);
    renderCartFull(cartItems);
    saveCartLocal(cartItems);
    $.post('http://localhost:8080/Ecommerce_Website/CartServlet', { action: 'update', productId: pid, qty: cartItems[idx].qty });
  });

  $(document).on('click', '.remove', function (e) {
    e.stopPropagation();
    const pid = $(this).data('id');
    if (!confirm("Are you sure you want to remove this item?")) return;
    $.post('http://localhost:8080/Ecommerce_Website/CartServlet', { action: 'remove', productId: pid })
      .done(function (data) {
        cartItems = Array.isArray(data) ? data.map(normalize) : cartItems.filter(x => String(x.productId) !== String(pid));
        renderCartFull(cartItems);
        saveCartLocal(cartItems);
      });
  });

  $(document).on('change', '.item-check', function () {
    const pid = $(this).data('id');
    const idx = cartItems.findIndex(x => String(x.productId) === String(pid));
    if (idx >= 0) {
      cartItems[idx].checked = $(this).is(':checked');
      updateTotalsAndHeaderCheckbox();
    }
  });

  $(document).on('change', '#select-all', function () {
    const checked = $(this).is(':checked');
    cartItems.forEach(it => it.checked = checked);
    renderCartFull(cartItems);
    saveCartLocal(cartItems);
  });

  $('#checkout-btn').click(function () {
    const checkedRows = $('.item-check:checked');
    if (checkedRows.length === 0) {
      alert('Please select one or more items to checkout.');
      return;
    }
    const selectedItems = [];
    let total = 0;
    checkedRows.each(function () {
      const row = $(this).closest('.cart-row');
      const id = parseInt(row.data('id'), 10);
      const name = row.find('td:nth-child(2)').text().trim();
      const price = parseFloat(row.find('td:nth-child(3)').text().replace('₹', '').trim());
      const qty = parseInt(row.find('.qty-val').text(), 10);
      const subtotal = parseFloat(row.find('td:nth-child(5)').text().replace('₹', '').trim());
      selectedItems.push({ productId: id, name, price, qty, total: subtotal });
      total += subtotal;
    });
    localStorage.setItem('checkoutItems', JSON.stringify(selectedItems));
    window.location.href = '../Html/payment.html?amount=' + total.toFixed(2);
  });

  $('#proceed-btn').click(function () {
    const checkedRows = $('.item-check:checked');
    if (checkedRows.length === 0) {
      alert('Please select one or more items to proceed.');
      return;
    }

    const user = JSON.parse(localStorage.getItem('user') || '{}');
    $('#fullname').val(user.name || '');
    $('#phone').val(user.mobile || '');
    $('#address').val(user.address || '');
    $('#pincode').val(user.pinCode || '');
    $('#address-modal').removeClass('hidden');

    const selectedItems = [];
    let total = 0;
    checkedRows.each(function () {
      const row = $(this).closest('.cart-row');
      const id = parseInt(row.data('id'), 10);
      const name = row.find('td:nth-child(2)').text().trim();
      const price = parseFloat(row.find('td:nth-child(3)').text().replace('₹', '').trim());
      const qty = parseInt(row.find('.qty-val').text(), 10);
      const subtotal = parseFloat(row.find('td:nth-child(5)').text().replace('₹', '').trim());
      selectedItems.push({ productId: id, name, price, qty, total: subtotal });
      total += subtotal;
    });
    localStorage.setItem('checkoutItems', JSON.stringify(selectedItems));
    localStorage.setItem('checkoutAmount', total.toFixed(2));
  });

  $('#cancel-modal').click(() => {
    $('#address-modal').addClass('hidden');
    $('#form-error').addClass('hidden').text('');
  });

  $('#confirm-address').click(function () {
    const name = $('#fullname').val().trim();
    const phone = $('#phone').val().trim();
    const address = $('#address').val().trim();
    const pincode = $('#pincode').val().trim();
    $('#form-error').addClass('hidden').text('');
    if (!name || !phone || !address || !pincode) {
      $('#form-error').removeClass('hidden').text(' All fields are mandatory. Please fill out every detail.');
      return;
    }
    const phoneRegex = /^[6-9]\d{9}$/;
    const pincodeRegex = /^\d{6}$/;
    if (!phoneRegex.test(phone)) {
      $('#form-error').removeClass('hidden').text(' Please enter a valid 10-digit phone number.');
      $('#phone').focus();
      return;
    }
    if (!pincodeRegex.test(pincode)) {
      $('#form-error').removeClass('hidden').text('Please enter a valid 6-digit pincode.');
      $('#pincode').focus();
      return;
    }
    const updatedUser = { name, mobile: phone, address, pinCode: pincode };
    localStorage.setItem('deliveryDetails', JSON.stringify(updatedUser));
    const total = localStorage.getItem('checkoutAmount');
    window.location.href = '../Html/payment.html?amount=' + total;
  });

  $('#nav-back').click(() => window.location.href = 'navbar.html');
  
  $('#nav-logout').click(() => {
    localStorage.clear();
    localStorage.removeItem('cart');
    localStorage.removeItem('buyNowItem');
    $('#cart-badge').text(0);
    window.location.href = '../Html/login.html';
  });
  
  $("#home-btn").click(() => window.location.href = 'navbar.html');
  
  $(window).on('focus', loadCart);
});
