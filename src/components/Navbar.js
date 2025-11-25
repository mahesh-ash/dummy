import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../hooks/useCart';
import { wishlistService } from '../services/wishlistService';
import { showToast } from '../utils/toast';

const Navbar = () => {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const { getCartCount } = useCart();
  const [wishlistCount, setWishlistCount] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    if (user) {
      fetchWishlistCount();
    }
  }, [user]);

  const fetchWishlistCount = async () => {
    try {
      const userId = user.userId || user.id || user.user_id;
      const response = await wishlistService.getWishlistCount(userId);
      setWishlistCount(response.count || 0);
    } catch (error) {
      console.error('Failed to fetch wishlist count:', error);
    }
  };

  const handleLogout = async () => {
    try {
      await logout();
      showToast('Logged out successfully');
      navigate('/login');
    } catch (error) {
      console.error('Logout error:', error);
      showToast('Logged out');
      navigate('/login');
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/?search=${encodeURIComponent(searchQuery)}`);
    }
  };

  return (
    <nav className="bg-green-600 text-white fixed w-full top-0 z-50 shadow-md">
      <div className="max-w-7xl mx-auto px-4">
        {/* Main navbar */}
        <div className="flex items-center justify-between h-16">
          {/* Logo/Brand */}
          <Link to="/" className="flex items-center">
            <div className="text-xl font-semibold">Shopping Center</div>
          </Link>

          {/* Search Bar */}
          <div className="hidden md:flex flex-1 max-w-xl mx-8">
            <form onSubmit={handleSearch} className="w-full flex">
              <input
                type="text"
                placeholder="Search products..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="flex-1 px-4 py-2 rounded-l-md text-gray-800 focus:outline-none"
              />
              <button
                type="submit"
                className="bg-yellow-500 px-4 py-2 rounded-r-md hover:bg-yellow-600 transition"
              >
                Search
              </button>
            </form>
          </div>

          {/* Right side items */}
          <div className="flex items-center space-x-4">
            {/* Wishlist */}
            <Link
              to="/wishlist"
              className="relative hover:text-yellow-300 transition"
              aria-label="Wishlist"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-6 w-6"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
                />
              </svg>
              {wishlistCount > 0 && (
                <span className="absolute -top-2 -right-2 bg-red-500 text-white rounded-full px-2 text-xs">
                  {wishlistCount}
                </span>
              )}
            </Link>

            {/* Cart */}
            <Link
              to="/cart"
              className="relative hover:text-yellow-300 transition"
              aria-label="Cart"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-6 w-6"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-1.293 6.707A1 1 0 007.707 21h8.586a1 1 0 00.707-1.707L15 13H7z"
                />
              </svg>
              {getCartCount() > 0 && (
                <span className="absolute -top-2 -right-2 bg-red-500 text-white rounded-full px-2 text-xs">
                  {getCartCount()}
                </span>
              )}
            </Link>

            {/* Profile */}
            <Link
              to="/profile"
              className="hover:text-yellow-300 transition"
              aria-label="Profile"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-6 w-6"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
                />
              </svg>
            </Link>

            {/* Orders */}
            <Link
              to="/orders"
              className="hidden md:block px-3 py-1 hover:bg-green-700 rounded transition"
            >
              Orders
            </Link>

            {/* Logout */}
            <button
              onClick={handleLogout}
              className="px-4 py-2 bg-red-500 hover:bg-red-600 rounded transition"
            >
              Logout
            </button>
          </div>
        </div>

        {/* Mobile Search Bar */}
        <div className="md:hidden pb-3">
          <form onSubmit={handleSearch} className="w-full flex">
            <input
              type="text"
              placeholder="Search products..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="flex-1 px-4 py-2 rounded-l-md text-gray-800 focus:outline-none"
            />
            <button
              type="submit"
              className="bg-yellow-500 px-4 py-2 rounded-r-md hover:bg-yellow-600"
            >
              Search
            </button>
          </form>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;