let allProducts = [];
let lastCategoryId = null;
let currentFilter = null;
let categories = [];

function isDataUri(s) {
  return typeof s === 'string' && /^data:/i.test(s);
}
function isAbsoluteUrl(s) {
  return typeof s === 'string' && /^https?:\/\//i.test(s);
}
function isProbablyBase64(s) {
  if (typeof s !== 'string') return false;
  if (s.length < 50) return false; // too short to be image base64 usually
  // allow A-Z a-z 0-9 + / = and optional newlines/spaces (we trim earlier)
  return /^[A-Za-z0-9+/=\s]+$/.test(s.replace(/\s+/g, ''));
}
function prefixBase64Mime(b64) {
  if (!b64) return '';
  const head = b64.replace(/\s+/g, '').substring(0, 8);
  if (head.startsWith('/9j/')) return 'data:image/jpeg;base64,' + b64;
  if (head.startsWith('iVBOR')) return 'data:image/png;base64,' + b64;
  if (head.startsWith('R0lG')) return 'data:image/gif;base64,' + b64;
  if (head.startsWith('UklG')) return 'data:image/webp;base64,' + b64;
  // fallback
  return 'data:image/jpeg;base64,' + b64;
}

function resolveImageUrl(imagePath) {
  // returns a string safe to use in <img src="...">
  if (!imagePath) return 'https://via.placeholder.com/400x250?text=No+Image';
  imagePath = String(imagePath).trim();
  if (!imagePath) return 'https://via.placeholder.com/400x250?text=No+Image';
  if (isDataUri(imagePath)) return imagePath;
  if (isAbsoluteUrl(imagePath)) return imagePath;
  if (isProbablyBase64(imagePath)) return prefixBase64Mime(imagePath);
  const origin = window.location.origin;
  if (imagePath.startsWith('/')) return origin + imagePath;
  if (imagePath.indexOf('Ecommerce_Website') !== -1) return origin + (imagePath.startsWith('/') ? imagePath : '/' + imagePath);
  return origin + '/Ecommerce_Website/' + imagePath.replace(/^\/+/, '');
}

function fetchCategoriesAndInit() {
  $.getJSON("/Ecommerce_Website/CategoryServlet")
    .done(function(data){
      categories = Array.isArray(data) ? data.map(c => ({ id: c.categoryId ?? c.category_id, name: c.categoryName ?? c.category_name })) : [];

      if ($('#category-select').length) {
        $('#category-select').empty().append(`<option value="">All</option>`);
        categories.forEach(c => $('#category-select').append(`<option value="${c.id}">${c.name}</option>`));
      }

      renderCategoryButtons();

      const $mobileBtns = $('#category-buttons-mobile');
      if ($mobileBtns.length) {
        $mobileBtns.empty();
        $mobileBtns.append(`<button class="cat-btn" data-cat="">All</button>`);
        categories.forEach(c => $mobileBtns.append(`<button class="cat-btn" data-cat="${c.id}">${c.name}</button>`));
        $mobileBtns.find('.cat-btn[data-cat=""]').addClass('active');

        $mobileBtns.off('click', '.cat-btn').on('click', '.cat-btn', function(){
          const cat = $(this).attr('data-cat');
          setActiveCategory(cat);
          lastCategoryId = cat ? parseInt(cat) : null;
          fetchProducts(lastCategoryId, $('#search-input').val().trim(), currentFilter);
        });
      }

      fetchProducts(lastCategoryId, $('#search-input').val().trim(), currentFilter);
    })
    .fail(function(){
      categories = [];
      renderCategoryButtons();
      fetchProducts(lastCategoryId, $('#search-input').val().trim(), currentFilter);
    });
}

function renderCategoryButtons() {
  const $catButtons = $('#category-buttons');
  if ($catButtons.length === 0) return;
  $catButtons.empty();
  $catButtons.append(`<button class="cat-btn" data-cat="">All</button>`);
  categories.forEach(c => $catButtons.append(`<button class="cat-btn" data-cat="${c.id}">${c.name}</button>`));

  $catButtons.off('click', '.cat-btn').on('click', '.cat-btn', function(){
    const cat = $(this).attr('data-cat');
    setActiveCategory(cat);
    lastCategoryId = cat ? parseInt(cat) : null;
    fetchProducts(lastCategoryId, $('#search-input').val().trim(), currentFilter);
  });

  $catButtons.find('.cat-btn[data-cat=""]').addClass('active');
}

