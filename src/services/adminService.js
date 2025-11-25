import api from '../utils/api';

export const adminService = {
  // Admin Authentication
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

  getAdmin: async () => {
    try {
      const response = await api.get('/AdminServlet', {
        params: { action: 'getAdmin' }
      });
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  logout: async () => {
    try {
      const response = await api.post('/AdminServlet',
        new URLSearchParams({ action: 'logout' }),
        {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        }
      );
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  // Products
  listProducts: async () => {
    try {
      const response = await api.post('/AdminServlet',
        new URLSearchParams({ action: 'listProducts' }),
        {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        }
      );
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  addProduct: async (formData) => {
    try {
      formData.append('action', 'addProduct');
      const response = await api.post('/AdminServlet', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  updateProduct: async (formData) => {
    try {
      formData.append('action', 'updateProduct');
      const response = await api.post('/AdminServlet', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  deleteProduct: async (productId) => {
    try {
      const response = await api.post('/AdminServlet',
        new URLSearchParams({ action: 'deleteProduct', productId }),
        {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        }
      );
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  // Users
  listUsers: async () => {
    try {
      const response = await api.get('/AdminServlet', {
        params: { action: 'listUsers' }
      });
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  deactivateUser: async (userId, adminMessage) => {
    try {
      const response = await api.post('/AdminServlet',
        new URLSearchParams({ action: 'deactivateUser', userId, adminMessage }),
        {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        }
      );
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  toggleUserStatus: async (userId, active) => {
    try {
      const response = await api.post('/AdminServlet',
        new URLSearchParams({ action: 'toggleStatus', userId, active }),
        {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        }
      );
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  // Orders
  listOrders: async () => {
    try {
      const response = await api.get('/AdminServlet', {
        params: { action: 'listOrders' }
      });
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  // Categories
  listCategories: async () => {
    try {
      const response = await api.get('/AdminServlet', {
        params: { action: 'listCategories' }
      });
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  addCategory: async (categoryData) => {
    try {
      const params = new URLSearchParams({
        action: 'addCategory',
        ...categoryData
      });
      const response = await api.post('/AdminServlet', params, {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
      });
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  updateCategory: async (categoryData) => {
    try {
      const params = new URLSearchParams({
        action: 'updateCategory',
        ...categoryData
      });
      const response = await api.post('/AdminServlet', params, {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
      });
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  // Discounts
  listDiscounts: async () => {
    try {
      const response = await api.get('/AdminServlet', {
        params: { action: 'listDiscounts' }
      });
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  addDiscount: async (discountData) => {
    try {
      const params = new URLSearchParams({
        action: 'addDiscount',
        ...discountData
      });
      const response = await api.post('/AdminServlet', params, {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
      });
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  updateDiscount: async (discountData) => {
    try {
      const params = new URLSearchParams({
        action: 'updateDiscount',
        ...discountData
      });
      const response = await api.post('/AdminServlet', params, {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
      });
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  deleteDiscount: async (discountId) => {
    try {
      const response = await api.post('/AdminServlet',
        new URLSearchParams({ action: 'deleteDiscount', discountId }),
        {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        }
      );
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  checkExistingDiscount: async (productId) => {
    try {
      const response = await api.get('/AdminServlet', {
        params: { action: 'checkExistingDiscount', productId }
      });
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  // Unblock Requests
  listPendingUnblockRequests: async () => {
    try {
      const response = await api.get('/UnblockManagementServlet', {
        params: { action: 'listPendingRequests' }
      });
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  approveUnblockRequest: async (requestId, adminNotes) => {
    try {
      const response = await api.post('/UnblockManagementServlet',
        new URLSearchParams({ action: 'approveRequest', requestId, adminNotes }),
        {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        }
      );
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  rejectUnblockRequest: async (requestId, adminNotes) => {
    try {
      const response = await api.post('/UnblockManagementServlet',
        new URLSearchParams({ action: 'rejectRequest', requestId, adminNotes }),
        {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        }
      );
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  },

  changePassword: async (newPassword) => {
    try {
      const response = await api.post('/AdminServlet',
        new URLSearchParams({ action: 'changePassword', newPassword }),
        {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        }
      );
      return response.data;
    } catch (error) {
      throw error.response?.data || error;
    }
  }
};

export default adminService;