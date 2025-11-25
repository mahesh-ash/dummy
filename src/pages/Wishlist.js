import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { wishlistService } from '../services/wishlistService';
import { cartService } from '../services/cartService';
import { useCart } from '../hooks/useCart';
import { resolveImageUrl } from '../utils/helpers';
import { showToast, showError, showConfirm } from '../utils/toast';

const Wishlist = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { fetchCart } = useCart();
  const [wishlistItems, setWishlistItems] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (user) {
      loadWishlist();
    }
  }, [user]);

  const loadWishlist = async () => {
    setLoading(true);
    try {
      const userId = user.userId || user.id || user.user_id;
      const items = await wishlistService.getWishlist(userId);
      setWishlistItems(Array.isArray(items) ? items : []);
    } catch (error) {
      showError('Failed to load wishlist');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const handleRemove = async (productId) => {
    showConfirm('Remove this item from wishlist?', async () => {
      try {
        const userId = user.userId || user.id || user.user_id;
        await wishlistService.removeFromWishlist(productId, userId);
        setWishlistItems(prev => prev.filter(item => 
          (item.productId || item.product_id) !== productId
        ));
        showToast('Removed from wishlist');
      } catch (error) {
        showError('Failed to remove item');
      }
    });
  };

  const handleMoveToCart = async (productId) => {
    try {
      const userId = user.userId || user.id || user.user_id;
      
      // Add to cart
      const response = await cartService.addToCart(productId, 1);
      
      if (response.status === 'ok') {
        // Remove from wishlist
        await wishlistService.removeFromWishlist(productId, userId);
        
        // Update UI
        setWishlistItems(prev => prev.filter(item => 
          (item.productId || item.product_id) !== productId
        ));
        
        fetchCart();
        showToast('Moved to cart');
      } else {
        showError(response.message || 'Failed to add to cart');
      }
    } catch (error) {
      showError('Failed to move to cart');
      console.error(error);
    }
  };

  const handleBuyNow = async (product) => {
    try {
      const productId = product.productId || product.product_id;
      await cartService.addToCart(productId, 1);
      
      // Remove from wishlist
      const userId = user.userId || user.id || user.user_id;
      await wishlistService.removeFromWishlist(productId, userId);
      
      navigate('/cart');
    } catch (error) {
      showError('Failed to process');
    }
  };

  const handleClearAll = () => {
    showConfirm('Clear all items from your wishlist?', async () => {
      try {
        const userId = user.userId || user.id || user.user_id;
        await wishlistService.clearWishlist(userId);
        setWishlistItems([]);
        showToast('Wishlist cleared');
      } catch (error) {
        showError('Failed to clear wishlist');
      }
    });
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center mt-16">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading wishlist...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 pt-20 pb-12">
      <div className="max-w-7xl mx-auto px-4">
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-3xl font-bold text-gray-800">My Wishlist</h1>
          <div className="flex gap-2">
            <button
              onClick={loadWishlist}
              className="px-4 py-2 bg-gray-100 hover:bg-gray-200 rounded-lg transition"
            >
              Refresh
            </button>
            {wishlistItems.length > 0 && (
              <button
                onClick={handleClearAll}
                className="px-4 py-2 bg-red-100 text-red-700 hover:bg-red-200 rounded-lg transition"
              >
                Clear All
              </button>
            )}
          </div>
        </div>

        {/* Content */}
        {wishlistItems.length === 0 ? (
          <div className="text-center py-12">
            <svg
              className="mx-auto h-24 w-24 text-gray-400"
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
            <h3 className="mt-4 text-xl font-medium text-gray-900">Your wishlist is empty</h3>
            <p className="mt-2 text-gray-500">Start adding items you love!</p>
            <button
              onClick={() => navigate('/')}
              className="mt-6 px-6 py-3 bg-green-600 text-white rounded-lg hover:bg-green-700 transition"
            >
              Browse Products
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {wishlistItems.map((item) => {
              const productId = item.productId || item.product_id || item.id;
              const name = item.productName || item.name || item.product_name || 'Product';
              const price = parseFloat(item.price || 0);
              const discount = parseFloat(item.discountPercent || 0);
              const discountedPrice = discount > 0 ? 
                Math.round((price - price * discount / 100) * 100) / 100 : null;
              const stock = item.stock != null ? item.stock : 0;
              const isOutOfStock = stock <= 0;

              let imageUrl = 'https://via.placeholder.com/400x250?text=No+Image';
              if (Array.isArray(item.images) && item.images.length) {
                imageUrl = resolveImageUrl(item.images[0]);
              } else if (item.imageUrl || item.image) {
                imageUrl = resolveImageUrl(item.imageUrl || item.image);
              }

              return (
                <div
                  key={productId}
                  className="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-lg transition"
                >
                  {/* Image */}
                  <div
                    className="cursor-pointer"
                    onClick={() => navigate(`/product/${productId}`)}
                  >
                    <img
                      src={imageUrl}
                      alt={name}
                      className="w-full h-48 object-cover"
                      onError={(e) => {
                        e.target.src = 'https://via.placeholder.com/400x250?text=No+Image';
                      }}
                    />
                  </div>

                  {/* Content */}
                  <div className="p-4">
                    <h3
                      className="font-semibold text-lg text-gray-800 mb-2 cursor-pointer hover:text-green-600 line-clamp-2"
                      onClick={() => navigate(`/product/${productId}`)}
                    >
                      {name}
                    </h3>

                    {/* Price */}
                    <div className="mb-2">
                      {discount > 0 ? (
                        <>
                          <span className="line-through text-gray-500 mr-2">
                            ₹{price.toFixed(2)}
                          </span>
                          <span className="font-bold text-green-600">
                            ₹{discountedPrice.toFixed(2)}
                          </span>
                          <span className="ml-2 text-sm text-red-600 font-semibold">
                            {discount}% OFF
                          </span>
                        </>
                      ) : (
                        <span className="font-bold text-green-600">₹{price.toFixed(2)}</span>
                      )}
                    </div>

                    {/* Stock */}
                    <p className={`text-sm mb-3 ${isOutOfStock ? 'text-red-600 font-semibold' : 'text-gray-600'}`}>
                      {isOutOfStock ? 'Out of stock' : `In stock: ${stock}`}
                    </p>

                    {/* Actions */}
                    <div className="flex gap-2">
                      <button
                        onClick={() => handleMoveToCart(productId)}
                        disabled={isOutOfStock}
                        className="flex-1 bg-green-600 text-white px-3 py-2 rounded-lg hover:bg-green-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        Move to Cart
                      </button>
                      <button
                        onClick={() => handleBuyNow(item)}
                        disabled={isOutOfStock}
                        className="flex-1 bg-yellow-500 text-white px-3 py-2 rounded-lg hover:bg-yellow-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        Buy Now
                      </button>
                    </div>

                    {/* Remove Button */}
                    <button
                      onClick={() => handleRemove(productId)}
                      className="w-full mt-2 text-red-600 hover:text-red-700 text-sm font-medium"
                    >
                      Remove
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

export default Wishlist;