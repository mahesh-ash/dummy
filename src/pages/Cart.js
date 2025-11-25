import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../hooks/useCart';
import { resolveImageUrl } from '../utils/helpers';
import { showToast, showError } from '../utils/toast';

const Cart = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { cartItems, updateQuantity, removeFromCart, fetchCart, getCartTotal } = useCart();
  
  const [localCartItems, setLocalCartItems] = useState([]);
  const [showAddressModal, setShowAddressModal] = useState(false);
  const [addressForm, setAddressForm] = useState({
    fullname: '',
    phone: '',
    address: '',
    pincode: ''
  });

  useEffect(() => {
    if (user) {
      const userData = user;
      setAddressForm({
        fullname: userData.name || '',
        phone: userData.mobile || '',
        address: userData.address || '',
        pincode: userData.pinCode || ''
      });
    }
  }, [user]);

  useEffect(() => {
    setLocalCartItems(cartItems.map(item => ({ ...item, checked: true })));
  }, [cartItems]);

  const handleCheckItem = (productId) => {
    setLocalCartItems(prev =>
      prev.map(item =>
        (item.productId === productId)
          ? { ...item, checked: !item.checked }
          : item
      )
    );
  };

  const handleSelectAll = (checked) => {
    setLocalCartItems(prev => prev.map(item => ({ ...item, checked })));
  };

  const handleIncrement = async (productId) => {
    const item = localCartItems.find(i => i.productId === productId);
    if (!item) return;

    const result = await updateQuantity(productId, item.qty + 1);
    if (!result.success) {
      showError(result.message || 'Failed to update quantity');
    }
  };

  const handleDecrement = async (productId) => {
    const item = localCartItems.find(i => i.productId === productId);
    if (!item || item.qty <= 1) return;

    const result = await updateQuantity(productId, item.qty - 1);
    if (!result.success) {
      showError(result.message || 'Failed to update quantity');
    }
  };

  const handleRemove = async (productId) => {
    const result = await removeFromCart(productId);
    if (result.success) {
      showToast('Item removed from cart');
    } else {
      showError('Failed to remove item');
    }
  };

  const getSelectedTotal = () => {
    return localCartItems
      .filter(item => item.checked)
      .reduce((total, item) => total + (item.price * item.qty), 0);
  };

  const handleProceed = () => {
    const selectedItems = localCartItems.filter(item => item.checked);
    if (selectedItems.length === 0) {
      showError('Please select at least one item');
      return;
    }
    setShowAddressModal(true);
  };

  const handleAddressSubmit = () => {
    const { fullname, phone, address, pincode } = addressForm;

    if (!fullname || !phone || !address || !pincode) {
      showError('All fields are required');
      return;
    }

    if (!/^[6-9]\d{9}$/.test(phone)) {
      showError('Please enter a valid 10-digit phone number');
      return;
    }

    if (!/^\d{6}$/.test(pincode)) {
      showError('Please enter a valid 6-digit pincode');
      return;
    }

    const selectedItems = localCartItems.filter(item => item.checked);
    localStorage.setItem('checkoutItems', JSON.stringify(selectedItems));
    localStorage.setItem('deliveryDetails', JSON.stringify(addressForm));
    localStorage.setItem('checkoutAmount', getSelectedTotal().toFixed(2));

    navigate(`/payment?amount=${getSelectedTotal().toFixed(2)}`);
  };

  if (!user) {
    navigate('/login');
    return null;
  }

  if (localCartItems.length === 0) {
    return (
      <div className="min-h-screen flex items-center justify-center mt-16">
        <div className="text-center">
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
              d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"
            />
          </svg>
          <h3 className="mt-4 text-xl font-medium text-gray-900">Your cart is empty</h3>
          <p className="mt-2 text-gray-500">Start shopping to add items to your cart!</p>
          <button
            onClick={() => navigate('/')}
            className="mt-6 px-6 py-3 bg-green-600 text-white rounded-lg hover:bg-green-700"
          >
            Continue Shopping
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 pt-20 pb-12">
      <div className="max-w-7xl mx-auto px-4">
        <h1 className="text-3xl font-bold mb-6">Shopping Cart</h1>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Cart Items */}
          <div className="lg:col-span-2">
            <div className="bg-white rounded-lg shadow-md overflow-hidden">
              <table className="w-full">
                <thead className="border-b">
                  <tr>
                    <th className="p-4 text-center">
                      <input
                        type="checkbox"
                        checked={localCartItems.every(item => item.checked)}
                        onChange={(e) => handleSelectAll(e.target.checked)}
                        className="w-5 h-5 cursor-pointer"
                      />
                    </th>
                    <th className="text-left p-4">Product</th>
                    <th className="p-4">Price</th>
                    <th className="p-4">Quantity</th>
                    <th className="p-4">Total</th>
                    <th className="p-4"></th>
                  </tr>
                </thead>
                <tbody>
                  {localCartItems.map((item) => {
                    const imageUrl = resolveImageUrl(item.image);
                    const itemTotal = item.price * item.qty;

                    return (
                      <tr key={item.productId} className="border-b">
                        <td className="p-4 text-center">
                          <input
                            type="checkbox"
                            checked={item.checked}
                            onChange={() => handleCheckItem(item.productId)}
                            className="w-5 h-5 cursor-pointer"
                          />
                        </td>
                        <td className="p-4">
                          <div className="flex items-center gap-3">
                            <img
                              src={imageUrl}
                              alt={item.name}
                              className="w-20 h-20 object-cover rounded"
                              onError={(e) => {
                                e.target.src = 'https://via.placeholder.com/80?text=No+Image';
                              }}
                            />
                            <span className="font-medium">{item.name}</span>
                          </div>
                        </td>
                        <td className="p-4 text-center">₹{item.price.toFixed(2)}</td>
                        <td className="p-4">
                          <div className="flex items-center justify-center gap-2">
                            <button
                              onClick={() => handleDecrement(item.productId)}
                              className="w-8 h-8 rounded-full bg-gray-200 hover:bg-gray-300"
                            >
                              −
                            </button>
                            <span className="w-12 text-center">{item.qty}</span>
                            <button
                              onClick={() => handleIncrement(item.productId)}
                              className="w-8 h-8 rounded-full bg-gray-200 hover:bg-gray-300"
                            >
                              +
                            </button>
                          </div>
                        </td>
                        <td className="p-4 text-center font-semibold">
                          ₹{itemTotal.toFixed(2)}
                        </td>
                        <td className="p-4">
                          <button
                            onClick={() => handleRemove(item.productId)}
                            className="px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600"
                          >
                            Remove
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {/* Order Summary */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-lg shadow-md p-6 sticky top-24">
              <h2 className="text-xl font-semibold mb-4">Order Summary</h2>
              
              <div className="space-y-3 mb-4">
                <div className="flex justify-between">
                  <span>Selected Items Total:</span>
                  <span className="font-semibold">₹{getSelectedTotal().toFixed(2)}</span>
                </div>
                <div className="flex justify-between text-lg font-bold border-t pt-3">
                  <span>Grand Total:</span>
                  <span>₹{getCartTotal().toFixed(2)}</span>
                </div>
              </div>

              <button
                onClick={handleProceed}
                className="w-full bg-green-600 text-white py-3 rounded-lg hover:bg-green-700 font-semibold"
              >
                Proceed to Checkout
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Address Modal */}
      {showAddressModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg p-6 max-w-md w-full">
            <h2 className="text-2xl font-bold mb-4">Confirm Delivery Address</h2>
            
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">Full Name</label>
                <input
                  type="text"
                  value={addressForm.fullname}
                  onChange={(e) => setAddressForm({ ...addressForm, fullname: e.target.value })}
                  className="w-full border rounded px-3 py-2"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Phone Number</label>
                <input
                  type="tel"
                  value={addressForm.phone}
                  onChange={(e) => setAddressForm({ ...addressForm, phone: e.target.value })}
                  className="w-full border rounded px-3 py-2"
                  maxLength="10"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Address</label>
                <textarea
                  value={addressForm.address}
                  onChange={(e) => setAddressForm({ ...addressForm, address: e.target.value })}
                  className="w-full border rounded px-3 py-2"
                  rows="3"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Pincode</label>
                <input
                  type="text"
                  value={addressForm.pincode}
                  onChange={(e) => setAddressForm({ ...addressForm, pincode: e.target.value })}
                  className="w-full border rounded px-3 py-2"
                  maxLength="6"
                  required
                />
              </div>
            </div>

            <div className="flex gap-3 mt-6">
              <button
                onClick={() => setShowAddressModal(false)}
                className="flex-1 px-4 py-2 border rounded hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={handleAddressSubmit}
                className="flex-1 px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700"
              >
                Proceed to Payment
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Cart;