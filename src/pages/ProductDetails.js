import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { productService } from '../services/productService';
import { cartService } from '../services/cartService';
import { wishlistService } from '../services/wishlistService';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../hooks/useCart';
import { resolveImageUrl } from '../utils/helpers';
import { showToast, showError } from '../utils/toast';

const ProductDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { fetchCart } = useCart();

  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [images, setImages] = useState([]);
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const [quantity, setQuantity] = useState(1);
  const [isInWishlist, setIsInWishlist] = useState(false);

  useEffect(() => {
    loadProduct();
    checkWishlistStatus();
  }, [id]);

  const loadProduct = async () => {
    setLoading(true);
    try {
      const data = await productService.getProductById(id);
      setProduct(data);

      // Handle images
      let productImages = [];
      if (Array.isArray(data.images) && data.images.length) {
        productImages = data.images.map(img => resolveImageUrl(img));
      } else if (data.imageUrl || data.image) {
        productImages = [resolveImageUrl(data.imageUrl || data.image)];
      } else {
        productImages = ['https://via.placeholder.com/800x500?text=No+Image'];
      }
      setImages(productImages);
    } catch (error) {
      showError('Failed to load product');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const checkWishlistStatus = async () => {
    if (!user) return;
    try {
      const userId = user.userId || user.id || user.user_id;
      const wishlist = await wishlistService.getWishlist(userId);
      const isInList = wishlist.some(item => 
        (item.productId || item.product_id) === parseInt(id)
      );
      setIsInWishlist(isInList);
    } catch (error) {
      console.error('Failed to check wishlist status:', error);
    }
  };

  const handleAddToCart = async () => {
    if (!user) {
      showError('Please login to add items to cart');
      navigate('/login');
      return;
    }

    try {
      const response = await cartService.addToCart(id, quantity);
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

  const handleBuyNow = async () => {
    if (!user) {
      showError('Please login to buy products');
      navigate('/login');
      return;
    }

    try {
      await cartService.addToCart(id, quantity);
      localStorage.setItem('buyNowItem', JSON.stringify({
        productId: id,
        name: product.productName || product.name,
        price: product.discountedPrice || product.price,
        qty: quantity
      }));
      navigate('/cart');
    } catch (error) {
      showError('Failed to process');
    }
  };

  const handleWishlistToggle = async () => {
    if (!user) {
      showError('Please login to use wishlist');
      navigate('/login');
      return;
    }

    const userId = user.userId || user.id || user.user_id;

    try {
      if (isInWishlist) {
        await wishlistService.removeFromWishlist(id, userId);
        setIsInWishlist(false);
        showToast('Removed from wishlist');
      } else {
        await wishlistService.addToWishlist(id, userId);
        setIsInWishlist(true);
        showToast('Added to wishlist');
      }
    } catch (error) {
      showError('Failed to update wishlist');
    }
  };

  const handlePrevImage = () => {
    setCurrentImageIndex(prev => (prev === 0 ? images.length - 1 : prev - 1));
  };

  const handleNextImage = () => {
    setCurrentImageIndex(prev => (prev === images.length - 1 ? 0 : prev + 1));
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center mt-16">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading product...</p>
        </div>
      </div>
    );
  }

  if (!product) {
    return (
      <div className="min-h-screen flex items-center justify-center mt-16">
        <div className="text-center">
          <h2 className="text-2xl font-bold text-gray-800">Product not found</h2>
          <button
            onClick={() => navigate('/')}
            className="mt-4 px-6 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700"
          >
            Back to Home
          </button>
        </div>
      </div>
    );
  }

  const price = parseFloat(product.price || 0);
  const discount = parseFloat(product.discountPercent || 0);
  const discountedPrice = discount > 0 ? 
    Math.round((price - price * discount / 100) * 100) / 100 : null;
  const stock = product.stock != null ? product.stock : 0;
  const isOutOfStock = stock <= 0;

  return (
    <div className="min-h-screen bg-gray-50 pt-20 pb-12">
      <div className="max-w-7xl mx-auto px-4">
        <div className="bg-white rounded-lg shadow-md p-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            {/* Image Gallery */}
            <div>
              <div className="relative">
                <img
                  src={images[currentImageIndex]}
                  alt={product.productName || product.name}
                  className="w-full h-96 object-cover rounded-lg"
                  onError={(e) => {
                    e.target.src = 'https://via.placeholder.com/800x500?text=No+Image';
                  }}
                />
                
                {/* Wishlist Button */}
                <button
                  onClick={handleWishlistToggle}
                  className={`absolute top-4 right-4 p-3 rounded-full shadow-lg ${
                    isInWishlist ? 'bg-red-50' : 'bg-white'
                  } hover:scale-110 transition-transform`}
                >
                  <svg
                    className="w-6 h-6"
                    fill={isInWishlist ? '#ef4444' : 'none'}
                    stroke="#ef4444"
                    strokeWidth="2"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
                    />
                  </svg>
                </button>

                {/* Navigation Arrows */}
                {images.length > 1 && (
                  <>
                    <button
                      onClick={handlePrevImage}
                      className="absolute left-2 top-1/2 -translate-y-1/2 bg-white/80 p-2 rounded-full hover:bg-white"
                    >
                      ←
                    </button>
                    <button
                      onClick={handleNextImage}
                      className="absolute right-2 top-1/2 -translate-y-1/2 bg-white/80 p-2 rounded-full hover:bg-white"
                    >
                      →
                    </button>
                  </>
                )}
              </div>

              {/* Thumbnails */}
              {images.length > 1 && (
                <div className="flex gap-2 mt-4 overflow-x-auto">
                  {images.map((img, idx) => (
                    <img
                      key={idx}
                      src={img}
                      alt={`Thumbnail ${idx + 1}`}
                      className={`w-20 h-20 object-cover rounded cursor-pointer border-2 ${
                        idx === currentImageIndex ? 'border-green-600' : 'border-gray-200'
                      }`}
                      onClick={() => setCurrentImageIndex(idx)}
                    />
                  ))}
                </div>
              )}
            </div>

            {/* Product Info */}
            <div>
              <h1 className="text-3xl font-bold text-gray-800 mb-4">
                {product.productName || product.name}
              </h1>

              <div className="flex items-center mb-4">
                {[...Array(5)].map((_, i) => (
                  <svg
                    key={i}
                    className={`w-5 h-5 ${
                      i < (product.stars || 4) ? 'text-yellow-400' : 'text-gray-300'
                    }`}
                    fill="currentColor"
                    viewBox="0 0 20 20"
                  >
                    <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.286 3.966a1 1 0 00.95.69h4.175c.969 0 1.371 1.24.588 1.81l-3.38 2.455a1 1 0 00-.364 1.118l1.287 3.966c.3.921-.755 1.688-1.54 1.118l-3.38-2.455a1 1 0 00-1.175 0l-3.38 2.455c-.784.57-1.838-.197-1.539-1.118l1.287-3.966a1 1 0 00-.364-1.118L2.174 9.393c-.783-.57-.38-1.81.588-1.81h4.175a1 1 0 00.95-.69l1.286-3.966z" />
                  </svg>
                ))}
                <span className="ml-2 text-gray-600">
                  {product.stars || 4} / 5
                </span>
              </div>

              {/* Price */}
              <div className="mb-4">
                {discount > 0 ? (
                  <div>
                    <span className="line-through text-gray-500 text-xl mr-3">
                      ₹{price.toFixed(2)}
                    </span>
                    <span className="text-3xl font-bold text-green-600">
                      ₹{discountedPrice.toFixed(2)}
                    </span>
                    <span className="ml-3 px-2 py-1 bg-red-100 text-red-600 rounded text-sm font-semibold">
                      {discount}% OFF
                    </span>
                  </div>
                ) : (
                  <span className="text-3xl font-bold text-green-600">
                    ₹{price.toFixed(2)}
                  </span>
                )}
              </div>

              {/* Stock */}
              <p className={`text-lg mb-6 ${
                isOutOfStock ? 'text-red-600 font-semibold' : 'text-green-600'
              }`}>
                {isOutOfStock ? 'Out of stock' : `In stock: ${stock}`}
              </p>

              {/* Description */}
              {product.description && (
                <div className="mb-6">
                  <h3 className="text-lg font-semibold mb-2">Description</h3>
                  <p className="text-gray-700">{product.description}</p>
                </div>
              )}

              {/* Quantity Selector */}
              {!isOutOfStock && (
                <div className="flex items-center gap-4 mb-6">
                  <label className="font-medium">Quantity:</label>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => setQuantity(Math.max(1, quantity - 1))}
                      className="w-10 h-10 rounded-full bg-gray-200 hover:bg-gray-300 flex items-center justify-center"
                    >
                      -
                    </button>
                    <input
                      type="number"
                      value={quantity}
                      onChange={(e) => setQuantity(Math.max(1, Math.min(stock, parseInt(e.target.value) || 1)))}
                      className="w-20 text-center border rounded py-2"
                      min="1"
                      max={stock}
                    />
                    <button
                      onClick={() => setQuantity(Math.min(stock, quantity + 1))}
                      className="w-10 h-10 rounded-full bg-gray-200 hover:bg-gray-300 flex items-center justify-center"
                    >
                      +
                    </button>
                  </div>
                </div>
              )}

              {/* Action Buttons */}
              {!isOutOfStock && (
                <div className="flex gap-4">
                  <button
                    onClick={handleAddToCart}
                    className="flex-1 bg-green-600 text-white py-3 rounded-lg hover:bg-green-700 font-semibold"
                  >
                    Add to Cart
                  </button>
                  <button
                    onClick={handleBuyNow}
                    className="flex-1 bg-yellow-500 text-white py-3 rounded-lg hover:bg-yellow-600 font-semibold"
                  >
                    Buy Now
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProductDetails;