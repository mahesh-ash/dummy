import { useState, useEffect, useCallback } from 'react';
import { cartService } from '../services/cartService';
import { normalizeCartItem } from '../utils/helpers';

export const useCart = () => {
  const [cartItems, setCartItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchCart = useCallback(async () => {
    setLoading(true);
    setError(null);
    
    try {
      const data = await cartService.getCart();
      const items = Array.isArray(data) ? data.map(normalizeCartItem) : [];
      setCartItems(items);
      return items;
    } catch (err) {
      setError(err.message || 'Failed to load cart');
      console.error('Cart fetch error:', err);
      return [];
    } finally {
      setLoading(false);
    }
  }, []);

  const addToCart = async (productId, qty = 1) => {
    try {
      const response = await cartService.addToCart(productId, qty);
      
      if (response.status === 'ok') {
        await fetchCart();
        return { success: true, data: response };
      }
      
      return { success: false, message: response.message || 'Failed to add to cart' };
    } catch (err) {
      console.error('Add to cart error:', err);
      return { success: false, message: err.message || 'Failed to add to cart' };
    }
  };

  const updateQuantity = async (productId, qty) => {
    try {
      const response = await cartService.updateQuantity(productId, qty);
      
      if (response.status === 'ok') {
        await fetchCart();
        return { success: true, data: response };
      }
      
      return { success: false, message: response.message || 'Failed to update quantity' };
    } catch (err) {
      console.error('Update quantity error:', err);
      return { success: false, message: err.message || 'Failed to update quantity' };
    }
  };

  const removeFromCart = async (productId) => {
    try {
      const response = await cartService.removeFromCart(productId);
      await fetchCart();
      return { success: true, data: response };
    } catch (err) {
      console.error('Remove from cart error:', err);
      return { success: false, message: err.message || 'Failed to remove from cart' };
    }
  };

  const clearCart = async () => {
    try {
      const response = await cartService.clearCart();
      await fetchCart();
      return { success: true, data: response };
    } catch (err) {
      console.error('Clear cart error:', err);
      return { success: false, message: err.message || 'Failed to clear cart' };
    }
  };

  const getCartCount = useCallback(() => {
    return cartItems.reduce((total, item) => total + (item.qty || 0), 0);
  }, [cartItems]);

  const getCartTotal = useCallback(() => {
    return cartItems.reduce((total, item) => total + (item.price * item.qty), 0);
  }, [cartItems]);

  useEffect(() => {
    const user = localStorage.getItem('user');
    if (user) {
      fetchCart();
    }
  }, [fetchCart]);

  return {
    cartItems,
    loading,
    error,
    fetchCart,
    addToCart,
    updateQuantity,
    removeFromCart,
    clearCart,
    getCartCount,
    getCartTotal,
  };
};