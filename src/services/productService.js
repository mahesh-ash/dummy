import api from '../utils/api';

export const productService = {
  getAllProducts: async (params = {}) => {
    try {
      const response = await api.get('/ProductServlet', { params });
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  getProductById: async (productId) => {
    try {
      const response = await api.get('/ProductServlet', {
        params: { productId }
      });
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  searchProducts: async (query, categoryId, filter) => {
    try {
      const params = {};
      if (categoryId) params.category_id = categoryId;
      if (query) params.query = query;
      if (filter) params.filter = filter;
      
      const response = await api.get('/ProductServlet', { params });
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },
};