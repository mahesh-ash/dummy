$(function(){

    function toast(msg, ms = 1400){
      $('#toast').stop(true, true).text(msg).fadeIn(200).delay(ms).fadeOut(400);
    }


    function checkAdminSessionAndRedirect(){
      $.ajax({
        url: '/Ecommerce_Website/AdminServlet',
        method: 'GET',
        data: { action: 'getAdmin' },
        dataType: 'json'
      }).done(function(adminUser){
        if (adminUser && (adminUser.email || adminUser.name)) {
         
          if (adminUser.name) $('#adminName').text(adminUser.name);
          window.location.href = '/Ecommerce_Website/Html/Admin.html';
        } else {
    
          $('#adminName').text('Admin');
        }
      }).fail(function(){
   
      });
    }

    checkAdminSessionAndRedirect();

    
    $('#adminLoginForm').on('submit', function(e){
      e.preventDefault();
      $('#msg').hide();
      const data = {
        email: $('#email').val().trim(),
        password: $('#password').val().trim()
      };

      $.ajax({
        url: '/Ecommerce_Website/AdminLoginServlet',
        method: 'POST',
        data: data,
        dataType: 'json'
      }).done(function(res){
        if (res && (res.status === 'success' || res.status === 'ok')) {
          const redirect = res.redirectUrl ? res.redirectUrl : '/Ecommerce_Website/Html/Admin.html';
          toast('Login successful', 800);
          
          setTimeout(function(){ window.location.href = redirect; }, 600);
        } else {
          $('#msg').text((res && res.message) ? res.message : 'Invalid admin credentials').show();
        }
      }).fail(function(){
        $('#msg').text('Server error — try again').show();
      });
    });

    $('#profileBtn').click(function(){
      
      window.location.href = '/Ecommerce_Website/Html/adminprofile.html';
    });

    
    $('#logoutBtn').click(function(){
      if (!confirm('Logout?')) return;
      $.post('/Ecommerce_Website/AdminServlet', { action: 'logout' })
        .always(function(){ window.location.href = '/Ecommerce_Website/Html/admin_login.html'; 
          
        });
    });

    
    $('#menuToggle').click(function(){});
          
  });