function toast(msg, ms = 1400) {
  if ($('#toast').length === 0) $('body').append('<div id="toast" style="position:fixed;top:20px;right:20px;display:none;z-index:10000;background:#111827;color:#fff;padding:8px 12px;border-radius:6px;"></div>');
  $('#toast').stop(true,true).text(msg).fadeIn(200).delay(ms).fadeOut(300);
}

function renderStars(rating) {
  const fullStar = '<svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 inline text-yellow-400" viewBox="0 0 20 20" fill="currentColor"><path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.286 3.966a1 1 0 00.95.69h4.175c.969 0 1.371 1.24.588 1.81l-3.38 2.455a1 1 0 00-.364 1.118l1.287 3.966c.3.921-.755 1.688-1.54 1.118l-3.38-2.455a1 1 0 00-1.175 0l-3.38 2.455c-.784.57-1.838-.197-1.539-1.118l1.287-3.966a1 1 0 00-.364-1.118L2.174 9.393c-.783-.57-.38-1.81.588-1.81h4.175a1 1 0 00.95-.69l1.286-3.966z" /></svg>';
  let html = '';
  const full = Math.floor(rating || 0);
  const half = (rating % 1 >= 0.5 ? 1 : 0);
  for(let i=0;i<full;i++) html += fullStar;
  if(half) html += fullStar;
  for(let i=full+half;i<5;i++) html += '<svg class="h-5 w-5 inline text-gray-300"></svg>';
  return html;
}

