import api from '../utils/api';

export const authService = {
  login: async (email, password) => {
    try {
      const response = await api.post('/LoginServlet', 
        new URLSearchParams({ email, password }),
        {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        }
      );
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  register: async (userData) => {
    try {
      const response = await api.post('/RegisterServlet', 
        new URLSearchParams(userData),
        {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        }
      );
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  logout: async () => {
    try {
      const response = await api.post('/LogoutServlet');
      localStorage.clear();
      return response.data;
    } catch (error) {
      localStorage.clear();
      throw error;
    }
  },

  adminLogin: async (email, password) => {
    try {
      const response = await api.post('/AdminLoginServlet',
        new URLSearchParams({ email, password }),
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