
$(document).ready(function() {
  let userData = null;
  const localPlaceholder = '../assets/placeholder-120.png';

  function toast(msg, ms = 1400) {
    if ($('#toast').length === 0) $('body').append('<div id="toast" style="position:fixed;top:20px;right:20px;display:none;z-index:10000;background:#111827;color:#fff;padding:8px 12px;border-radius:6px;"></div>');
    $('#toast').stop(true,true).text(msg).fadeIn(200).delay(ms).fadeOut(300);
  }

  function safeParse(str) {
    if (typeof str !== 'string' || str.trim() === '' || str === 'undefined') return null;
    try { return JSON.parse(str); } catch (err) { console.warn('safeParse failed:', err, str); return null; }
  }

  function updateCartBadge() {
    const fallback = (JSON.parse(localStorage.getItem('cart') || '[]') || []).reduce((s,i)=>s+(i.qty||1),0);
    $.ajax({ url: 'http://localhost:8080/Ecommerce_Website/CartServlet', 
      method: 'GET', 
      dataType: 'json', 
      timeout: 4000 
    })
      .done(function(serverData) {
        if (!serverData || !Array.isArray(serverData)) { 
          $('#cart-badge').text(fallback); 
          return; 
        }
        const serverCount = serverData.reduce((sum, item) => sum + (item.qty || 1), 0);
        $('#cart-badge').text(serverCount);
      }).fail(function() {
        $('#cart-badge').text(fallback);
      });
  }

  const rawUser = localStorage.getItem('user');
  const user = safeParse(rawUser);

  if (!user) {
    alert("Please login to view profile");
    $('#side-name').text('Guest');
    updateCartBadge();
    $('#profile-section').remove();
    $('#wishlist-panel').show();
    $('#wishlist-grid').html('<p class="text-center text-gray-500">Please <a href="../Html/login.html" class="text-green-600">login</a> to access your wishlist.</p>');
    return;
  }

  $('#side-name').text(user.name || user.fullname || 'User');
  $('#side-email').text(user.email || '');
  $('#display-name').text(user.name || user.fullname || 'Full Name');
  $('#display-email').text(user.email || 'youremail@example.com');


  $('#profile-photo').on('error', function() { 
    $(this).attr('src', localPlaceholder); 
  });

  function fetchUserData() {
    $.ajax({ url: 'http://localhost:8080/Ecommerce_Website/ProfileServlet', 
      type: 'GET', 
      dataType: 'json', 
      timeout: 5000 
    })
      .done(function(profile) {
        if (!profile) {
          $('#fullname').val(user.name || '');
          $('#email').val(user.email || '');
          updateCartBadge();
          return;
        }
        if (profile.error) { 
          alert(profile.error); 
          window.location.href = '../Html/login.html'; 
          return; 
        }
        userData = profile;
        $('#fullname').val(profile.name || profile.fullname || '');
        $('#email').val(profile.email || '');
        $('#phone').val(profile.mobile || profile.phone || '');
        $('#address').val(profile.address || '');
        $('#pincode').val(profile.pinCode || profile.pincode || '');
        updateCartBadge();
      }).fail(function() {
        $('#fullname').val(user.name || '');
        $('#email').val(user.email || '');
        updateCartBadge();
      });
  }

  $('#edit-btn').click(function(){
    $('#profile-form input, #profile-form textarea').prop('disabled', false).removeClass('disabled-input');
    $('#email').prop('readonly', true).addClass('disabled-input');
    $('#edit-btn').addClass('hidden');
    $('#save-btn, #cancel-btn').removeClass('hidden');
  });

  $('#cancel-btn').click(function(){
    $('#profile-form input, #profile-form textarea').prop('disabled', true).addClass('disabled-input');
    $('#edit-btn').removeClass('hidden');
    $('#save-btn, #cancel-btn').addClass('hidden');
    if (userData) {
      $('#fullname').val(userData.name || '');
      $('#phone').val(userData.mobile || '');
      $('#address').val(userData.address || '');
      $('#pincode').val(userData.pinCode || '');
    } 
    else {
      $('#fullname').val(user.name || '');
      $('#phone').val('');
      $('#address').val('');
      $('#pincode').val('');
    }
  });

  $('#save-btn').click(function(e){
    e.preventDefault();
    const updatedData = { 
      fullname: $('#fullname').val(), 
      phone: $('#phone').val(), 
      address: $('#address').val(), 
      pincode: $('#pincode').val() 
    };
    $.ajax({ url: 'http://localhost:8080/Ecommerce_Website/ProfileServlet', 
      method: 'POST',
       data: updatedData, 
       dataType: 'json', 
       timeout: 5000 
      })
      .done(function(res) {
        if (res && res.status === 'success') { 
          toast('Profile updated!'); 
          fetchUserData(); 
          $('#cancel-btn').click();
         }
        else { 
          const msg = res && res.message ? res.message : 'Unknown server response'; 
          alert('Error: ' + msg); 
        }
      }).fail(function() { 
        alert('Server error while updating profile.'); 
      });
  });

  $('#change-password-btn').click(()=>window.location.href='../Html/changepassword.html');
  $('#sidebar-orders, #orders-btn').click(()=>window.location.href='../Html/orderhistory.html');
  $('#home-btn').click(()=>window.location.href='../Html/navbar.html');
  $('#cart-btn').click(()=>window.location.href='../Html/cart.html');

  $('#nav-logout').off('click').on('click', function () {
    const user = localStorage.getItem('user');
    if (!user) {
      localStorage.clear();
      localStorage.removeItem('buyNowItem');
      $('#cart-badge').text(0);
      updateAuthUI();
      toast('You have logged out');
      setTimeout(() => window.location.href = 'login.html', 700);
      return;
    }

    $.ajax({
      url: 'http://localhost:8080/Ecommerce_Website/LogoutServlet',
      type: 'POST',
      dataType: 'json',
      success: function (res) {
        if (typeof res === 'string') {
          try { res = JSON.parse(res); } catch (e) { res = null; }
        }
        if (res && res.status === 'success') {
          localStorage.clear();
          localStorage.removeItem('buyNowItem');
          $('#cart-badge').text(0);
          updateAuthUI();
          toast('Logged out successfully');
          setTimeout(() => window.location.href = 'login.html', 700);
        } else {
          toast((res && res.message) ? res.message : 'Logout failed');
        }
      },
      error: function () {
        toast('Server error during logout');
      }
    });
  });

  function showWishlistLoading() {
    $('#wishlist-loading').show();
    $('#wishlist-empty').hide();
    $('#wishlist-grid').empty();
  }
  function hideWishlistLoading() {
    $('#wishlist-loading').hide();
  }

  function fetchWishlist() {
    showWishlistLoading();
    const raw = localStorage.getItem('user');
    const userObj = safeParse(raw);
    const userId = userObj ? (userObj.userId || userObj.id || userObj.user_id) : null;

    $.getJSON('http://localhost:8080/Ecommerce_Website/WishlistServlet', { userId: userId })
      .done(function(products) {
        hideWishlistLoading();
        if (!products || products.length === 0) {
          $('#wishlist-empty').show();
          $('#wishlist-grid').empty();
          return;
        }
        $('#wishlist-empty').hide();
        renderWishlist(products);
      }).fail(function() {
        hideWishlistLoading();
        $('#wishlist-grid').html('<p class="text-red-600">Failed to load wishlist. Try refreshing.</p>');
      });
  }

  function resolveImage(imagePath) {
    if (!imagePath) return 'https://via.placeholder.com/400x250?text=No+Image';
    imagePath = String(imagePath).trim();
    if (/^https?:\/\//i.test(imagePath)) return imagePath;
    const origin = window.location.origin;
    if (imagePath.startsWith('/')) return origin + imagePath;
    if (imagePath.indexOf('Ecommerce_Website') !== -1) return origin + (imagePath.startsWith('/') ? imagePath : '/' + imagePath);
    return origin + '/Ecommerce_Website/' + imagePath.replace(/^\/+/, '');
  }

  function renderWishlist(products) {
    const $grid = $('#wishlist-grid');
    $grid.empty();
    products.forEach(p => {
      const id = p.productId ?? p.id ?? p.product_id;
      const name = p.productName ?? p.name ?? p.product_name ?? 'Product';
      const desc = p.description ?? '';
      const price = parseFloat(p.price ?? 0);
      const discount = parseFloat(p.discountPercent ?? 0);
      const discounted = (discount > 0) ? (Math.round((price - (price * discount / 100)) * 100)/100) : null;
      const image = Array.isArray(p.images) && p.images.length ? resolveImage(p.images[0]) : resolveImage(p.imageUrl ?? p.image);

      const stock = (p.stock != null) ? p.stock : 0;
      const oos = stock <= 0;
      const stockText = oos ? '<span class="text-red-600 font-semibold">Out of stock</span>' : ('In stock: ' + stock);

      const card = $(`
        <div class="product-card">
          <div class="flex gap-3">
            <div style="flex:0 0 36%;"><img src="${image}" class="product-image" alt="${escapeHtml(name)}"></div>
            <div style="flex:1">
              <div class="product-title">${escapeHtml(name)}</div>
              <div class="product-desc">${escapeHtml(desc)}</div>
              <div class="mt-3">
                ${ discount > 0 ? `<span class="line-through">₹${price}</span> <span class="price">₹${discounted}</span> <span class="text-sm text-red-600 font-semibold ml-2">${discount}% OFF</span>` : `<span class="price">₹${price}</span>` }
              </div>
              <div class="mt-2 text-sm text-gray-600">${stockText}</div>

              <div class="mt-4 flex items-center gap-2">
                <button class="small-btn bg-green-600 text-white move-to-cart" data-id="${id}" ${oos ? 'disabled' : ''}>Move to Cart</button>
                <button class="small-btn bg-yellow-500 text-white buy-now-wishlist" data-id="${id}" ${oos ? 'disabled' : ''}>Buy Now</button>
                <button class="small-btn bg-red-100 text-red-700 remove-wishlist" data-id="${id}">Remove</button>
              </div>
            </div>
          </div>
        </div>
      `);
      $grid.append(card);
    });
  }

  function escapeHtml(s){
    if (s == null) return '';
    return String(s).replace(/[&<>"'`]/g, function(ch){ return ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;','`':'&#96;'})[ch]; });
  }

  $(document).on('click', '.remove-wishlist', function(){
    const $btn = $(this);
    const productId = $btn.data('id');
    const raw = localStorage.getItem('user');
    const userObj = safeParse(raw);
    const userId = userObj ? (userObj.userId || userObj.id || userObj.user_id) : null;

    $btn.prop('disabled', true);
    $.post('http://localhost:8080/Ecommerce_Website/WishlistServlet', { 
      action:'remove', 
      productId: productId, 
      userId: userId 
    })
      .done(function(res){
        try { if (typeof res === 'string') res = JSON.parse(res); } catch(e){}
        if (res && res.status === 'ok') {
          toast('Removed from wishlist');
          $btn.closest('.product-card').fadeOut(200, function(){ $(this).remove(); 
            if ($('#wishlist-grid').children().length === 0) $('#wishlist-empty').show(); });
          if (typeof res.count !== 'undefined') $('#wishlist-badge').text(res.count);
        } 
        else {
          alert((res && res.message) ? res.message : 'Failed to remove from wishlist');
          $btn.prop('disabled', false);
        }
      }).fail(function(){ alert('Network error'); $btn.prop('disabled', false); });
  });

  $(document).on('click', '.move-to-cart', function(){
    const $btn = $(this);
    const productId = $btn.data('id');
    const userRaw = localStorage.getItem('user');
    if (!userRaw) { alert('Please login to add items to cart'); window.location.href = '../Html/login.html'; return; }
    $btn.prop('disabled', true);
    $.post('http://localhost:8080/Ecommerce_Website/CartServlet', { 
      action:'add', 
      productId: productId, 
      qty:1 
    })
      .done(function(res){
        try { if (typeof res === 'string') res = JSON.parse(res); } catch(e){}
        if (res && res.status === 'ok') {
          toast('Moved to cart');
          updateCartBadge();
          try {
            const u = safeParse(userRaw);
            const uid = u ? (u.userId || u.id || u.user_id) : null;
            if (uid) {
              $.post('http://localhost:8080/Ecommerce_Website/WishlistServlet', { action:'remove', productId: productId, userId: uid })
                .always(function(resp){ fetchWishlist(); }); 
            } else fetchWishlist();
          } catch(e){ 
            fetchWishlist(); 
          }
        } else {
          alert((res && res.message) ? res.message : 'Failed to add to cart');
          $btn.prop('disabled', false);
        }
      }).fail(function(){ 
        alert('Network error while adding to cart'); 
        $btn.prop('disabled', false); 
      });
  });

  $(document).on('click', '.buy-now-wishlist', function(){
    const $btn = $(this);
    const productId = $btn.data('id');
    const userRaw = localStorage.getItem('user');
    if (!userRaw) { alert('Please login to buy'); window.location.href = '../Html/login.html'; return; }
    $btn.prop('disabled', true);
    $.post('http://localhost:8080/Ecommerce_Website/CartServlet', { 
      action:'add', 
      productId: productId, 
      qty:1 
    })
      .done(function(res){
        try { if (typeof res === 'string') res = JSON.parse(res); } catch(e){}
        if (res && res.status === 'ok') {
          updateCartBadge();
          try {
            const u = safeParse(userRaw);
            const uid = u ? (u.userId || u.id || u.user_id) : null;
            if (uid) {
              $.post('http://localhost:8080/Ecommerce_Website/WishlistServlet', { 
                action:'remove', 
                productId: productId, 
                userId: uid 
              }).always(function(){ window.location.href = '../Html/cart.html'; 
                });
            } 
            else window.location.href = '../Html/cart.html';
          } catch(e){ 
            window.location.href = '../Html/cart.html'; 
          }
        } 
        else {
          alert((res && res.message) ? res.message : 'Failed to buy item');
          $btn.prop('disabled', false);
        }
      }).fail(function(){ 
        alert('Network error while buying'); 
        $btn.prop('disabled', false); 
      });
  });

  function refreshWishlistBadge() {
    const raw = localStorage.getItem('user');
    const userObj = safeParse(raw);
    const userId = userObj ? (userObj.userId || userObj.id || userObj.user_id) : null;
    $.getJSON('http://localhost:8080/Ecommerce_Website/WishlistServlet', { count:1, userId: userId })
      .done(function(res){ if (res && typeof res.count !== 'undefined') $('#wishlist-badge').text(res.count); })
      .fail(function(){ 

      });
  }

  $('#wishlist-clear-all').click(function(){
    if (!confirm('Clear all items from your wishlist?')) return;
    const raw = localStorage.getItem('user');
    const u = safeParse(raw);
    const userId = u ? (u.userId || u.id || u.user_id) : null;
    if (!userId) { alert('Login required'); window.location.href='../Html/login.html'; return; }
    $.getJSON('http://localhost:8080/Ecommerce_Website/WishlistServlet', { userId: userId })
      .done(function(products){
        if (!products || products.length === 0) { $('#wishlist-empty').show(); $('#wishlist-grid').empty(); return; }
        const removePromises = products.map(p => {
          const pid = p.productId ?? p.product_id ?? p.id;
          return $.post('http://localhost:8080/Ecommerce_Website/WishlistServlet', { 
            action:'remove', 
            productId: pid, 
            userId: userId 
          });
        });
        $.when.apply($, removePromises).always(function(){
          toast('Wishlist cleared');
          refreshWishlistBadge();
          fetchWishlist();
        });
      }).fail(function(){ alert('Failed to clear wishlist'); });
  });

  $('#sidebar-profile').click(function(){ 
    $('#profile-section').show(); 
  });

  $('#sidebar-wishlist').click(function(){ 
    window.location.href = '../Html/whishlist.html'; 
  });

  $('#wishlist-refresh').click(function(){ 
    fetchWishlist(); 
  });

  fetchUserData();
  updateCartBadge();
  refreshWishlistBadge();

  $(window).on('focus', function(){ 
    updateCartBadge(); 
    refreshWishlistBadge(); 
    if ($('#wishlist-panel').is(':visible')) fetchWishlist(); 
  });
});