function buildProductCardHtml(p) {
  const id = p.productId ?? p.id ?? p.product_id;
  const name = p.productName ?? p.name ?? p.product_name ?? "Unnamed";

  let image = 'https://via.placeholder.com/400x250?text=No+Image';
  if (Array.isArray(p.images) && p.images.length) image = resolveImageUrl(p.images[0]);
  else image = resolveImageUrl(p.imageUrl ?? p.image);

  const price = parseFloat(p.price ?? 0);
  const discount = parseFloat(p.discountPercent ?? 0);
  const discountedPrice = (discount > 0) ? (Math.round((price - price * discount / 100) * 100) / 100) : null;
  const stars = (p.stars??4);
  const stock = (p.stock != null) ? p.stock : 0;
  const oos = stock <= 0;

  const priceHtml = (discount > 0)
    ? `<div><span class="line-through text-gray-500 mr-2">₹${price}</span><span class="font-bold text-green-600">₹${discountedPrice}</span><span class="ml-2 text-sm text-red-600 font-semibold">${discount}% OFF</span></div>`
    : `<div class="font-bold text-green-600 mt-2">₹${price}</div>`;

  const isWish = p.inWishlist === true;
  const heartClass = isWish ? 'wishlist-btn filled' : 'wishlist-btn';
  const heartHtml = `
    <button class="${heartClass}" data-id="${id}" aria-label="Wishlist" title="${isWish ? 'Remove from wishlist' : 'Add to wishlist'}">
      <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" role="img" aria-hidden="true">
        <path d="M12 21s-7-4.35-9-7.2C-1.1 8.8 4.5 3 8.4 6.2 10.8 8.3 12 10 12 10s1.2-1.7 3.6-3.8C19.5 3 25.1 8.8 21 13.8 19 16.65 12 21 12 21z"></path>
      </svg>
    </button>
  `;

  // ensure data-image carries a usable src
  const dataImage = image.replace(/"/g, '&quot;');

  return `
    <div class="bg-white p-4 rounded-lg shadow-md text-center product-card relative" data-id="${id}">
      ${heartHtml}
      <img src="${image}" class="product-image mb-3 mx-auto rounded" alt="${escapeHtml(name)}" style="width:100%;height:200px;object-fit:cover;">
      <h4 class="font-semibold text-gray-800">${escapeHtml(name)}</h4>
      <div class="flex justify-center items-center mb-2">${renderStars(stars)}<span class="ml-2 text-yellow-600 font-semibold">${stars}</span></div>
      ${priceHtml}
      <p class="stock-text text-sm mt-1 ${oos ? 'text-red-600 font-semibold' : 'text-gray-600'}">
        ${ oos ? 'Out of stock' : ('In stock: ' + stock) }
      </p>
      <div class="mt-3 button-container" ${oos ? 'style="display:none;"' : ''}>
        <button class="add-cart bg-green-600 text-white px-4 py-2 rounded-md"
          data-id="${id}" data-name="${escapeHtml(name)}" data-price="${(discountedPrice ?? price)}" data-image="${dataImage}">
          Add to Cart
        </button>
        <button class="buy-now bg-yellow-500 text-white px-4 py-2 rounded-md ml-2"
          data-id="${id}" data-name="${escapeHtml(name)}" data-price="${(discountedPrice ?? price)}" data-image="${dataImage}">
          Buy Now
        </button>
      </div>
    </div>
  `;
}

function escapeHtml(s){
  if (s == null) return '';
  return String(s).replace(/[&<>"'`]/g, function(ch){ return ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;','`':'&#96;'})[ch]; });
}

function showProducts(list) {
  if (!list || list.length===0) {
   $("#product-list").html("<p class='text-center text-gray-500'>No products found.</p>");
   return;
  }
  let html = "";
  const chunkSize = 4;
  for (let i=0;i<list.length;i+=chunkSize){
   html += '<div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-8 mb-10">';
   const row = list.slice(i,i+chunkSize);
   html += row.map(p => buildProductCardHtml(p)).join('');
   html += '</div>';
  }
  $("#product-list").html(html);

  $('#product-list').off('click', '.product-card').on('click', '.product-card', function(e){
    if ($(e.target).closest('.add-cart, .buy-now, .wishlist-btn').length) return;
    const id = $(this).data('id');
    if (id) window.location.href = 'product-details.html?productId=' + encodeURIComponent(id);
  });
}

function setActiveCategory(catId) {
  const cstr = (catId === null || catId === '' || typeof catId === 'undefined') ? '' : String(catId);

  $('#category-buttons .cat-btn, #category-buttons-mobile .cat-btn').removeClass('active');
  $(`#category-buttons .cat-btn[data-cat='${cstr}']`).addClass('active');
  $(`#category-buttons-mobile .cat-btn[data-cat='${cstr}']`).addClass('active');

  $('#category-visible-list .dropdown-category').removeClass('cat-active').attr('aria-pressed','false');

  const idToKey = { '1':'mobiles', '2':'laptops', '3':'tvs', '4':'speakers', '7':'cameras', '8':'tablets', '':'' };
  const key = (cstr === '' ? '' : idToKey[cstr] ?? '');
  if (key) {
    $(`#category-visible-list .dropdown-category[data-cat='${key}']`).addClass('cat-active').attr('aria-pressed','true');
  }

  if ($('#category-select').length) $('#category-select').val(cstr);
}

function fetchProducts(categoryId = null, query = '', filter = currentFilter) {
  lastCategoryId = (categoryId === null || categoryId === '' ? null : categoryId);
  setActiveCategory(categoryId);

  if (query && typeof query === 'object' && query.q) query = query.q;

  let url = "/Ecommerce_Website/ProductServlet";
  const params = [];

  if (categoryId) params.push("category_id=" + encodeURIComponent(categoryId));
  if (query && query.trim() !== "") params.push("query=" + encodeURIComponent(query));
  if (filter) params.push("filter=" + encodeURIComponent(filter));

  if (params.length) url += "?" + params.join("&");

  $.getJSON(url)
   .done(data => {
    allProducts = Array.isArray(data) ? data : [];

    const detailPromises = [];
    allProducts.forEach(p => {
      // fetch detail only if missing images or discount
      if ((typeof p.discountPercent === 'undefined' || !Array.isArray(p.images) || p.images.length === 0) && (p.productId || p.product_id || p.id)) {
        const pid = p.productId ?? p.product_id ?? p.id;
        detailPromises.push(
          $.getJSON("/Ecommerce_Website/ProductServlet?productId=" + encodeURIComponent(pid))
            .done(function(details){
              if (!details) return;
              if (typeof details.discountPercent !== 'undefined') p.discountPercent = details.discountPercent;
              if (details.discountedPrice) p.discountedPrice = details.discountedPrice;
              if (Array.isArray(details.images) && details.images.length) p.images = details.images;
              else if (details.image) p.image = details.image;
            })
            .fail(function(){})
        );
      }
    });

    function markWishlistOnProducts(wishlistIds) {
      if (!Array.isArray(wishlistIds)) wishlistIds = [];
      const idSet = {};
      wishlistIds.forEach(id => { idSet[Number(id)] = true; });
      allProducts.forEach(p => {
        const pid = Number(p.productId ?? p.product_id ?? p.id);
        p.inWishlist = !!idSet[pid];
      });
    }

    if (detailPromises.length > 0) {
      $.when.apply($, detailPromises).always(function(){
        fetchWishlistIdsForCurrentUser().always(function(ids){
          markWishlistOnProducts(ids);
          showProducts(allProducts);
        });
      });
    } else {
      fetchWishlistIdsForCurrentUser().always(function(ids){
        markWishlistOnProducts(ids);
        showProducts(allProducts);
      });
    }
   })
   .fail(() => {
    $("#product-list").html("<p class='text-center text-red-500'>Failed to load products.</p>");
   });
}

function refreshBadge() {
  const user = localStorage.getItem("user");
  if(!user) { $('#cart-badge').text(0); return; }
  $.getJSON("/Ecommerce_Website/CartServlet")
   .done(data=>{
     const total = Array.isArray(data)?data.reduce((sum,it)=>sum+(it.qty??it.quantity??0),0):0;
     $('#cart-badge').text(total);
   }).fail(()=>$('#cart-badge').text(0));
}

function getCurrentUserIdFromLocalStorage() {
  const user = localStorage.getItem('user');
  if (!user) return null;
  try {
    const u = JSON.parse(user);
    return u ? (u.userId || u.id || u.user_id) : null;
  } catch (e) { return null; }
}

function fetchWishlistIdsForCurrentUser() {
  const deferred = $.Deferred();
  const uid = getCurrentUserIdFromLocalStorage();
  $.getJSON('/Ecommerce_Website/WishlistServlet', { userId: uid })
    .done(function(res){
      if (Array.isArray(res)) {
        const ids = res.map(function(item){ return Number(item.productId ?? item.product_id ?? item.id); }).filter(Boolean);
        deferred.resolve(ids);
      } else if (res && typeof res.count !== 'undefined' && (res.count === 0)) {
        deferred.resolve([]);
      } else {
        try {
          if (res && typeof res === 'object') {
            if (res.productId || res.product_id) {
              deferred.resolve([Number(res.productId ?? res.product_id)]);
            } else {
              deferred.resolve([]);
            }
          } else {
            deferred.resolve([]);
          }
        } catch (e) { deferred.resolve([]); }
      }
    })
    .fail(function(){
      deferred.resolve([]);
    });
  return deferred.promise();
}

function ensureWishlistStyles() {
  if ($('#_wishlist_styles').length) return;
  const css = `
    .wishlist-btn { position: absolute; top:12px; right:12px; background: transparent; border: none; cursor: pointer; padding:6px; border-radius: 50%; transition: transform .12s; }
    .wishlist-btn svg { width:20px; height:20px; fill: transparent; stroke: #666; stroke-width: 1; transition: fill .12s, stroke .12s; }
    .wishlist-btn.filled svg { fill: #e11d48; stroke: #e11d48; }
    .wishlist-btn:active { transform: scale(.96); }
  `;
  $('<style id="_wishlist_styles"></style>').text(css).appendTo('head');
}

function refreshWishlistBadge() {
  const user = localStorage.getItem("user");
  if (!user) { $('#wishlist-badge').text(0); return; }
  let userObj;
  try { userObj = JSON.parse(user); } catch(e) { userObj = null; }
  const userId = userObj ? (userObj.userId || userObj.id || userObj.user_id) : null;

  $.getJSON('/Ecommerce_Website/WishlistServlet', { count:1, userId: userId })
    .done(function(res){
      const cnt = res && typeof res.count !== 'undefined' ? res.count : 0;
      $('#wishlist-badge').text(cnt);
    }).fail(function(){ $('#wishlist-badge').text(0); });
}

$(document).ready(function(){

  if (typeof $ === 'undefined') {
   console.error("jQuery is not loaded. Ensure you included jQuery before navbar.js");
   return;
  }

  ensureWishlistStyles();

  if ($('#category-select').length) {
    $('#category-select').off('change').on('change', function(){
      const val = $(this).val();
      setActiveCategory(val);
      lastCategoryId = val ? parseInt(val) : null;
      fetchProducts(lastCategoryId, $('#search-input').val().trim(), currentFilter);
    });
  }

  $('#mobile-cat-toggle').off('click').on('click', function(){ $('#category-container').toggleClass('hidden'); });

  function updateAuthUI() {
    const logged = !!localStorage.getItem('user');
    if (logged) $('#nav-login, #nav-register').hide(); else $('#nav-login, #nav-register').show();
    if (logged) $('#nav-profile, #nav-logout, #cart-link, #nav-back').show();
    else { $('#nav-profile, #nav-logout').hide(); $('#cart-badge').text(0); }
  }
  updateAuthUI();

  $(document).off('click.wishlistLink').on('click.wishlistLink', '#wishlist-link', function(){
    const user = localStorage.getItem("user");
    if(!user) { alert("Please login to see your wishlist"); window.location.href = "login.html"; return; }
    window.location.href = "whishlist.html";
  });

  $("#nav-login").off('click').on('click', () => {
    const user = localStorage.getItem("user");
    if(user) { alert("You are already logged in!"); window.location.href = "navbar.html"; } else window.location.href = "login.html";
  });
  $("#nav-register").off('click').on('click', () => {
    const user = localStorage.getItem("user");
    if(user) { alert("You are already registered and logged in!"); window.location.href = "index.html"; } else window.location.href = "register.html";
  });
  $("#cart-link").off('click').on('click', () => {
    const user = localStorage.getItem("user");
    if(!user) { alert("Please login first to view your cart!"); window.location.href = "login.html"; } else window.location.href = "cart.html";
  });
  $("#nav-profile").off('click').on('click', () => {
    const user = localStorage.getItem("user");
    if(!user) { alert("Please login first to view your profile!"); window.location.href = "login.html"; } else window.location.href = "profile.html";
  });
  $("#home-btn").off('click').on('click', () => window.location.href='navbar.html');

  $('#nav-logout').off('click').on('click', function () {
    $('#nav-logout').prop('disabled', true);
    $.ajax({
      url: '/Ecommerce_Website/LogoutServlet',
      type: 'POST',
      dataType: 'json',
      timeout: 4000
    }).always(function(resRaw, textStatus, jqXHRorUndefined) {
      var res = resRaw;
      if (typeof resRaw === 'string') {
        try { res = JSON.parse(resRaw); } catch(e){ res = null; }
      } else if (jqXHRorUndefined && jqXHRorUndefined.getResponseHeader) {
        try {
          res = (resRaw && resRaw.responseJSON) ? resRaw.responseJSON : resRaw;
        } catch(e){ }
      }
      localStorage.clear();
      localStorage.removeItem('buyNowItem');
      $('#cart-badge').text(0);
      updateAuthUI();
      if (res && res.status === 'success') {
        toast('Logged out successfully');
      } else {
        toast('Logged out (server may not have a session)');
      }
      setTimeout(function(){ window.location.href = 'login.html'; }, 700);
    }).fail(function(){
      localStorage.clear();
      localStorage.removeItem('buyNowItem');
      $('#cart-badge').text(0);
      updateAuthUI();
      toast('Logged out (network/server error)');
      setTimeout(function(){ window.location.href = 'login.html'; }, 700);
    }).always(function(){ $('#nav-logout').prop('disabled', false); });
  });

  $("#search-input").off('input').on("input", function () {
    let query = $("#search-input").val().toLowerCase().trim();
    if (!query) { fetchProducts(lastCategoryId,'',currentFilter); return; }
    const filtered = (allProducts || []).filter(p => ((p.productName || p.name) + '').toLowerCase().includes(query));
    showProducts(filtered);
  });

  $('#search-btn').off('click').on('click', function() {
    const q = $('#search-input').val().trim();
    fetchProducts(lastCategoryId, q, currentFilter);
  });

  $('#search-input').off('keydown').on('keydown', function(e) {
    if (e.key === 'Enter') {
      e.preventDefault();
      const q = $(this).val().trim();
      fetchProducts(lastCategoryId, q, currentFilter);
    }
  });

  $('#filter-btn').off('click').on('click', function(e){
    $('#filter-options').toggle();
  });
  $(document).off('click.filterHide').on('click.filterHide', function(){ $('#filter-options').hide(); });

  $(document).off('click.filterOption').on('click.filterOption', '.filter-option', function() {
    currentFilter = $(this).data('type');
    $('#filter-options').hide();
    const q = $('#search-input').val().trim();
    fetchProducts(lastCategoryId, q, currentFilter);
  });

  $(document).on('click', '#category-visible-list .dropdown-category', function(e){
    const key = $(this).data('cat');
    const map = { mobiles:1, laptops:2, tvs:3, speakers:4, cameras:7, tablets:8 };
    const catId = map[key] ?? null;
    setActiveCategory(catId);
    lastCategoryId = catId;
    fetchProducts(lastCategoryId, $('#search-input').val().trim(), currentFilter);
  });

  $(document).off('click.wishlist').on('click.wishlist', '.wishlist-btn', function(e){
    const $btn = $(this);
    const productId = $btn.data('id');
    const user = localStorage.getItem('user');
    if (!user) {
      alert('Please login to use wishlist');
      window.location.href = 'login.html';
      return;
    }
    let userObj;
    try { userObj = JSON.parse(user); } catch(e){ userObj = null; }
    const userId = userObj ? (userObj.userId || userObj.id || userObj.user_id) : null;

    const currentlyFilled = $btn.hasClass('filled');
    $btn.toggleClass('filled', !currentlyFilled);
    $btn.attr('title', !currentlyFilled ? 'Remove from wishlist' : 'Add to wishlist');

    const action = currentlyFilled ? 'remove' : 'add';

    $.post("/Ecommerce_Website/WishlistServlet",
      { action: action, productId: productId, userId: userId },
      function(res){
        if (typeof res === 'string') {
          try { res = JSON.parse(res); } catch(e){}
        }
        if (res && res.status === 'ok') {
          if (typeof res.count !== 'undefined') {
            $('#wishlist-badge').text(res.count);
          } else {
            refreshWishlistBadge();
          }

          if (action === 'add') {
            if (typeof res.added !== 'undefined' && res.added === false) {
              toast('Already wishlisted', 1200);
              $btn.addClass('filled').attr('title', 'Remove from wishlist');
            } else {
              toast('Added to wishlist', 1000);
              $btn.addClass('filled').attr('title', 'Remove from wishlist');
            }
          } else if (action === 'remove') {
            if (typeof res.removed !== 'undefined' && res.removed === false) {
              toast('Item was not in wishlist', 1200);
              $btn.addClass('filled').attr('title', 'Remove from wishlist');
            } else {
              toast('Removed from wishlist', 1000);
              $btn.removeClass('filled').attr('title', 'Add to wishlist');
            }
          }

          if (action === 'remove' && window.location.pathname.endsWith('wishlist.html')) {
            $(`.product-card[data-id='${productId}']`).fadeOut(200, function(){ $(this).remove(); });
          }
        } else {
          $btn.toggleClass('filled', currentlyFilled);
          alert((res && res.message) ? res.message : 'Failed to update wishlist.');
        }
      }, 'json'
    ).fail(function(){
      $btn.toggleClass('filled', currentlyFilled);
      alert('Network error: failed to update wishlist.');
    });
  });

  $(document).off('click.addCart').on("click.addCart", ".add-cart", function(e){
    const user = localStorage.getItem("user");
    if(!user) { alert("Please login to add items to your cart!"); window.location.href = "login.html"; return; }
    const $btn = $(this);
    const productId = $btn.data('id');
    const data={ action:'add', productId:productId, qty:1 };
    $btn.prop('disabled', true).addClass('opacity-70');
    $.post("/Ecommerce_Website/CartServlet", data, function(res){
      try {
        if (typeof res === 'string') res = JSON.parse(res);
      } catch(e){}
      if (res && res.status === 'ok') {
        toast('Added to cart', 900);
        refreshBadge();
        const newStock = (res.newStock != null) ? parseInt(res.newStock) : null;
        $(`.product-card[data-id='${productId}']`).each(function(){
          const $card = $(this);
          if (newStock != null) {
            $card.find('.stock-text').text(newStock <= 0 ? 'Out of stock' : ('In stock: ' + newStock));
            if (newStock <= 0) {
              $card.find('.button-container').hide();
            }
          }
        });
        for (let i=0;i<allProducts.length;i++){
          const pid = allProducts[i].productId ?? allProducts[i].id ?? allProducts[i].product_id;
          if (pid == productId) { if (newStock != null) allProducts[i].stock = newStock; break; }
        }
      } else {
        alert((res && res.message) ? res.message : 'Failed to add to cart');
      }
      $btn.prop('disabled', false).removeClass('opacity-70');
    }, 'json').fail(function(){
      alert('Failed to add to cart');
      $btn.prop('disabled', false).removeClass('opacity-70');
    });
  });

  $(document).off('click.buyNow').on("click.buyNow", ".buy-now", function(e){
    const user = localStorage.getItem("user");
    if(!user) { alert("Please login to buy products!"); window.location.href = "login.html"; return; }
    const $btn = $(this);
    const productId = $btn.data('id');
    const product = { productId: productId, name: $btn.data('name'), price: $btn.data('price'), qty:1, image: $btn.data('image') };
    $btn.prop('disabled', true).addClass('opacity-70');
    $.post("/Ecommerce_Website/CartServlet", { action:'add', productId: productId, qty:1 }, function(res){
      try {
        if (typeof res === 'string') res = JSON.parse(res);
      } catch(e){}
      if (res && res.status === 'ok') {
        const newStock = (res.newStock != null) ? parseInt(res.newStock) : null;
        refreshBadge();
        $(`.product-card[data-id='${productId}']`).each(function(){ 
          const $card = $(this);
          if (newStock != null) {
            $card.find('.stock-text').text(newStock <= 0 ? 'Out of stock' : ('In stock: ' + newStock));
            if (newStock <= 0) {
              $card.find('.button-container').hide();
            }
          }
        });
        localStorage.setItem("buyNowItem", JSON.stringify(product));
        try {
          const userObj = JSON.parse(localStorage.getItem('user') || '{}');
          const uid = userObj.userId || userObj.id || userObj.user_id || null;
          if (uid) {
            $.post("/Ecommerce_Website/WishlistServlet", { action:'remove', productId: productId, userId: uid })
              .always(function(){ refreshWishlistBadge(); });
          }
        } catch(err) {}
        window.location.href = "cart.html";
      } else {
        alert((res && res.message) ? res.message : 'Failed to add to cart');
        $btn.prop('disabled', false).removeClass('opacity-70');
      }
    }, 'json').fail(function(){
      alert('Failed to add to cart');
      $btn.prop('disabled', false).removeClass('opacity-70');
    });
  });

  fetchCategoriesAndInit();
  refreshBadge();
  refreshWishlistBadge();

  // polling to keep UI reasonably fresh (optional)
  setInterval(function(){
    fetchProducts(lastCategoryId, $('#search-input').val().trim(), currentFilter);
    refreshBadge();
    refreshWishlistBadge();
  }, 15000);
});
