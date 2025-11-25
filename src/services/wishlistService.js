import api from '../utils/api';

export const wishlistService = {
  getWishlist: async (userId) => {
    try {
      const response = await api.get('/WishlistServlet', {
        params: { userId }
      });
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  addToWishlist: async (productId, userId) => {
    try {
      const response = await api.post('/WishlistServlet',
        new URLSearchParams({ action: 'add', productId, userId }),
        {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        }
      );
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  removeFromWishlist: async (productId, userId) => {
    try {
      const response = await api.post('/WishlistServlet',
        new URLSearchParams({ action: 'remove', productId, userId }),
        {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        }
      );
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  clearWishlist: async (userId) => {
    try {
      const response = await api.post('/WishlistServlet',
        new URLSearchParams({ action: 'clearAll', userId }),
        {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        }
      );
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  getWishlistCount: async (userId) => {
    try {
      const response = await api.get('/WishlistServlet', {
        params: { count: 1, userId }
      });
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },
};