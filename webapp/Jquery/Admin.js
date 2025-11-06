$(document).ready(function () {
  const categoryIdNameMap = {};
  const userIdNameMap = {};

  // ------------------ IMAGE HANDLING ------------------
  function resolveImageUrl(imagePath) {
    if (!imagePath) return '';
    imagePath = String(imagePath).trim();
    if (!imagePath) return '';

    // Already a data URI
    if (/^data:/i.test(imagePath)) return imagePath;

    // Base64 raw (no data: prefix) -> add prefix heuristically
    if (isProbablyBase64(imagePath)) return prefixDataUriIfNeeded(imagePath);

    // Absolute http(s)
    if (/^https?:\/\//i.test(imagePath)) return imagePath;

    const origin = window.location.origin;
    if (imagePath.startsWith('/')) return origin + imagePath;
    if (imagePath.indexOf('Ecommerce_Website') !== -1) {
      return origin + (imagePath.startsWith('/') ? imagePath : '/' + imagePath);
    }
    return origin + '/Ecommerce_Website/' + imagePath.replace(/^\/+/, '');
  }

  function prefixDataUriIfNeeded(b64) {
    if (!b64) return '';
    if (b64.startsWith('/9j/')) return 'data:image/jpeg;base64,' + b64;
    if (b64.startsWith('iVBOR')) return 'data:image/png;base64,' + b64;
    if (b64.startsWith('R0lG')) return 'data:image/gif;base64,' + b64;
    return 'data:image/jpeg;base64,' + b64; // default
  }

  function isProbablyBase64(s) {
    if (!s) return false;
    if (s.length < 50) return false;
    return /^[A-Za-z0-9+/]+=*$/.test(s) && !/\s/.test(s);
  }

  // ------------------ MODAL ------------------
  function showModal(id) {
    $(id).addClass('open').attr('aria-hidden', 'false');
    $('body').css('overflow', 'hidden');
  }

  function hideModal(id) {
    $(id).removeClass('open').attr('aria-hidden', 'true');
    if ($('.modal-overlay.open').length === 0) $('body').css('overflow', '');
  }

  function toast(msg) {
    $('#toast').text(msg).fadeIn(200).delay(1400).fadeOut(400);
  }

  $('.sidebar-btn').click(function () {
    const target = $(this).data('target');
    $('.panel').addClass('hidden');
    $('#view-' + target).removeClass('hidden');
    $('.sidebar .nav-item').removeClass('active');
    $(this).addClass('active');
  });

  // ------------------ DATA TABLES ------------------
  const productsTable = $('#productsTable').DataTable({
    ajax: {
      url: '/Ecommerce_Website/AdminServlet',
      type: 'POST',
      data: { action: 'listProducts' },
      dataSrc: ''
    },
    columns: [
      {
        data: null,
        render: function (r) {
          const imagePath = r.image ?? r.imageUrl ?? r.image_url ?? r.img ?? '';
          const resolved = resolveImageUrl(imagePath);
          const imgHtml = resolved
            ? `<img src="${resolved}" style="height:48px;max-width:64px;object-fit:cover;border-radius:6px;margin-right:10px;">`
            : `<div style="width:64px;height:48px;background:#f3f4f6;border-radius:6px;display:inline-block;margin-right:10px;"></div>`;
          const title = r.productName ?? r.product_name ?? r.name ?? '';
          const desc = r.description ?? r.desc ?? '';
          return `<div style="display:flex;align-items:center">${imgHtml}<div><strong>${escapeHtml(title)}</strong><div class="muted" style="font-size:12px">${escapeHtml(desc)}</div></div></div>`;
        }
      },
      { data: 'categoryName', defaultContent: '' },
      {
        data: 'price',
        render: function (d) {
          if (d == null || d === '') return '₹0';
          return '₹' + parseFloat(d).toFixed(2);
        }
      },
      { data: 'stock', defaultContent: '-' },
      {
        data: null,
        orderable: false,
        render: function (r) {
          const id = r.productId ?? r.product_id ?? '';
          return ''
            + '<button class="edit-btn bg-blue-500 text-white px-2 py-1 rounded" data-id="'
            + id
            + '" type="button">Edit</button>'
            + '<button class="delete-btn bg-red-500 text-white px-2 py-1 rounded ml-1" data-id="'
            + id
            + '" type="button">Delete</button>';
        }
      }
    ],
    columnDefs: [
      {
        targets: 1,
        render: function (data, type, row) {
          const serverName = (data && String(data).trim() !== '')
            ? data
            : (row.categoryName ?? row.category_name ?? null);
          const cid = row.categoryId ?? row.category_id ?? null;

          if (serverName && String(serverName).trim() !== '') {
            const catPattern = /^Category\s*(\d+)$/i;
            const match = String(serverName).match(catPattern);
            if (match && cid != null && categoryIdNameMap[cid]) {
              return ''
                + categoryIdNameMap[cid]
                + ' <span class="text-xs text-gray-400">('
                + cid
                + ')</span>';
            }
            if (match && match[1] && categoryIdNameMap[match[1]]) {
              return ''
                + categoryIdNameMap[match[1]]
                + ' <span class="text-xs text-gray-400">('
                + match[1]
                + ')</span>';
            }
            return cid != null
              ? serverName + ' <span class="text-xs text-gray-400">(' + cid + ')</span>'
              : serverName;
          }

          if (cid != null && categoryIdNameMap[cid]) {
            return categoryIdNameMap[cid] + ' <span class="text-xs text-gray-400">(' + cid + ')</span>';
          }

          if (cid != null && typeof CATEGORY_NAMES !== 'undefined' && CATEGORY_NAMES[cid]) {
            return CATEGORY_NAMES[cid] + ' <span class="text-xs text-gray-400">(' + cid + ')</span>';
          }

          return cid != null ? 'Category ' + cid : '—';
        }
      }
    ],
    dom: 'Bfrtip',
    buttons: [
      {
        extend: 'csv',
        filename: 'products_details',
        exportOptions: { columns: [0, 1, 2, 3] }
      },
      {
        extend: 'excel',
        filename: 'products_details',
        exportOptions: { columns: [0, 1, 2, 3] }
      }
    ],
    pageLength: 10
  });

  // ------------------ FILTERING ------------------
  $.fn.dataTable.ext.search.push(function (settings, data, dataIndex) {
    if (settings.nTable && settings.nTable.id !== 'productsTable') return true;
    const selectedCat = $('#categoryFilter').val();
    if (!selectedCat) return true;
    const row = productsTable.row(dataIndex).data();
    if (!row) return true;
    const cid = row.categoryId ?? row.category_id ?? row.catId ?? row.cat ?? null;
    if (cid != null) return String(cid) === String(selectedCat);
    const selectedName = $('#categoryFilter option:selected').text();
    const cname = row.categoryName ?? row.category_name ?? row.catName ?? row.category ?? '';
    return cname && selectedName ? String(cname).trim() === String(selectedName).trim() : false;
  });

  $('#categoryFilter').off('change').on('change', function () {
    productsTable.draw();
  });

  productsTable.on('xhr', function (e, settings, json) {
    const $filter = $('#categoryFilter');
    const $select = $('#categorySelect');
    if ($filter.length) $filter.find('option:not(:first)').remove();
    if ($select.length) $select.find('option:not(:first)').remove();

    (json || []).forEach(function (p) {
      const cid = p.categoryId ?? p.category_id ?? null;
      const cname = p.categoryName ?? p.category_name ?? p.catName ?? p.category ?? null;
      if (cid != null && cname) categoryIdNameMap[cid] = cname;
    });

    const entries = Object.entries(categoryIdNameMap || {}).sort((a, b) => String(a[1]).localeCompare(b[1]));
    entries.forEach(pair => {
      const id = pair[0];
      const name = pair[1];
      if ($('#categoryFilter').length) $('#categoryFilter').append('<option value="' + id + '">' + name + '</option>');
      if ($('#categorySelect').length) $('#categorySelect').append('<option value="' + id + '">' + name + '</option>');
    });

    try { productsTable.rows().invalidate().draw(false); } catch (e) { }
  });

  // ------------------ USERS TABLE ------------------
  const usersTable = $('#usersTable').DataTable({
    ajax: {
      url: '/Ecommerce_Website/AdminServlet',
      type: 'GET',
      data: { action: 'listUsers' },
      dataSrc: ''
    },
    columns: [
      { data: 'name' },
      { data: 'email' },
      { data: 'mobile' },
      {
        data: 'status',
        render: function (d) {
          return d == 0 ? '<span class="text-red-600">Inactive</span>' : '<span class="text-green-600">Active</span>';
        }
      },
      {
        data: null,
        render: function (r) {
          const id = r.id ?? r.userId ?? r.user_id;
          if (r.status == 0 || r.is_active == 0) {
            return '<button class="activate-user-btn bg-green-500 text-white px-2 py-1 rounded" data-id="'
              + id + '" type="button">Activate</button>';
          }
          return '<button class="deactivate-btn bg-yellow-500 text-white px-2 py-1 rounded" data-id="'
            + id + '" type="button">Deactivate</button>';
        },
        className: 'not-export-col'
      }
    ],
    dom: 'Bfrtip',
    buttons: [
      { extend: 'csv', filename: 'users_details', exportOptions: { columns: [0, 1, 2] } },
      { extend: 'excel', filename: 'users_details', exportOptions: { columns: [0, 1, 2] } }
    ],
    pageLength: 10
  });

  // ------------------ ORDERS TABLE ------------------
  const ordersTable = $('#ordersTable').DataTable({
    ajax: {
      url: '/Ecommerce_Website/AdminServlet',
      type: 'GET',
      data: { action: 'listOrders' },
      dataSrc: ''
    },
    columns: [
      { data: 'orderId' },
      {
        data: null,
        render: function (r) {
          const name = r.username ?? r.userName ?? r.user ?? null;
          if (name && String(name).trim() !== '') return name;
          const embeddedUser = r.userObj ?? r.userObject ?? r.customer ?? null;
          const embeddedName = embeddedUser
            ? (embeddedUser.username ?? embeddedUser.name ?? embeddedUser.fullname ?? embeddedUser.email ?? null)
            : null;
          if (embeddedName && String(embeddedName).trim() !== '') {
            if (r.userId ?? r.user_id) userIdNameMap[r.userId ?? r.user_id] = embeddedName;
            return embeddedName;
          }
          const uid = r.userId ?? r.user_id ?? null;
          if (uid != null && userIdNameMap[uid]) return userIdNameMap[uid];
          return '—';
        }
      },
      { data: 'totalAmount', render: d => d == null ? '₹0' : '₹' + d },
      { data: 'orderDate', render: d => { try { return new Date(d).toLocaleString(); } catch (e) { return d; } } },
      { data: 'status', render: d => d === 'Pending'
          ? '<span class="text-yellow-600 font-semibold">Pending</span>'
          : '<span class="text-green-600 font-semibold">Received</span>' }
    ],
    dom: 'Bfrtip',
    buttons: [
      { extend: 'csv', filename: 'orders_details', exportOptions: { columns: [0,1,2,3] } },
      { extend: 'excel', filename: 'orders_details', exportOptions: { columns: [0,1,2,3] } }
    ],
    pageLength: 10
  });

  ordersTable.on('xhr', function (e, settings, json) {
    (json || []).forEach(function (o) {
      const uid = o.userId ?? o.user_id ?? (o.userObj && (o.userObj.id ?? o.userObj.userId)) ?? null;
      const uname = o.username ?? o.userName ?? o.user ?? (o.userObj && (o.userObj.username ?? o.userObj.name ?? o.userObj.fullname ?? o.userObj.email)) ?? null;
      if (uid != null && uname) userIdNameMap[uid] = uname;
    });
  });

  // ------------------ LOAD ADMIN ------------------
  $.get('/Ecommerce_Website/AdminServlet', { action: 'getAdmin' }, function (admin) {
    if (admin && (admin.username || admin.name)) $('#adminName').text(admin.username || admin.name);
  }, 'json');

  // ------------------ PRODUCT MODAL ------------------
  $('#addProductBtn, #addProductBtnTop').click(function () {
    $('#productModalTitle').text('Add Product');
    $('#productForm')[0].reset();
    $('#productId').val('');
    $('#imagePreview').hide();
    $('#fileLabel').text('Select image file');
    $('#imageFile').val('');
    $('#image').val('');
    showModal('#productModal');
  });

  $('.modal-overlay').click(function (e) { if (e.target === this) hideModal('#' + this.id); });
  $('#closeProductModal').click(() => hideModal('#productModal'));

  // IMAGE FILE PREVIEW
  $('#imageFile').on('change', function () {
    const file = this.files && this.files[0];
    if (!file) {
      $('#imagePreview').hide();
      $('#fileLabel').text('Select image file');
      return;
    }
    $('#fileLabel').text(file.name);
    const reader = new FileReader();
    reader.onload = function (ev) { $('#imagePreview').attr('src', ev.target.result).show(); };
    reader.readAsDataURL(file);
    $('#image').val(''); // clear legacy input
  });

  $('#image').on('input', function () {
    const raw = $(this).val().trim();
    if (!raw) { $('#imagePreview').hide(); return; }
    $('#imagePreview').attr('src', resolveImageUrl(raw)).show();
    $('#imageFile').val('');
    $('#fileLabel').text('Select image file');
  });

  // ------------------ PRODUCT FORM SUBMIT ------------------
  $('#productForm').submit(function (e) {
    e.preventDefault();
    const pid = $('#productId').val();
    const action = pid ? 'updateProduct' : 'addProduct';

    const stockVal = Number($('#stock').val());
    if (!Number.isFinite(stockVal) || stockVal < 0) { toast('Stock cannot be negative'); return; }

    const priceVal = Number($('#price').val());
    if (!Number.isFinite(priceVal) || priceVal <= 0) { toast('Price must be > 0'); return; }

    const fd = new FormData(this);
    fd.append('action', action);
    if (pid) fd.append('productId', pid);

    // Only append image if file selected
    const file = $('#imageFile')[0].files[0];
    if (file) fd.set('image', file); // override legacy text input if file selected

    $.ajax({
      url: '/Ecommerce_Website/AdminServlet',
      method: 'POST',
      data: fd,
      cache: false,
      contentType: false,
      processData: false,
      success: function (res) {
        if (res && res.status === 'ok') {
          toast('Saved');
          hideModal('#productModal');
          try { productsTable.ajax.reload(null, false); } catch (e) { location.reload(); }
        } else {
          toast('Error: ' + (res?.error ?? 'Save failed'));
        }
      },
      error: function (xhr) {
        let msg = 'Server error';
        try { msg = xhr.responseJSON?.error || JSON.parse(xhr.responseText)?.error || msg; } catch (e) {}
        toast(msg);
      }
    });
  });

  // ------------------ EDIT PRODUCT ------------------
  $('#productsTable').on('click', '.edit-btn', function () {
    const id = $(this).data('id');
    const row = productsTable.rows().data().toArray().find(r => (r.productId == id) || (r.product_id == id));
    if (!row) { toast('Product not found'); return; }

    const imagePath = row.image ?? row.imageUrl ?? row.image_url ?? row.img ?? '';
    const imageurl = resolveImageUrl(imagePath);

    $('#productModalTitle').text('Edit Product');
    $('#productId').val(row.productId ?? row.product_id ?? '');
    $('#productName').val(row.productName ?? row.product_name ?? '');
    const cid = row.categoryId ?? row.category_id ?? row.catId ?? '';
    if (cid) $('#categorySelect').val(cid);

    $('#description').val(row.description ?? row.desc ?? '');
    $('#price').val(row.price ?? row.productPrice ?? '');
    $('#stock').val(row.stock ?? row.qty ?? '');
    $('#image').val(imagePath);

    if (imageurl) $('#imagePreview').attr('src', imageurl).show(); else $('#imagePreview').hide();

    $('#imageFile').val('');
    $('#fileLabel').text('Select image file');

    showModal('#productModal');
  });

  // ------------------ DELETE PRODUCT ------------------
  $('#productsTable').on('click', '.delete-btn', function () {
    const id = $(this).data('id');
    if (!confirm('Delete this product?')) return;

    $.post('/Ecommerce_Website/AdminServlet', { action: 'deleteProduct', productId: id }, function (res) {
      if (res?.status === 'ok') { toast('Deleted'); productsTable.ajax.reload(null, false); }
      else toast('Failed to delete');
    }, 'json');
  });

  // ------------------ USERS ------------------
  $('#usersTable').on('click', '.activate-user-btn', function () {
    const uid = $(this).data('id');
    $.post('/Ecommerce_Website/AdminServlet', { action: 'activateUser', userId: uid }, function (res) {
      if (res?.status === 'ok') { toast('Activated'); usersTable.ajax.reload(null, false); }
      else toast('Failed');
    }, 'json');
  });

  $('#usersTable').on('click', '.deactivate-btn', function () {
    const uid = $(this).data('id');
    if (!confirm('Deactivate user?')) return;
    $.post('/Ecommerce_Website/AdminServlet', { action: 'deactivateUser', userId: uid }, function (res) {
      if (res?.status === 'ok') { toast('Deactivated'); usersTable.ajax.reload(null, false); }
      else toast('Failed');
    }, 'json');
  });

  // ------------------ HELPER ------------------
  function escapeHtml(text) {
    if (!text) return '';
    return text.replace(/[\"&'\/<>]/g, function (a) {
      return { '"': '&quot;', '&': '&amp;', "'": '&#39;', '/': '&#47;', '<': '&lt;', '>': '&gt;' }[a];
    });
  }
});
