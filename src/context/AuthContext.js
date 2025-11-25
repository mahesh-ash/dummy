import React, { createContext, useState, useContext, useEffect } from 'react';
import { authService } from '../services/authService';
import { safeParse } from '../utils/helpers';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isAdmin, setIsAdmin] = useState(false);

  useEffect(() => {
    try {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        const userData = safeParse(storedUser);
        setUser(userData);
        setIsAdmin(localStorage.getItem('isAdmin') === 'true');
      }
    } catch (error) {
      console.error('Error loading user:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  const login = async (email, password) => {
    try {
      const response = await authService.login(email, password);
      
      if (response.status === 'success') {
        const userData = response.user || { email };
        localStorage.setItem('user', JSON.stringify(userData));
        localStorage.setItem('isAdmin', 'false');
        setUser(userData);
        setIsAdmin(false);
        return response;
      } else if (response.status === 'blocked') {
        const blockedUser = response.user || { email, isBlocked: true };
        localStorage.setItem('user', JSON.stringify(blockedUser));
        return response;
      }
      
      return response;
    } catch (error) {
      console.error('Login error:', error);
      throw error;
    }
  };

  const adminLogin = async (email, password) => {
    try {
      const response = await authService.adminLogin(email, password);
      
      if (response.status === 'success') {
        const adminData = response.user || { email, role: 'admin' };
        localStorage.setItem('user', JSON.stringify(adminData));
        localStorage.setItem('isAdmin', 'true');
        setUser(adminData);
        setIsAdmin(true);
        return response;
      }
      
      return response;
    } catch (error) {
      console.error('Admin login error:', error);
      throw error;
    }
  };

  const register = async (userData) => {
    try {
      const response = await authService.register(userData);
      return response;
    } catch (error) {
      console.error('Register error:', error);
      throw error;
    }
  };

  const logout = async () => {
    try {
      await authService.logout();
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      localStorage.clear();
      setUser(null);
      setIsAdmin(false);
    }
  };

  return (
    <AuthContext.Provider value={{ user, isAdmin, loading, login, adminLogin, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};