import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { showSuccess, showError } from '../utils/toast';

const Register = () => {
  const navigate = useNavigate();
  const { register } = useAuth();
  const [formData, setFormData] = useState({
    fname: '',
    email: '',
    phone: '',
    password: '',
    confirm_password: '',
    full_address: '',
    pincode: ''
  });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });

    // Real-time validation
    let newErrors = { ...errors };

    if (name === 'fname') {
      if (!/^[a-zA-Z\s]*$/.test(value)) {
        newErrors.fname = 'Full name must contain only letters and spaces';
      } else if (value.trim().length > 0 && value.trim().length < 3) {
        newErrors.fname = 'Full name must be at least 3 characters long';
      } else {
        delete newErrors.fname;
      }
    }

    if (name === 'email') {
      if (value.length > 0 && /^[A-Z]/.test(value)) {
        newErrors.email = 'Email must not start with an uppercase letter';
      } else if (!/^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$/i.test(value) && value.length > 0) {
        newErrors.email = 'Please enter a valid email address';
      } else {
        delete newErrors.email;
      }
    }

    if (name === 'phone') {
      if (!/^[0-9]*$/.test(value)) {
        newErrors.phone = 'Phone must contain only numbers';
      } else if (value.length > 0 && value.length !== 10) {
        newErrors.phone = 'Phone must be 10 digits';
      } else {
        delete newErrors.phone;
      }
    }

    if (name === 'confirm_password' || name === 'password') {
      if (name === 'confirm_password' && value !== formData.password) {
        newErrors.confirm_password = 'Passwords do not match';
      } else if (name === 'password' && formData.confirm_password && value !== formData.confirm_password) {
        newErrors.confirm_password = 'Passwords do not match';
      } else {
        delete newErrors.confirm_password;
      }
    }

    if (name === 'pincode') {
      if (!/^[0-9]{6}$/.test(value) && value.length > 0) {
        newErrors.pincode = 'Pincode must be a 6-digit number';
      } else {
        delete newErrors.pincode;
      }
    }

    setErrors(newErrors);
  };

  const validateForm = () => {
    const newErrors = {};

    if (!formData.fname || formData.fname.trim().length < 3) {
      newErrors.fname = 'Full name must be at least 3 characters';
    }

    if (!/^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$/i.test(formData.email)) {
      newErrors.email = 'Please enter a valid email address';
    }

    if (!/^[0-9]{10}$/.test(formData.phone)) {
      newErrors.phone = 'Phone must be a 10-digit number';
    }

    if (!formData.password || formData.password.length < 6) {
      newErrors.password = 'Password must be at least 6 characters';
    }

    if (formData.password !== formData.confirm_password) {
      newErrors.confirm_password = 'Passwords do not match';
    }

    if (!formData.full_address || formData.full_address.trim().length === 0) {
      newErrors.full_address = 'Address is required';
    }

    if (!/^[0-9]{6}$/.test(formData.pincode)) {
      newErrors.pincode = 'Pincode must be a 6-digit number';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      showError('Please fix all errors before submitting');
      return;
    }

    setLoading(true);

    try {
      const response = await register(formData);

      if (response.status === 'success') {
        showSuccess('Registered successfully! Redirecting to login...', () => {
          navigate('/login');
        });
      } else if (response.status === 'exists') {
        showError('An account with this email already exists!');
      } else {
        showError(response.message || 'Registration failed! Please try again later.');
      }
    } catch (error) {
      showError('Server error. Please try again later.');
      console.error('Registration error:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-indigo-50 via-white to-indigo-100 flex flex-col">
      <nav className="bg-green-600 text-white fixed w-full top-0 z-50 shadow-md">
        <div className="max-w-7xl mx-auto px-4 flex items-center justify-between h-16">
          <Link to="/" className="text-xl font-semibold">Shopping Center</Link>
          <div className="flex items-center space-x-4">
            <Link to="/login" className="px-4 py-2 rounded-md hover:bg-yellow-300">Login</Link>
            <Link to="/register" className="px-4 py-2 rounded-md hover:bg-yellow-300">Register</Link>
            <Link to="/" className="px-4 py-2 rounded-md bg-gray-400 text-white hover:bg-gray-500">Back</Link>
          </div>
        </div>
      </nav>

      <div className="flex-grow flex justify-center items-center pt-20 pb-8">
        <div className="w-full max-w-3xl bg-white rounded-2xl shadow-lg border border-gray-200 p-8 mx-4">
          <h1 className="text-3xl font-bold text-center text-indigo-700 mb-6">Register</h1>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Full Name <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                name="fname"
                value={formData.fname}
                onChange={handleChange}
                required
                className={`w-full border ${errors.fname ? 'border-red-500' : 'border-gray-300'} rounded px-3 py-2`}
                placeholder="Enter your full name"
              />
              {errors.fname && <p className="text-red-500 text-sm mt-1">{errors.fname}</p>}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Email <span className="text-red-500">*</span>
              </label>
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                required
                className={`w-full border ${errors.email ? 'border-red-500' : 'border-gray-300'} rounded px-3 py-2`}
                placeholder="Enter your email"
              />
              {errors.email && <p className="text-red-500 text-sm mt-1">{errors.email}</p>}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Phone Number <span className="text-red-500">*</span>
              </label>
              <input
                type="tel"
                name="phone"
                value={formData.phone}
                onChange={handleChange}
                required
                maxLength="10"
                className={`w-full border ${errors.phone ? 'border-red-500' : 'border-gray-300'} rounded px-3 py-2`}
                placeholder="Enter 10-digit phone number"
              />
              {errors.phone && <p className="text-red-500 text-sm mt-1">{errors.phone}</p>}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Pincode <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                name="pincode"
                value={formData.pincode}
                onChange={handleChange}
                required
                maxLength="6"
                className={`w-full border ${errors.pincode ? 'border-red-500' : 'border-gray-300'} rounded px-3 py-2`}
                placeholder="Enter 6-digit pincode"
              />
              {errors.pincode && <p className="text-red-500 text-sm mt-1">{errors.pincode}</p>}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Password <span className="text-red-500">*</span>
              </label>
              <input
                type="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                required
                className={`w-full border ${errors.password ? 'border-red-500' : 'border-gray-300'} rounded px-3 py-2`}
                placeholder="Enter password"
              />
              {errors.password && <p className="text-red-500 text-sm mt-1">{errors.password}</p>}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Confirm Password <span className="text-red-500">*</span>
              </label>
              <input
                type="password"
                name="confirm_password"
                value={formData.confirm_password}
                onChange={handleChange}
                required
                className={`w-full border ${errors.confirm_password ? 'border-red-500' : 'border-gray-300'} rounded px-3 py-2`}
                placeholder="Confirm password"
              />
              {errors.confirm_password && <p className="text-red-500 text-sm mt-1">{errors.confirm_password}</p>}
            </div>

            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Full Address <span className="text-red-500">*</span>
              </label>
              <textarea
                name="full_address"
                value={formData.full_address}
                onChange={handleChange}
                required
                rows="3"
                className={`w-full border ${errors.full_address ? 'border-red-500' : 'border-gray-300'} rounded px-3 py-2`}
                placeholder="Enter your full address"
              />
              {errors.full_address && <p className="text-red-500 text-sm mt-1">{errors.full_address}</p>}
            </div>

            <div className="md:col-span-2">
              <button
                onClick={handleSubmit}
                disabled={loading}
                className="w-full bg-green-600 text-white py-2 rounded-lg hover:bg-green-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {loading ? 'Registering...' : 'Submit'}
              </button>
            </div>
          </div>

          <p className="text-center text-sm text-gray-500 mt-4">
            Already have an account?{' '}
            <Link to="/login" className="text-indigo-600 font-semibold hover:underline">
              Login here
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default Register;