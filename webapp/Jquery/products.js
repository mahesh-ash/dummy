(function($){
  'use strict';

  const API_ROOT = '/Ecommerce_Website';
  let images = [];
  let curIndex = 0;
  let price = 0;
  let discount = 0;
  let discountedPrice = null;
  let availableStock = 0;
  let pid = new URLSearchParams(window.location.search).get('productId');
  const PLACEHOLDER = 'https://via.placeholder.com/800x500?text=No+Image';

  function toast(msg, ms = 1400){
    if (!$('#toast').length) $('body').append('<div id="toast" class="toast"></div>');
    $('#toast').stop(true, true).text(msg).fadeIn(150).delay(ms).fadeOut(250);
  }

  /**
   * Robust resolver for different image formats:
   * - absolute http/https (returned as-is)
   * - data: URIs and blob: URIs (returned as-is)
   * - absolute paths starting with '/' -> origin + path
   * - paths that already include 'Ecommerce_Website' -> origin + path
   * - relative paths -> origin + '/Ecommerce_Website/' + path
   */
  function resolveImageUrl(imagePath){
    try {
      if (!imagePath) return PLACEHOLDER;
      imagePath = String(imagePath).trim();
      if (!imagePath) return PLACEHOLDER;

      // If it's already a data: or blob: URI return as-is
      if (/^(data:|blob:)/i.test(imagePath)) return imagePath;

      // If absolute http(s) URL, return as-is
      if (/^https?:\/\//i.test(imagePath)) return imagePath;

      const origin = window.location.origin || (window.location.protocol + '//' + window.location.host);

      // If it's already an absolute path on the server
      if (imagePath.startsWith('/')) {
        return origin + imagePath;
      }

      // If path contains Ecommerce_Website already, ensure it has leading slash
      if (imagePath.indexOf('Ecommerce_Website') !== -1) {
        return origin + (imagePath.startsWith('/') ? imagePath : '/' + imagePath);
      }

      // Otherwise assume relative to project assets root
      return origin + '/Ecommerce_Website/' + imagePath.replace(/^\/+/, '');
    } catch (e) {
      return PLACEHOLDER;
    }
  }

  function safeParse(r){
    try{
      if (typeof r === 'string') return JSON.parse(r);
    } catch(e){}
    return r;
  }

  function escapeHtml(s){
    if (s == null) return '';
    return String(s).replace(/[&<>"'`]/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;','`':'&#96;'}[ch]));
  }

  function setMainImageByIndex(i){
    if (!images || images.length === 0){
      $('#main-image').attr('src', PLACEHOLDER);
      return;
    }
    curIndex = ((i % images.length) + images.length) % images.length;
    const src = images[curIndex] || PLACEHOLDER;
    $('#main-image').off('error.mainImg').on('error.mainImg', function(){
      // if main image fails, attempt next image, else placeholder
      if (images.length > 1) {
        const next = (curIndex + 1) % images.length;
        curIndex = next;
        $(this).attr('src', images[next] || PLACEHOLDER);
      } else {
        $(this).attr('src', PLACEHOLDER);
      }
    }).attr('src', src);
    $('#thumbs .thumb').removeClass('active');
    $(`#thumbs .thumb[data-idx="${curIndex}"]`).addClass('active');
  }

  /**
   * Render thumbnails + main image.
   * Accepts list which may contain strings, objects {path, url, imgId} etc.
   */
  function renderImages(list){
    images = (list || []).map(function(it){
      // normalize allowed shapes
      if (!it) return null;
      if (typeof it === 'string') return resolveImageUrl(it);
      // if object — try common properties
      if (typeof it === 'object') {
        if (it.url) return resolveImageUrl(it.url);
        if (it.path) return resolveImageUrl(it.path);
        if (it.image) return resolveImageUrl(it.image);
        if (it.image_path) return resolveImageUrl(it.image_path);
        if (it.imgId) {
          // construct ImageServlet URL by imgId
          return resolveImageUrl(API_ROOT + '/ImageServlet?imgId=' + encodeURIComponent(it.imgId));
        }
        if (it.productId) {
          return resolveImageUrl(API_ROOT + '/ImageServlet?productId=' + encodeURIComponent(it.productId));
        }
      }
      return null;
    }).filter(Boolean);

    if (!images.length) images = [PLACEHOLDER];

    const $t = $('#thumbs').empty();
    images.forEach((src, idx) => {
      // add error handling on each thumb so broken thumbs are swapped to placeholder
      const $img = $(`<img src="${src}" class="thumb ${idx === 0 ? 'active' : ''}" data-idx="${idx}" alt="thumb" loading="lazy">`);
      $img.on('error', function(){
        $(this).off('error').attr('src', PLACEHOLDER);
      });
      $t.append($img);
    });

    setMainImageByIndex(0);
  }

  function renderPriceBlock(){
    if (discount && discount > 0) {
      discountedPrice = Math.round((price - (price * discount / 100)) * 100) / 100;
      $('#price-row').html(
        `<div>
           <span class="line-through text-gray-500 mr-2">₹${(isNaN(price)?0:price).toFixed(2)}</span>
           <span class="text-2xl font-bold text-green-600">₹${(isNaN(discountedPrice)?0:discountedPrice).toFixed(2)}</span>
           <span class="ml-2 px-2 py-1 text-sm bg-red-100 text-red-600 rounded">${Number(discount)}% OFF</span>
         </div>`
      );
    }
     else {
      discountedPrice = null;
      $('#price-row').html(`<div class="text-2xl font-bold text-green-600">₹${(isNaN(price)?0:price).toFixed(2)}</div>`);
    }
  }

  function setStock(n){
    availableStock = Number.isFinite(Number(n)) ? Number(n) : 0;

    if (availableStock <= 0){
      $('#stock').text('Out of stock').addClass('text-red-600 font-semibold').removeClass('text-green-600');

      $('#add-cart, #buy-now').prop('disabled', true).addClass('disabled');

      $('#qty-decr, #qty-incr, #qty').hide();
    } else {
      $('#stock').text('In stock: ' + availableStock).addClass('text-green-600').removeClass('text-red-600 font-semibold');

      $('#add-cart, #buy-now').prop('disabled', false).removeClass('disabled');

      $('#qty-decr, #qty-incr, #qty').show();
      const q = Math.max(1, Math.min(Number($('#qty').val() || 1), availableStock));
      $('#qty').val(q);
    }
  }


  function refreshBadge(){
    const user = localStorage.getItem('user');
    if (!user){ $('#cart-badge').text(0); return; }
    $.getJSON(API_ROOT + '/CartServlet')
      .done(data => {
        const total = Array.isArray(data) ? data.reduce((s, i) => s + (i.qty || 0), 0) : 0;
        $('#cart-badge').text(total);
      })
      .fail(() => $('#cart-badge').text(0));
  }

  function loadProduct(pidParam){
    if (!pidParam) { $('#loading').text('Product not specified'); return; }
    $('#loading').show();
    $('#product-content').addClass('hidden');

    $.getJSON(API_ROOT + '/ProductServlet?productId=' + encodeURIComponent(pidParam))
      .done(function(res){
        res = safeParse(res);
        if (!res || !(res.productId || res.product_id)){ $('#loading').text('Product not found'); return; }
        $('#loading').hide();
        $('#product-content').removeClass('hidden');

        const name = res.productName ?? res.product_name ?? '';
        $('#pname').text(name);
        $('#desc').text(res.description ?? res.desc ?? '');

        price = parseFloat(res.price ?? 0) || 0;
        discount = parseFloat(res.discountPercent ?? 0) || 0;
        renderPriceBlock();
        setStock(res.stock ?? res.qty ?? 0);

        // Support many shapes: images array of strings, array of objects, or single image field
        let imgs = [];
        if (Array.isArray(res.images) && res.images.length) {
          imgs = res.images.slice();
        } else if (Array.isArray(res.imageList) && res.imageList.length) {
          imgs = res.imageList.slice();
        } else if (res.image) {
          imgs = [res.image];
        } else if (res.image_path) {
          imgs = [res.image_path];
        } else if (res.img) {
          imgs = [res.img];
        } else if (res.productImgs && Array.isArray(res.productImgs)) {
          imgs = res.productImgs.slice();
        }

        // If server returned no images but returned image_data flag (or expects ImageServlet),
        // attempt to construct an ImageServlet URL for product.
        if (imgs.length === 0) {
          // prefer imgId if available
          if (res.primaryImageId) {
            imgs.push(API_ROOT + '/ImageServlet?imgId=' + encodeURIComponent(res.primaryImageId));
          } else {
            // fallback to ImageServlet by productId so DB-stored binary is served
            const pidForImg = res.productId ?? res.product_id ?? pidParam;
            if (pidForImg) imgs.push(API_ROOT + '/ImageServlet?productId=' + encodeURIComponent(pidForImg));
          }
        }

        renderImages(imgs);

        $('#meta').text('Product ID: ' + (res.productId ?? res.product_id));

        const optional = $('#optional-sections').empty();
        const specs = res.specs ?? res.specifications ?? res.features;
        if (specs && (typeof specs === 'object') && Object.keys(specs).length){
          const $specWrap = $(`<div id="spec-block" class="mt-6"><h3 class="text-lg font-semibold">Specifications</h3><div id="specs" class="mt-2 bg-white"></div></div>`);
          optional.append($specWrap);
          const $specs = $specWrap.find('#specs');
          Object.keys(specs).forEach(k => {
            $specs.append(`<div class="flex justify-between text-sm py-2 border-b"><div class="text-gray-700">${escapeHtml(k)}</div><div class="text-gray-800 font-medium">${escapeHtml(specs[k])}</div></div>`);
          });
        }

        const reviews = res.reviews;
        if (Array.isArray(reviews) && reviews.length){
          const $revWrap = $(`<div id="reviews-block" class="mt-6"><h3 class="text-lg font-semibold">Reviews</h3><div id="reviews" class="mt-2"></div></div>`);
          optional.append($revWrap);
          const $rlist = $revWrap.find('#reviews');
          reviews.forEach(rv => {
            const user = escapeHtml(rv.user ?? rv.username ?? 'User');
            const text = escapeHtml(rv.text ?? rv.comment ?? '');
            const rating = rv.rating ?? rv.stars ?? '';
            const when = rv.date ?? '';
            $rlist.append(`<div class="p-3 border rounded mb-2"><div class="text-sm font-medium">${user}${rating ? (' — ' + rating + '/5') : ''}</div><div class="text-sm text-gray-700 mt-1">${text}</div>${when ? ('<div class="text-xs text-gray-400 mt-1">'+escapeHtml(when)+'</div>') : ''}</div>`);
          });
        }

      })
      .fail(function(){
        $('#loading').text('Failed to load product. Please try again');
      });
  }

  function doAddToCart(productId, qty, onSuccess){
    const user = localStorage.getItem('user');
    if (!user){ alert('Please login to add items to cart'); window.location.href = 'login.html'; return; }
    const $btn = $('#add-cart');
    $btn.prop('disabled', true).addClass('disabled');
    $.post(API_ROOT + '/CartServlet', { action: 'add', productId: productId, qty: qty }, function(res){
      res = safeParse(res);
      if (res && res.status === 'ok'){
        toast('Added to cart', 900);
        refreshBadge();
        if (typeof onSuccess === 'function') onSuccess(res);
      }
      else {
        alert((res && res.message) ? res.message : 'Failed to add to cart');
      }
    }, 'json').fail(() => alert('Failed to add to cart')).always(() => {
      $btn.prop('disabled', false).removeClass('disabled');
    });
  }

  let allProducts = [];
  function fetchAllProductsFallback(){
    return $.getJSON(API_ROOT + '/ProductServlet').done(list => {
      allProducts = Array.isArray(list) ? list : [];
    }).fail(() => { allProducts = []; });
  }

  function showSuggestions(q){
    const $s = $('#suggestions');
    if (!q || q.trim() === ''){ $s.hide().empty(); return; }

    const tryShow = () => {
      const matches = (allProducts || []).filter(p => (((p.productName || p.name || '') + '').toLowerCase().includes(q.toLowerCase()))).slice(0, 8);
      if (!matches.length){ $s.hide().empty(); return; }

      const html = matches.map(m => {
        const id = m.productId ?? m.product_id ?? m.id;
        const name = escapeHtml(m.productName ?? m.name ?? m.product_name);
        // pick first available image: images array, images[0], image, imageUrl
        const imgCandidate = (Array.isArray(m.images) && m.images[0]) || m.image || m.imageUrl || null;
        const img = resolveImageUrl(imgCandidate);
        return `<div class="suggestion-item flex items-center gap-3 p-2 cursor-pointer" data-id="${id}" data-name="${name}" tabindex="0" style="border-bottom:1px solid rgba(0,0,0,0.03);">
                  <img src="${img}" class="w-12 h-8 object-cover rounded flex-shrink-0" alt="${name}" loading="lazy" onerror="this.onerror=null;this.src='${PLACEHOLDER}';">
                  <div class="suggestion-meta" style="min-width:0;">
                    <div class="suggestion-name text-sm text-gray-800" style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:320px;">${name}</div>
                  </div>
                </div>`;
      }).join('');
      $s.html(html).show();
    };

    if (allProducts.length) tryShow();
    else fetchAllProductsFallback().always(tryShow);
  }

  $(function(){
    function updateAuthUI(){
      const logged = !!localStorage.getItem('user');
      if (logged){
        $('#nav-login, #nav-register').hide();
        $('#nav-profile, #nav-logout').show();
      } else {
        $('#nav-login, #nav-register').show();
        $('#nav-profile, #nav-logout').hide();
      }
    }

    updateAuthUI();
    refreshBadge();

    if (pid) loadProduct(pid);

    // click on thumbnail
    $('#thumbs').on('click', '.thumb', function(){
       setMainImageByIndex(parseInt($(this).attr('data-idx')));
    });

    $('#prev-img').on('click', () => setMainImageByIndex(curIndex - 1));
    $('#next-img').on('click', () => setMainImageByIndex(curIndex + 1));

    $('#qty-incr').on('click', () => { let v = Number($('#qty').val() || 1); if (availableStock && v >= availableStock) return; $('#qty').val(v + 1); });
    $('#qty-decr').on('click', () => { let v = Number($('#qty').val() || 1); if (v > 1) $('#qty').val(v - 1); });

    $('#add-cart').on('click', function(){
      const q = Math.max(1, Number($('#qty').val() || 1));
      doAddToCart(pid, q);
    });

    $('#buy-now').on('click', function(){
      const q = Math.max(1, Number($('#qty').val() || 1));
      doAddToCart(pid, q, function(){
        const item = { productId: pid, name: $('#pname').text(), price: (discountedPrice !== null ? discountedPrice : price), qty: q, image: images[0] || '' };
        localStorage.setItem('buyNowItem', JSON.stringify(item));
        window.location.href = 'cart.html';
      });
    });

    let suggestTimer = null;
    $('#search-input').on('input', function(){
      const q = $(this).val().trim();
      clearTimeout(suggestTimer);
      suggestTimer = setTimeout(() => showSuggestions(q), 140);
    });

    $('#suggestions').on('click', '.suggestion-item', function(){
      const id = $(this).data('id');
      if (id){ $('#suggestions').hide(); window.location.href = 'product-details.html?productId=' + encodeURIComponent(id); }
    });

    function doSearchAndHandle(q){
      if (!q || !q.trim()) return;
      $.getJSON(API_ROOT + '/ProductServlet?query=' + encodeURIComponent(q))
        .done(list => {
          list = Array.isArray(list) ? list : [];
          if (list.length === 0){
            $('#results-list').empty();
            $('#search-results').removeClass('hidden');
          } else if (list.length === 1){
            const id = list[0].productId ?? list[0].product_id ?? list[0].id;
            window.location.href = 'product-details.html?productId=' + encodeURIComponent(id);
          } else {
            $('#results-list').empty();
            list.forEach(p => {
              const id = p.productId ?? p.product_id ?? p.id;
              const name = p.productName ?? p.name ?? p.product_name;
              const img = resolveImageUrl((Array.isArray(p.images) && p.images[0]) || p.image || p.imageUrl);
              const pr = parseFloat(p.price ?? 0) || 0;
              const disc = parseFloat(p.discountPercent ?? 0) || 0;
              const pricedisp = disc > 0 ? Math.round((pr - pr * disc / 100) * 100) / 100 : pr;
              $('#results-list').append(`
                <div class="bg-white p-3 rounded shadow-sm cursor-pointer" data-id="${id}">
                  <div class="flex gap-3 items-center">
                    <img src="${img}" class="w-20 h-16 object-cover rounded" onerror="this.onerror=null;this.src='${PLACEHOLDER}';">
                    <div>
                      <div class="font-medium">${escapeHtml(name)}</div>
                      <div class="text-green-600 font-bold">₹${pricedisp}</div>
                    </div>
                  </div>
                </div>`);
            });
            $('#search-results').removeClass('hidden');
          }
        })
        .fail(() => { toast('Search failed'); });
    }

    $('#search-input').on('keydown', function(e){
      if (e.key === 'Enter'){
        e.preventDefault();
        const q = $(this).val().trim();
        $('#suggestions').hide();
        doSearchAndHandle(q);
      } else if (e.key === 'ArrowDown'){
        const $first = $('#suggestions .suggestion-item').first();
        if ($first.length) $first.focus();
      }
    });

    $('#search-btn').on('click', function(){
      const q = $('#search-input').val().trim();
      $('#suggestions').hide();
      doSearchAndHandle(q);
    });

    $('#results-list').on('click', '[data-id]', function(){
      const id = $(this).data('id'); if (id) window.location.href = 'product-details.html?productId=' + encodeURIComponent(id);
    });

    $('#brand').on('click', () => window.location.href = 'navbar.html');

    $('#cart-link').on('click', () => {
      const user = localStorage.getItem('user');
      if (!user){ alert('Please login to view cart'); window.location.href = 'login.html'; return; }
      window.location.href = 'cart.html';
    });

    $('#nav-login').on('click', () => {
       if (localStorage.getItem('user')) window.location.href = 'navbar.html';
       else window.location.href = 'login.html';
      });
    $('#nav-register').on('click', () => {
      if (localStorage.getItem('user')) window.location.href = 'navbar.html';
      else window.location.href = 'register.html';
    });
    $('#nav-profile').on('click', () => {
       if (!localStorage.getItem('user')) {
        alert('Please login');
        window.location.href = 'login.html';
      }
      else window.location.href = 'profile.html';
    });
    $('#nav-logout').on('click', function(){
      localStorage.clear();
      localStorage.removeItem('buyNowItem');
      refreshBadge();
      updateAuthUI();
      toast('You have logged out');
      setTimeout(() => window.location.href = 'login.html', 700);
    });

    $(document).on('click', function(e){
       if (!$(e.target).closest('#suggestions, #search-input, #search-btn').length) $('#suggestions').hide();
      });

    fetchAllProductsFallback();

    function updateAuthUI(){
      const logged = !!localStorage.getItem('user');
      if (logged){
        $('#nav-login, #nav-register').hide();
        $('#nav-profile, #nav-logout').show();
      }
      else {
        $('#nav-login, #nav-register').show();
        $('#nav-profile, #nav-logout').hide();
      }
    }

    updateAuthUI();
  });

})(jQuery);
