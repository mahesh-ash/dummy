export const escapeHtml = (text) => {
  if (!text) return '';
  return String(text).replace(/[&<>"'`]/g, (match) => {
    const escape = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;',
      '`': '&#96;',
    };
    return escape[match];
  });
};

export const resolveImageUrl = (imagePath, productId) => {
  if (!imagePath && !productId) {
    return 'https://via.placeholder.com/400x250?text=No+Image';
  }

  try {
    if (typeof imagePath === 'string' && imagePath.includes('ImageServlet')) {
      return imagePath;
    }

    if (imagePath && /^\d+$/.test(String(imagePath).trim())) {
      return `/Ecommerce_Website/ImageServlet?imgId=${encodeURIComponent(String(imagePath).trim())}`;
    }

    if (typeof imagePath === 'string' && /^https?:\/\//i.test(imagePath.trim())) {
      return imagePath;
    }
  } catch (e) {
    console.error('Error resolving image:', e);
  }

  if (productId) {
    return `/Ecommerce_Website/ImageServlet?productId=${encodeURIComponent(productId)}`;
  }

  return 'https://via.placeholder.com/400x250?text=No+Image';
};

export const formatPrice = (price) => {
  return `₹${parseFloat(price || 0).toFixed(2)}`;
};

export const normalizeCartItem = (item) => {
  return {
    productId: item.productId ?? item.id ?? item.product_id,
    name: item.name ?? item.productName ?? item.product_name ?? '',
    price: parseFloat(item.price ?? item.priceAmount ?? item.amount ?? 0) || 0,
    qty: parseInt(item.qty ?? item.quantity ?? 1, 10) || 1,
    image: item.image ?? item.imageUrl ?? '',
    checked: !!item.checked,
  };
};

export const safeParse = (str) => {
  if (typeof str !== 'string' || !str.trim()) return null;
  try {
    return JSON.parse(str);
  } catch (e) {
    return null;
  }
};