import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { productService } from '../services/productService';
import { cartService } from '../services/cartService';
import { wishlistService } from '../services/wishlistService';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../hooks/useCart';
import { resolveImageUrl, escapeHtml } from '../utils/helpers';
import { showToast, showError } from '../utils/toast';

const Home = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { fetchCart, getCartCount } = useCart();
  
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedCategory, setSelectedCategory] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [sortFilter, setSortFilter] = useState(null);
  const [wishlistIds, setWishlistIds] = useState(new Set());
  const [showFilterMenu, setShowFilterMenu] = useState(false);

  // Fetch categories
  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const response = await productService.getAllProducts();
        // Extract unique categories from products
        const uniqueCategories = [...new Set(response.map(p => p.categoryId || p.category_id))];
        setCategories(uniqueCategories.map(id => ({ id, name: `Category ${id}` })));
      } catch (error) {
        console.error('Failed to fetch categories:', error);
      }
    };
    fetchCategories();
  }, []);

  // Fetch wishlist
  useEffect(() => {
    const fetchWishlist = async () => {
      if (!user) return;
      try {
        const userId = user.userId || user.id || user.user_id;
        const wishlistItems = await wishlistService.getWishlist(userId);
        const ids = new Set(wishlistItems.map(item => item.productId || item.product_id));
        setWishlistIds(ids);
      } catch (error) {
        console.error('Failed to fetch wishlist:', error);
      }
    };
    fetchWishlist();
  }, [user]);

  // Fetch products
  const fetchProducts = useCallback(async () => {
    setLoading(true);
    try {
      const params = {};
      if (selectedCategory) params.category_id = selectedCategory;
      if (searchQuery) params.query = searchQuery;
      if (sortFilter) params.filter = sortFilter;

      const data = await productService.getAllProducts(params);
      setProducts(Array.isArray(data) ? data : []);
    } catch (error) {
      showError('Failed to load products');
      console.error(error);
    } finally {
      setLoading(false);
    }
  }, [selectedCategory, searchQuery, sortFilter]);

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  // Handle add to cart
  const handleAddToCart = async (productId, e) => {
    e.stopPropagation();
    if (!user) {
      showError('Please login to add items to cart');
      navigate('/login');
      return;
    }

    try {
      const response = await cartService.addToCart(productId, 1);
      if (response.status === 'ok') {
        showToast('Added to cart');
        fetchCart();
      } else {
        showError(response.message || 'Failed to add to cart');
      }
    } catch (error) {
      showError('Failed to add to cart');
    }
  };

  // Handle buy now
  const handleBuyNow = async (product, e) => {
    e.stopPropagation();
    if (!user) {
      showError('Please login to buy products');
      navigate('/login');
      return;
    }

    try {
      await cartService.addToCart(product.productId || product.id, 1);
      localStorage.setItem('buyNowItem', JSON.stringify({
        productId: product.productId || product.id,
        name: product.productName || product.name,
        price: product.discountedPrice || product.price,
        qty: 1
      }));
      navigate('/cart');
    } catch (error) {
      showError('Failed to process');
    }
  };

  // Handle wishlist toggle
  const handleWishlistToggle = async (productId, e) => {
    e.stopPropagation();
    if (!user) {
      showError('Please login to use wishlist');
      navigate('/login');
      return;
    }

    const userId = user.userId || user.id || user.user_id;
    const isInWishlist = wishlistIds.has(productId);

    try {
      if (isInWishlist) {
        await wishlistService.removeFromWishlist(productId, userId);
        setWishlistIds(prev => {
          const newSet = new Set(prev);
          newSet.delete(productId);
          return newSet;
        });
        showToast('Removed from wishlist');
      } else {
        await wishlistService.addToWishlist(productId, userId);
        setWishlistIds(prev => new Set([...prev, productId]));
        showToast('Added to wishlist');
      }
    } catch (error) {
      showError('Failed to update wishlist');
    }
  };

  // Render product card
  const renderProductCard = (product) => {
    const id = product.productId || product.id || product.product_id;
    const name = product.productName || product.name || product.product_name || 'Unnamed';
    const price = parseFloat(product.price || 0);
    const discount = parseFloat(product.discountPercent || 0);
    const discountedPrice = discount > 0 ? Math.round((price - price * discount / 100) * 100) / 100 : null;
    const stock = product.stock != null ? product.stock : 0;
    const isOutOfStock = stock <= 0;
    const isInWishlist = wishlistIds.has(id);

    let imageUrl = 'https://via.placeholder.com/400x250?text=No+Image';
    if (Array.isArray(product.images) && product.images.length) {
      imageUrl = resolveImageUrl(product.images[0]);
    } else if (product.imageUrl || product.image) {
      imageUrl = resolveImageUrl(product.imageUrl || product.image);
    }

    return (
      <div
        key={id}
        className="bg-white p-4 rounded-lg shadow-md text-center cursor-pointer hover:shadow-lg transition-shadow relative"
        onClick={() => navigate(`/product/${id}`)}
      >
        {/* Wishlist Button */}
        <button
          className={`absolute top-2 right-2 p-2 rounded-full z-10 ${
            isInWishlist ? 'bg-red-50' : 'bg-white'
          } shadow-md hover:scale-110 transition-transform`}
          onClick={(e) => handleWishlistToggle(id, e)}
          aria-label={isInWishlist ? 'Remove from wishlist' : 'Add to wishlist'}
        >
          <svg
            className="w-5 h-5"
            fill={isInWishlist ? '#ef4444' : 'none'}
            stroke="#ef4444"
            strokeWidth="2"
            viewBox="0 0 24 24"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
            />
          </svg>
        </button>

        <img
          src={imageUrl}
          alt={name}
          className="w-full h-48 object-cover rounded mb-3"
          onError={(e) => {
            e.target.src = 'https://via.placeholder.com/400x250?text=No+Image';
          }}
        />

        <h4 className="font-semibold text-gray-800 truncate">{name}</h4>

        <div className="flex justify-center items-center my-2">
          {[...Array(5)].map((_, i) => (
            <svg
              key={i}
              className={`w-4 h-4 ${i < (product.stars || 4) ? 'text-yellow-400' : 'text-gray-300'}`}
              fill="currentColor"
              viewBox="0 0 20 20"
            >
              <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.286 3.966a1 1 0 00.95.69h4.175c.969 0 1.371 1.24.588 1.81l-3.38 2.455a1 1 0 00-.364 1.118l1.287 3.966c.3.921-.755 1.688-1.54 1.118l-3.38-2.455a1 1 0 00-1.175 0l-3.38 2.455c-.784.57-1.838-.197-1.539-1.118l1.287-3.966a1 1 0 00-.364-1.118L2.174 9.393c-.783-.57-.38-1.81.588-1.81h4.175a1 1 0 00.95-.69l1.286-3.966z" />
            </svg>
          ))}
          <span className="ml-2 text-sm font-semibold text-yellow-600">{product.stars || 4}</span>
        </div>

        {discount > 0 ? (
          <div className="mb-2">
            <span className="line-through text-gray-500 mr-2">₹{price.toFixed(2)}</span>
            <span className="font-bold text-green-600">₹{discountedPrice.toFixed(2)}</span>
            <span className="ml-2 text-sm text-red-600 font-semibold">{discount}% OFF</span>
          </div>
        ) : (
          <div className="font-bold text-green-600 mb-2">₹{price.toFixed(2)}</div>
        )}

        <p className={`text-sm ${isOutOfStock ? 'text-red-600 font-semibold' : 'text-gray-600'}`}>
          {isOutOfStock ? 'Out of stock' : `In stock: ${stock}`}
        </p>

        {!isOutOfStock && (
          <div className="mt-3 flex gap-2">
            <button
              className="flex-1 bg-green-600 text-white px-3 py-2 rounded-md hover:bg-green-700"
              onClick={(e) => handleAddToCart(id, e)}
            >
              Add to Cart
            </button>
            <button
              className="flex-1 bg-yellow-500 text-white px-3 py-2 rounded-md hover:bg-yellow-600"
              onClick={(e) => handleBuyNow(product, e)}
            >
              Buy Now
            </button>
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="min-h-screen bg-green-50">
      {/* Hero Section */}
      <div className="bg-gradient-to-r from-green-600 to-green-700 text-white py-12 mt-16">
        <div className="max-w-7xl mx-auto px-4 text-center">
          <h1 className="text-4xl font-bold mb-2">Welcome to Shopping Center</h1>
          <p className="text-lg">We specialize in Electronics</p>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 py-8">
        {/* Filters */}
        <div className="mb-6 flex flex-wrap gap-4 items-center">
          {/* Category Filter */}
          <select
            className="px-4 py-2 border rounded-md bg-white"
            value={selectedCategory || ''}
            onChange={(e) => setSelectedCategory(e.target.value || null)}
          >
            <option value="">All Categories</option>
            {categories.map((cat) => (
              <option key={cat.id} value={cat.id}>
                {cat.name}
              </option>
            ))}
          </select>

          {/* Search */}
          <div className="flex-1 max-w-md">
            <input
              type="text"
              placeholder="Search products..."
              className="w-full px-4 py-2 border rounded-md"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          {/* Sort Filter */}
          <div className="relative">
            <button
              className="px-4 py-2 bg-yellow-500 text-white rounded-md hover:bg-yellow-600"
              onClick={() => setShowFilterMenu(!showFilterMenu)}
            >
              Sort By
            </button>
            {showFilterMenu && (
              <div className="absolute right-0 mt-2 w-48 bg-white rounded-md shadow-lg z-10">
                <button
                  className="block w-full text-left px-4 py-2 hover:bg-gray-100"
                  onClick={() => {
                    setSortFilter('low-high');
                    setShowFilterMenu(false);
                  }}
                >
                  Price: Low → High
                </button>
                <button
                  className="block w-full text-left px-4 py-2 hover:bg-gray-100"
                  onClick={() => {
                    setSortFilter('high-low');
                    setShowFilterMenu(false);
                  }}
                >
                  Price: High → Low
                </button>
                <button
                  className="block w-full text-left px-4 py-2 hover:bg-gray-100"
                  onClick={() => {
                    setSortFilter('rating');
                    setShowFilterMenu(false);
                  }}
                >
                  Top Rated
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Products Grid */}
        {loading ? (
          <div className="text-center py-12">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600 mx-auto"></div>
            <p className="mt-4 text-gray-600">Loading products...</p>
          </div>
        ) : products.length === 0 ? (
          <p className="text-center text-gray-500 py-12">No products found.</p>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
            {products.map(renderProductCard)}
          </div>
        )}
      </div>
    </div>
  );
};

export default Home;