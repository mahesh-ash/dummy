$(document).ready(function () {
  const categoryIdNameMap = {};
  const userIdNameMap = {};
  let productsTable = null;
  let usersTable = null;
  let ordersTable = null;

  // ------------------ IMAGE HANDLING ------------------
  function resolveImageUrl(imagePath) {
    if (!imagePath) return '';
    imagePath = String(imagePath).trim();
    if (!imagePath) return '';

    // Already a data URI
    if (/^data:/i.test(imagePath)) return imagePath;

    // Raw base64 (very simple heuristic) -> don't try to be fancy here
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
    const head = b64.substring(0, 8);
    if (head.startsWith('/9j/')) return 'data:image/jpeg;base64,' + b64;
    if (head.startsWith('iVBOR')) return 'data:image/png;base64,' + b64;
    if (head.startsWith('R0lG')) return 'data:image/gif;base64,' + b64;
    return 'data:image/jpeg;base64,' + b64; // default
  }

  function isProbablyBase64(s) {
    if (!s) return false;
    const trimmed = String(s).trim();
    if (trimmed.length < 50) return false;
    // allow padding = and no whitespace
    return /^[A-Za-z0-9+/=]+$/.test(trimmed) && !/\s/.test(trimmed);
  }

  // ------------------ MODAL / TOAST ------------------
  function showModal(id) {
    $(id).addClass('open').attr('aria-hidden', 'false');
    $('body').css('overflow', 'hidden');
  }
  function hideModal(id) {
    $(id).removeClass('open').attr('aria-hidden', 'true');
    if ($('.modal-overlay.open').length === 0) {
      $('body').css('overflow', '');
    }
  }
  function toast(msg) {
    if ($('#toast').length === 0) {
      $('body').append('<div id="toast" style="position:fixed;top:20px;right:20px;display:none;z-index:10000;background:#111827;color:#fff;padding:8px 12px;border-radius:6px;"></div>');
    }
    $('#toast').stop(true,true).text(msg).fadeIn(200).delay(1400).fadeOut(400);
  }

  $('.sidebar-btn').on('click', function () {
    const target = $(this).data('target');
    $('.panel').addClass('hidden');
    $('#view-' + target).removeClass('hidden');
    $('.sidebar .nav-item').removeClass('active');
    $(this).addClass('active');
  });

  // ------------------ LOAD MAPPINGS (must run before table init) ------------------
  function loadAdminCategories() {
    return $.getJSON('/Ecommerce_Website/AdminServlet', { action: 'listCategories' })
      .done(function (data) {
        // clear selects (leave first option)
        const $catSelect = $('#categorySelect');
        const $catFilter = $('#categoryFilter');
        if ($catSelect.length) $catSelect.find('option:not(:first)').remove();
        if ($catFilter.length) $catFilter.find('option:not(:first)').remove();

        (data || []).forEach(function (c) {
          // accept various shapes returned by backend
          const id = (typeof c.category_id !== 'undefined') ? c.category_id : (c.categoryId ?? c.id);
          const name = (typeof c.category_name !== 'undefined') ? c.category_name : (c.categoryName ?? c.name);
          if (typeof id !== 'undefined' && typeof name !== 'undefined') {
            categoryIdNameMap[String(id)] = name;
            const option = '<option value="' + id + '">' + escapeHtml(name) + '</option>';
            if ($catSelect.length) $catSelect.append(option);
            if ($catFilter.length) $catFilter.append(option);
          }
        });
      })
      .fail(function () {
        console.error('Failed to load admin categories');
      });
  }

  function loadAdminUsers() {
    return $.getJSON('/Ecommerce_Website/AdminServlet', { action: 'listUsers' })
      .done(function (data) {
        (data || []).forEach(function (u) {
          const id = u.id ?? u.userId ?? u.user_id;
          const name = u.name ?? u.fullname ?? u.username ?? u.email;
          if (typeof id !== 'undefined' && typeof name !== 'undefined') {
            userIdNameMap[String(id)] = name;
          }
        });
      })
      .fail(function () {
        console.error('Failed to load admin users for mapping');
      });
  }

  // ------------------ INIT DATATABLES (after categories/users loaded) ------------------
  function initDataTables() {
    // PRODUCTS
    productsTable = $('#productsTable').DataTable({
      ajax: {
        url: '/Ecommerce_Website/AdminServlet',
        type: 'POST',
        data: { action: 'listProducts' },
        dataSrc: ''
      },
      columns: [
        // product column with small image + name + desc
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
            // prefer explicit server-provided name, or lookup by id in the map
            const serverName = (data && String(data).trim() !== '')
              ? data
              : (row.categoryName ?? row.category_name ?? null);
            const cid = row.categoryId ?? row.category_id ?? null;

            if (serverName && String(serverName).trim() !== '') {
              const catPattern = /^Category\s*(\d+)$/i;
              const match = String(serverName).match(catPattern);
              if (match && cid != null && categoryIdNameMap[cid]) {
                return categoryIdNameMap[cid] + ' <span class="text-xs text-gray-400">(' + cid + ')</span>';
              }
              if (match && match[1] && categoryIdNameMap[match[1]]) {
                return categoryIdNameMap[match[1]] + ' <span class="text-xs text-gray-400">(' + match[1] + ')</span>';
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
        { extend: 'csv', filename: 'products_details', exportOptions: { columns: [0, 1, 2, 3] } },
        { extend: 'excel', filename: 'products_details', exportOptions: { columns: [0, 1, 2, 3] } }
      ],
      pageLength: 10
    });

    // PRODUCTS: update category select/filter when ajax returns
    productsTable.on('xhr', function (e, settings, json) {
      // ensure categoryFilter/select exist, and options keep the server mapping as priority
      const $filter = $('#categoryFilter');
      const $select = $('#categorySelect');

      // populate local map from returned products (will not remove existing names from admin list)
      (json || []).forEach(function (p) {
        const cid = (typeof p.categoryId !== 'undefined') ? p.categoryId : (p.category_id ?? null);
        const cname = p.categoryName ?? p.category_name ?? p.catName ?? p.category ?? null;
        if (cid != null && cname) {
          if (!categoryIdNameMap[String(cid)]) {
            categoryIdNameMap[String(cid)] = cname;
          }
        }
      });

      // repopulate filter/select options (keeping admin categories + any discovered)
      if ($filter.length) $filter.find('option:not(:first)').remove();
      if ($select.length) $select.find('option:not(:first)').remove();

      const entries = Object.entries(categoryIdNameMap || {}).sort(function (a, b) { return String(a[1]).localeCompare(b[1]); });
      entries.forEach(function (pair) {
        const id = pair[0];
        const name = pair[1];
        if ($filter.length) $filter.append('<option value="' + id + '">' + escapeHtml(name) + '</option>');
        if ($select.length) $select.append('<option value="' + id + '">' + escapeHtml(name) + '</option>');
      });

      // redraw table rows (but keep paging)
      try { productsTable.rows().invalidate().draw(false); } catch (err) { /* ignore */ }
    });

    // ------------------ USERS TABLE ------------------
    usersTable = $('#usersTable').DataTable({
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
    ordersTable = $('#ordersTable').DataTable({
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
              if (r.userId ?? r.user_id) userIdNameMap[String(r.userId ?? r.user_id)] = embeddedName;
              return embeddedName;
            }
            const uid = r.userId ?? r.user_id ?? null;
            if (uid != null && userIdNameMap[String(uid)]) return userIdNameMap[String(uid)];
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
        { extend: 'csv', filename: 'orders_details', exportOptions: { columns: [0, 1, 2, 3] } },
        { extend: 'excel', filename: 'orders_details', exportOptions: { columns: [0, 1, 2, 3] } }
      ],
      pageLength: 10
    });

    ordersTable.on('xhr', function (e, settings, json) {
      (json || []).forEach(function (o) {
        const uid = o.userId ?? o.user_id ?? (o.userObj && (o.userObj.id ?? o.userObj.userId)) ?? null;
        const uname = o.username ?? o.userName ?? o.user ?? (o.userObj && (o.userObj.username ?? o.userObj.name ?? o.userObj.fullname ?? o.userObj.email)) ?? null;
        if (uid != null && uname) userIdNameMap[String(uid)] = uname;
      });
    });
  }

  // ------------------ FILTER HOOK ------------------
  $.fn.dataTable.ext.search.push(function (settings, data, dataIndex) {
    if (settings.nTable && settings.nTable.id !== 'productsTable') return true;
    const selectedCat = $('#categoryFilter').val();
    if (!selectedCat) return true;
    if (!productsTable) return true;
    const row = productsTable.row(dataIndex).data();
    if (!row) return true;
    const cid = row.categoryId ?? row.category_id ?? row.catId ?? row.cat ?? null;
    if (cid != null) {
      return String(cid) === String(selectedCat);
    }
    const selectedName = $('#categoryFilter option:selected').text();
    const cname = row.categoryName ?? row.category_name ?? row.catName ?? row.category ?? '';
    if (cname && selectedName) {
      return String(cname).trim() === String(selectedName).trim();
    }
    return false;
  });

  $('#categoryFilter').off('change').on('change', function () {
    if (productsTable) productsTable.draw();
  });

  // ------------------ BOOTSTRAP: load maps THEN init tables ------------------
  $.when(loadAdminCategories(), loadAdminUsers()).always(function () {
    // init datatables only after categories/users loaded (ensures category select is populated)
    initDataTables();
  });

  // ------------------ ADMIN INFO ------------------
  $.get('/Ecommerce_Website/AdminServlet', { action: 'getAdmin' }, function (admin) {
    if (admin && (admin.username || admin.name)) {
      $('#adminName').text(admin.username || admin.name);
    }
  }, 'json');

  // ------------------ PRODUCT MODAL ------------------
  $('#addProductBtn, #addProductBtnTop').on('click', function () {
    $('#productModalTitle').text('Add Product');
    $('#productForm')[0].reset();
    $('#productId').val('');
    $('#imagePreview').hide();
    $('#fileLabel').text('Select image file');
    $('#imageFile').val('');
    $('#image').val('');
    showModal('#productModal');
  });

  $('.modal-overlay').on('click', function (e) { if (e.target === this) hideModal('#' + this.id); });
  $('#closeProductModal').on('click', function () { hideModal('#productModal'); });

  // IMAGE FILE PREVIEW
  $('#imageFile').on('change', function () {
    const file = this.files && this.files[0];
    if (!file) {
      $('#imagePreview').hide();
      $('#fileLabel').text('Select image file');
      return;
    }
    $('#fileLabel').text(file.name);
    try {
      const url = URL.createObjectURL(file);
      $('#imagePreview').attr('src', url).show();
    } catch (e) {
      const reader = new FileReader();
      reader.onload = function (ev) { $('#imagePreview').attr('src', ev.target.result).show(); };
      reader.readAsDataURL(file);
    }
    // set a potential server path - optional (you currently post file or text depending on flow)
    const serverPath = '/Ecommerce_Website/Assets/' + file.name;
    $('#image').val(serverPath);
  });

  $('#image').on('input', function () {
    const raw = $(this).val().trim();
    if (!raw) { $('#imagePreview').hide(); return; }
    const resolved = resolveImageUrl(raw);
    $('#imagePreview').attr('src', resolved).show();
    $('#imageFile').val('');
    $('#fileLabel').text('Select image file');
  });

  // ------------------ PRODUCT FORM SUBMIT ------------------
  $('#productForm').on('submit', function (e) {
    e.preventDefault();
    const pid = $('#productId').val();
    const action = pid ? 'updateProduct' : 'addProduct';
    const stockVal = Number($('#stock').val());
    if (!Number.isFinite(stockVal) || stockVal < 0) { toast('Stock cannot be negative'); return; }
    const priceVal = Number($('#price').val());
    if (!Number.isFinite(priceVal) || priceVal < 0) { toast('Price must be >= 0'); return; }

    // prefer FormData if file upload possible
    const formEl = this;
    const fd = new FormData(formEl);
    fd.append('action', action);
    const file = $('#imageFile')[0].files[0];
    if (file) fd.set('image', file);

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

$('#productsTable').on('click', '.edit-btn', function () {
    const id = $(this).data('id');
    if (!productsTable) { toast('Table not initialized'); return; }
    const rows = productsTable.rows().data().toArray();
    const row = rows.find(function (r) { return (r.productId == id) || (r.product_id == id); });
    if (!row) { toast('Product not found'); return; }

    const imagePath = row.image ?? row.imageUrl ?? row.image_url ?? row.img ?? '';
    const imageurl = resolveImageUrl(imagePath);

    $('#productModalTitle').text('Edit Product');
    $('#productId').val(row.productId ?? row.product_id ?? '');
    $('#productName').val(row.productName ?? row.product_name ?? '');
    const cid = row.categoryId ?? row.category_id ?? row.catId ?? '';
    if (cid) {
      $('#categorySelect').val(cid);
    } else {
      // try matching by name
      const cname = row.categoryName ?? row.category_name ?? row.catName ?? null;
      if (cname) {
        $('#categorySelect option').filter(function () {
          return $(this).text().trim() === cname.trim();
        }).prop('selected', true);
      }
    }
    $('#description').val(row.description ?? row.desc ?? '');
    $('#price').val(row.price ?? row.productPrice ?? '');
    $('#stock').val(row.stock ?? row.qty ?? '');
    $('#image').val(imagePath);
    if (imageurl && imageurl.trim() !== '') {
      $('#imagePreview').attr('src', imageurl).show();
    } else {
      $('#imagePreview').hide();
    }
    $('#imageFile').val('');
    $('#fileLabel').text('Select image file');
    showModal('#productModal');
  });

  $('#productsTable').on('click', '.delete-btn', function () {
    const id = $(this).data('id');
    if (!confirm('Delete product?')) return;
    $.post('/Ecommerce_Website/AdminServlet', { action: 'deleteProduct', productId: id }, function (res) {
      if (res && res.status === 'ok') {
        toast('Deleted');
        productsTable.ajax.reload();
      } else {
        toast('Delete failed');
      }
    }, 'json').fail(function () { toast('Server error'); });
  });

  // ------------------ USERS ACTIONS ------------------
  $('#usersTable').on('click', '.deactivate-btn', function () {
    const id = $(this).data('id');
    if (!confirm('Deactivate user?')) return;
    $.post('/Ecommerce_Website/AdminServlet', { action: 'toggleStatus', userId: id, active: false }, function (res) {
      if (res && res.status === 'ok') {
        toast('Updated');
        usersTable.ajax.reload();
        loadAdminUsers();
      } else toast('Failed');
    }, 'json').fail(function () { toast('Server error'); });
  });

  $('#usersTable').on('click', '.activate-user-btn', function () {
    const id = $(this).data('id');
    if (!confirm('Activate user?')) return;
    $.post('/Ecommerce_Website/AdminServlet', { action: 'toggleStatus', userId: id, active: true }, function (res) {
      if (res && res.status === 'ok') {
        toast('Updated');
        usersTable.ajax.reload();
        loadAdminUsers();
      } else toast('Failed');
    }, 'json').fail(function () { toast('Server error'); });
  });

  $('#usersTable').on('click', '.delete-user-btn', function () {
    const id = $(this).data('id');
    if (!confirm('Remove user (soft-delete)?')) return;
    $.post('/Ecommerce_Website/AdminServlet', { action: 'deleteUser', userId: id }, function (res) {
      if (res && res.status === 'ok') {
        toast('User deactivated');
        usersTable.ajax.reload();
        loadAdminUsers();
      } else toast('Failed');
    }, 'json').fail(function () { toast('Server error'); });
  });

  // ------------------ CATEGORY MODAL & CRUD ------------------
  $('#addCategoryBtn').on('click', function () {
    $('#categoryModalTitle').text('Add Category');
    $('#categoryForm')[0].reset();
    $('#categoryId').val('');
    showModal('#categoryModal');
  });
  $('#closeCategoryModal').on('click', function () { hideModal('#categoryModal'); });

  $('#categoryForm').on('submit', function (e) {
    e.preventDefault();
    const cid = $('#categoryId').val();
    const action = cid ? 'updateCategory' : 'addCategory';
    const name = $('#categoryName').val().trim();
    if (!name) { toast('Category name required'); return; }
    const data = $(this).serialize() + '&action=' + action;
    $.post('/Ecommerce_Website/AdminServlet', data, function (res) {
      if (res && res.status === 'ok') {
        toast('Saved');
        hideModal('#categoryModal');
        loadAdminCategories().always(function () {
          try { productsTable.ajax.reload(null, false); } catch (e) {}
        });
      } else {
        toast('Save failed');
      }
    }, 'json').fail(function () { toast('Server error'); });
  });

  // ------------------ SIMPLE HELPERS ------------------
  function escapeHtml(text) {
    if (!text) return '';
    return String(text).replace(/[\"&'\/<>]/g, function (a) {
      return { '"': '&quot;', '&': '&amp;', "'": '&#39;', '/': '&#47;', '<': '&lt;', '>': '&gt;' }[a];
    });
  }

  // ------------------ ADMIN NAV ------------------
  $('#profileBtn').on('click', function () { window.location.href = '../Html/adminprofile.html'; });
  $('#logoutBtn').on('click', function () {
    if (confirm('Logout?')) {
      $.post('/Ecommerce_Website/AdminServlet', { action: 'logout' }, function () {
        window.location.href = '../Html/admin_login.html';
      });
    }
  });

  // end of document.ready
});
