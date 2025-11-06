
$(document).ready(function(){

  function showToast(msg, ms = 1600){
    if ($('#_toast_box').length === 0) $('body').append('<div id="_toast_box" style="position:fixed;top:18px;right:18px;z-index:9999;pointer-events:none"></div>');
    const $t = $('<div style="pointer-events:auto;margin-top:8px;padding:8px 12px;border-radius:8px;background:#111827;color:#fff;font-weight:600;opacity:0;transition:all .22s">'+msg+'</div>');
    $('#_toast_box').append($t);
    setTimeout(()=>$t.css({opacity:1,transform:'translateY(0)'}),10);
    setTimeout(()=>$t.css({opacity:0,transform:'translateY(-8px)'}),ms);
    setTimeout(()=>$t.remove(), ms+250);
  }

  function safeParse(str){
    if (!str || typeof str !== 'string') return null;
    try { return JSON.parse(str); } catch(e){ return null; }
  }

  function updateCartBadge(){
    const fallback = (safeParse(localStorage.getItem('cart')) || []).reduce((s,i)=>s+(i.qty||1),0);
    $.ajax({ url:'http://localhost:8080/Ecommerce_Website/CartServlet',
       method:'GET', 
       dataType:'json', 
       timeout:4000, 
       xhrFields:{
        withCredentials:true
      } })
      .done(function(data){ 
        if (!Array.isArray(data)) $('#cart-badge').text(fallback); else $('#cart-badge').text(data.reduce((s,i)=>s+(i.qty||0),0));
       })
      .fail(function(){
         $('#cart-badge').text(fallback);
         });
  }

  function refreshWishlistBadge(){
    const u = safeParse(localStorage.getItem('user'));
    const userId = u ? (u.userId || u.id || u.user_id) : null;
    $.ajax({
       url:'http://localhost:8080/Ecommerce_Website/WishlistServlet', 
       data:{count:1, userId:userId},
        dataType:'json',
        timeout:4000, 
        xhrFields:{
          withCredentials:true
        } })
      .done(function(res){ 
        if (res && typeof res.count !== 'undefined') $('#wishlist-badge').text(res.count); 
      })
      .fail(function(){

      });
  }

  const rawUser = localStorage.getItem('user');
  const user = safeParse(rawUser);
  if (!user) { window.location.href = 'login.html'; return; }

  $('#nav-logout').off('click').on('click', function(){
    $.ajax({
      url: 'http://localhost:8080/Ecommerce_Website/LogoutServlet',
      method: 'POST',
      dataType: 'json',
      timeout: 5000,
      xhrFields: { withCredentials: true }
    }).always(function(){
      try { localStorage.clear(); } catch(e){}
      try { localStorage.removeItem('buyNowItem'); } catch(e){}
      $('#cart-badge').text(0);
      refreshWishlistBadge();
      showToast('Logged out');
      setTimeout(()=> window.location.href = 'login.html', 700);
    });
  });

  let originalAmount = 0.00;
  let appliedCoupon = null; 

  function getParameterByName(name) {
    const regex = new RegExp('[?&]' + name + '=([^&#]*)');
    const results = regex.exec(window.location.href);
    return results ? decodeURIComponent(results[1]) : null;
  }

  function setAmounts(orig, newAmt, discount) {
    $('#originalAmountText').text('₹ ' + orig.toFixed(2));
    $('#payableAmount').text('₹ ' + newAmt.toFixed(2));
    $('#totalAmountText').text('₹ ' + newAmt.toFixed(2));
    if (discount && discount > 0) {
      $('#couponMsg').html('<span class="text-green-600">Coupon applied: saved ₹' + discount.toFixed(2) + '</span>');
    } else {
      $('#couponMsg').text('');
    }
  }

  function getSelectedProductIds() {
    const checkoutItemsJson = localStorage.getItem('checkoutItems');
    if (!checkoutItemsJson) return [];
    try { const checkoutItems = JSON.parse(checkoutItemsJson); return checkoutItems.map(item => item.productId); } catch(e){ return []; }
  }

function luhnCheck(num) {
    num = num.replace(/\D/g, '');
    if (num.length < 12 || num.length > 19) return false;
    let sum = 0, alt = false;
    for (let i = num.length - 1; i >= 0; i--) {
      let n = parseInt(num.charAt(i), 10);
      if (alt) { n *= 2; if (n > 9) n -= 9; }
      sum += n;
      alt = !alt;
    }
    return sum % 10 === 0;
  }

  function isValidCardNumber(num){
    return luhnCheck(num || '');
  }
  
  function restrictCardNameInput($el) {
    $el.on('input', function() {
      let val = $(this).val();
      val = val.replace(/[^A-Za-z\s]/g, '').replace(/\s+/g, ' ');
      $(this).val(val);
    });
  }

  function restrictCardNumberInput($el) {
    $el.on('input', function() {
      let val = $(this).val();
      let digits = val.replace(/\D/g, '');
      if (digits.length > 19) digits = digits.slice(0,19);
      $(this).val(digits);
    });
  }

  function validateCardForm(){
    let valid = true;
    const name = $('#cardName').val().trim();
    const num = $('#cardNumber').val();
    const month = parseInt($('#expMonth').val(),10);
    const year = parseInt($('#expYear').val(),10);
    const cvv = $('#cvv').val();
    const now = new Date();

    if(!name || !/^[A-Za-z]+(?:\s+[A-Za-z]+)*$/.test(name)) {
      valid=false; $('#cardName').addClass('input-error');
       $('#cardNameError').show();
    } else { 
      $('#cardName').removeClass('input-error');
       $('#cardNameError').hide(); 
    }

    if(!isValidCardNumber(num)){ 
      valid=false; $('#cardNumber').addClass('input-error');
       $('#cardNumberError').show(); 
    }
    else {
       $('#cardNumber').removeClass('input-error');
        $('#cardNumberError').hide(); 
      }

    if(!month||month<1||month>12||!year||year<now.getFullYear()||year>now.getFullYear()+20){
      valid=false;
       $('#expMonth, #expYear').addClass('input-error'); 
       $('#expMonthError, #expYearError').show();
    } else {
       $('#expMonth, #expYear').removeClass('input-error');
        $('#expMonthError, #expYearError').hide(); }

    if(!/^\d{3,4}$/.test(cvv)){ 
      valid=false; $('#cvv').addClass('input-error'); 
      $('#cvvError').show(); 
    } else {
       $('#cvv').removeClass('input-error'); 
       $('#cvvError').hide(); 
      }

    return valid;
  }

  function validateUpiForm(){
    const upi = $('#upiId').val().trim();
    if(!upi || !upi.includes('@')){ $('#upiId').addClass('input-error'); 
      $('#upiError').show(); 
      return false; 
    }
    $('#upiId').removeClass('input-error'); 
    $('#upiError').hide(); 
    return true;
  }

  function togglePayBtn(){
    const isCard = !$('#cardForm').hasClass('hidden');
    const selected = getSelectedProductIds().length>0;
    let valid = isCard ? validateCardForm() : validateUpiForm();
    $('#payBtn').prop('disabled', !(valid && selected));
  }

  function applyCoupon(code) {
    if (!code) { $('#couponMsg').text('Enter coupon code'); return; }
    const fetchUrl = (window.location.origin && window.location.origin !== 'null')
      ? (window.location.origin + '/Ecommerce_Website/PaymentServlet?action=validateCoupon')
      : 'http://localhost:8080/Ecommerce_Website/PaymentServlet?action=validateCoupon';

    $.ajax({
      url: fetchUrl,
      type: 'POST',
      contentType: 'application/json',
      data: JSON.stringify({ code: code, amount: originalAmount }),
      dataType: 'json',
      xhrFields: { withCredentials: true },
      success: function(resp) {
        if (resp && resp.status === 'ok' && resp.valid) {
          appliedCoupon = { code: code, discountAmount: resp.discountAmount, newAmount: resp.newAmount };
          setAmounts(originalAmount, appliedCoupon.newAmount, appliedCoupon.discountAmount);
          $('#applyCouponBtn').addClass('hidden');
          $('#removeCouponBtn').removeClass('hidden');
          $('#couponCode').val(code);
          $('#couponMsg').html('<span class="text-green-600">Coupon applied</span>');
          togglePayBtn();
        } else {
          const msg = resp && resp.message ? resp.message : 'Invalid coupon';
          $('#couponMsg').html('<span class="text-red-500">' + msg + '</span>');
        }
      },
      error: function(xhr) {
        let txt = 'Server error validating coupon';
        try {
          const json = JSON.parse(xhr.responseText);
          if (json && json.message) txt = json.message;
        } catch(e){}
        $('#couponMsg').html('<span class="text-red-500">' + txt + '</span>');
      }
    });
  }

  function loadAvailableCoupons() {
    const url = (window.location.origin && window.location.origin !== 'null')
      ? (window.location.origin + '/Ecommerce_Website/PaymentServlet?action=listCoupons')
      : 'http://localhost:8080/Ecommerce_Website/PaymentServlet?action=listCoupons';

    $.ajax({
      url: url,
      type: 'GET',
      dataType: 'json',
      timeout: 8000,
      xhrFields: { 
        withCredentials: true 
      },
      success: function(resp) {
        $('#couponList').empty();
        if (resp && resp.status === 'ok' && Array.isArray(resp.coupons) && resp.coupons.length > 0) {
          resp.coupons.forEach(function(c) {
            const $card = $('<div class="p-2 border rounded hover:shadow-sm">');
            if (c.applicable === true) {
              const $code = $('<div>').addClass('text-sm font-bold text-blue-600').text(c.code || '');
              const $label = $('<div>').addClass('text-xs text-gray-500').text((c.label || '') + (c.minAmount ? (' • min ₹' + c.minAmount) : ''));
              $card.append($code).append($label);
              $card.css('cursor', 'pointer');
              $card.on('click', function(){ applyCoupon(c.code); });
            } else {
              $card.addClass('bg-gray-50').addClass('opacity-90').css({'cursor':'not-allowed', 'border-color':'#e5e7eb'});
              const $code = $('<div>').addClass('text-sm font-bold text-gray-400').text(c.code || '');
              const $label = $('<div>').addClass('text-xs text-gray-400').text((c.label || '') + (c.minAmount ? (' • min ₹' + c.minAmount) : '') + ' (not applicable)');
              $card.append($code).append($label);
            }
            $('#couponList').append($card);
          });
        } else {
          $('#couponList').append('<div class="text-xs text-gray-400">No coupons available</div>');
        }
      },
      error: function(xhr, status, err) {
        $('#couponList').empty();
        if (status === 'timeout') {
          $('#couponList').append('<div class="text-xs text-red-500">Coupon service timed out</div>');
        } else {
          $('#couponList').append('<div class="text-xs text-red-500">Could not load coupons</div>');
        }
      }
    });
  }

  originalAmount = parseFloat(getParameterByName('amount')) || 0.00;
  setAmounts(originalAmount, originalAmount, 0);

  restrictCardNameInput($('#cardName'));
  restrictCardNumberInput($('#cardNumber'));
  $('#cvv').on('input', function(){ 
    this.value = this.value.replace(/\D/g,'').slice(0,4); 
  });

  try { 
    loadAvailableCoupons(); 
  } catch (e) {

  }

  $('#tabCard').click(() => {
    $('#cardForm').removeClass('hidden'); $('#upiForm').addClass('hidden');
    $('#tabCard').addClass('text-green-600 border-green-600').removeClass('text-gray-500');
    $('#tabUpi').removeClass('text-green-600 border-green-600').addClass('text-gray-500');
    togglePayBtn();
  });
  $('#tabUpi').click(() => {
    $('#upiForm').removeClass('hidden'); $('#cardForm').addClass('hidden');
    $('#tabUpi').addClass('text-green-600 border-green-600').removeClass('text-gray-500');
    $('#tabCard').removeClass('text-green-600 border-green-600').addClass('text-gray-500');
    togglePayBtn();
  });

  $('.upi-icon').click(function(){
    $('#tabUpi').trigger('click');
    const upiProvider = $(this).data('upi');
    $('#upiId').val(upiProvider+'@upi');
    togglePayBtn();
  });

  $('#cardForm input,#upiId').on('input', togglePayBtn);

  $('#applyCouponBtn').off('click').on('click', function(e){
    e.preventDefault();
    const code = $('#couponCode').val().trim();
    applyCoupon(code);
  });

  $('#removeCouponBtn').on('click', function(e){
    e.preventDefault();
    appliedCoupon = null;
    $('#couponCode').val('');
    setAmounts(originalAmount, originalAmount, 0);
    $('#removeCouponBtn').addClass('hidden');
    $('#applyCouponBtn').removeClass('hidden');
    $('#couponMsg').text('');
    togglePayBtn();
  });

  let countdownInterval = null;
  const PAYMENT_KEY = 'payment_in_progress';
  const PAYMENT_TIMEOUT_MS = 90*1000;
  let ajaxRequest = null;

  function startCountdown(expiresAt){
    clearInterval(countdownInterval);
    countdownInterval = setInterval(()=>{
      const remaining = Math.max(0, Math.floor((expiresAt-Date.now())/1000));
      const mm = String(Math.floor(remaining/60)).padStart(2,'0');
      const ss = String(remaining%60).padStart(2,'0');
      $('#timer').text(mm+':'+ss);
      if (remaining<=0){ clearInterval(countdownInterval); $('#loaderBox').addClass('hidden'); $('#payBtn').prop('disabled',false); localStorage.removeItem(PAYMENT_KEY); showToast('Payment timed out'); }
    }, 500);
  }

  const inProgress = safeParse(localStorage.getItem(PAYMENT_KEY));
  if (inProgress && inProgress.expiresAt && Date.now() < inProgress.expiresAt){
    $('#loaderBox').removeClass('hidden');
    $('#payBtn').prop('disabled', true);
    startCountdown(inProgress.expiresAt);
  } 
  else {
    localStorage.removeItem(PAYMENT_KEY);
  }

  $('#payBtn').click(function(e){
    e.preventDefault();
    if($('#payBtn').prop('disabled')) return;
    const isCard = !$('#cardForm').hasClass('hidden');
    const selectedProductIds = getSelectedProductIds();
    if(selectedProductIds.length===0) return Swal.fire('No items selected for payment');
    if(isCard && !validateCardForm()) return Swal.fire('Fix errors in card details');
    if(!isCard && !validateUpiForm()) return Swal.fire('Fix errors in UPI ID');

    $('#payBtn').prop('disabled',true);
    $('#loaderBox').removeClass('hidden');

    const now = Date.now();
    const expiresAt = now + PAYMENT_TIMEOUT_MS;
    localStorage.setItem(PAYMENT_KEY, JSON.stringify({ startedAt: now, expiresAt }));
    startCountdown(expiresAt);

    const amountToSend = appliedCoupon ? appliedCoupon.newAmount : originalAmount;

    const payload = { 
      amount: amountToSend,
      originalAmount: originalAmount,
      selectedItems: selectedProductIds,
      method: isCard ? 'card' : 'upi',
      couponCode: appliedCoupon ? appliedCoupon.code : null
    };

    ajaxRequest = $.ajax({
      url: 'http://localhost:8080/Ecommerce_Website/PaymentServlet',
      type: 'POST', 
      contentType:'application/json',
      data: JSON.stringify(payload),
      xhrFields: { withCredentials: true },
      success:function(resp){
        $('#loaderBox').addClass('hidden'); clearInterval(countdownInterval);
        localStorage.removeItem(PAYMENT_KEY);
        if(resp && resp.status==='ok'){
          Swal.fire({ icon:'success', title:'Payment Successful', text:'Order ID: '+resp.orderId, timer:2000, showConfirmButton:false })
            .then(()=>{ localStorage.removeItem('checkoutItems'); window.location.href='../Html/orderhistory.html'; });
        } 
        else {
          Swal.fire({ icon:'error', title:'Payment failed', text:(resp&&resp.message)?resp.message:'Server rejected payment' });
        }
      },
      error:function(xhr){
         $('#loaderBox').addClass('hidden');
         clearInterval(countdownInterval);
         localStorage.removeItem(PAYMENT_KEY);
         let msg = 'Server error';
         try { if (xhr.responseJSON && xhr.responseJSON.message) msg = xhr.responseJSON.message; } catch(e){}
         Swal.fire({ icon:'error', title:'Error', text: msg });
      },
      complete:function(){ $('#payBtn').prop('disabled',false); }
    });
  });

  updateCartBadge();
  refreshWishlistBadge();
  togglePayBtn();

  setInterval(function(){
    updateCartBadge();
    refreshWishlistBadge();
  },15000);

});

