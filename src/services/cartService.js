import api from '../utils/api';

export const cartService = {
  getCart: async () => {
    try {
      const response = await api.get('/CartServlet');
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  addToCart: async (productId, qty = 1) => {
    try {
      const response = await api.post('/CartServlet',
        new URLSearchParams({ action: 'add', productId, qty }),
        {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        }
      );
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  updateQuantity: async (productId, qty) => {
    try {
      const response = await api.post('/CartServlet',
        new URLSearchParams({ action: 'update', productId, qty }),
        {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        }
      );
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  removeFromCart: async (productId) => {
    try {
      const response = await api.post('/CartServlet',
        new URLSearchParams({ action: 'remove', productId }),
        {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        }
      );
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  clearCart: async () => {
    try {
      const response = await api.post('/CartServlet',
        new URLSearchParams({ action: 'clear' }),
        {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        }
      );
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },
};