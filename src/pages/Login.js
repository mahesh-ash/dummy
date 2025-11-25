import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { showToast, showError } from '../utils/toast';
import api from '../utils/api';

const Login = () => {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [formData, setFormData] = useState({
    email: '',
    password: ''
  });
  const [loading, setLoading] = useState(false);
  const [showForgotPassword, setShowForgotPassword] = useState(false);
  const [forgotStep, setForgotStep] = useState(1);
  const [forgotData, setForgotData] = useState({
    email: '',
    otp: '',
    newPassword: '',
    confirmPassword: ''
  });

  useEffect(() => {
    const user = localStorage.getItem('user');
    if (user) {
      try {
        const userData = JSON.parse(user);
        if (userData && !userData.isBlocked) {
          navigate('/', { replace: true });
        }
      } catch (e) {
        console.error('Error parsing user data:', e);
      }
    }
  }, [navigate]);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleForgotChange = (e) => {
    setForgotData({
      ...forgotData,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const response = await login(formData.email, formData.password);

      if (response.status === 'success') {
        showToast('Login successful! Redirecting...', 'success');
        setTimeout(() => {
          navigate('/');
        }, 800);
      } else if (response.status === 'blocked') {
        showError('Your account is deactivated. Please submit an unblock request.');
        setTimeout(() => {
          const userId = response.user?.id || response.user?.userId || '';
          navigate(`/unblock-request${userId ? `?userId=${userId}` : ''}`);
        }, 1500);
      } else {
        showError(response.message || 'Invalid email or password');
      }
    } catch (error) {
      showError('Server error. Please try again later.');
      console.error('Login error:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSendOtp = async () => {
    if (!forgotData.email) {
      showError('Please enter your email');
      return;
    }

    try {
      const response = await api.post('/ForgotPasswordServlet',
        new URLSearchParams({ action: 'sendOtp', email: forgotData.email }),
        { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
      );

      if (response.data && (response.data.status === 'ok' || response.data.status === 'success')) {
        showToast('OTP sent to your email', 'success');
        setForgotStep(2);
      } else {
        showError(response.data?.message || 'Failed to send OTP');
      }
    } catch (error) {
      showError('Server error sending OTP');
    }
  };

  const handleResetPassword = async () => {
    if (!forgotData.otp || !forgotData.newPassword || !forgotData.confirmPassword) {
      showError('All fields are required');
      return;
    }

    if (forgotData.newPassword !== forgotData.confirmPassword) {
      showError('Passwords do not match');
      return;
    }

    try {
      const response = await api.post('/ForgotPasswordServlet',
        new URLSearchParams({
          action: 'reset',
          email: forgotData.email,
          otp: forgotData.otp,
          newPassword: forgotData.newPassword,
          confirmPassword: forgotData.confirmPassword
        }),
        { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
      );

      if (response.data && response.data.status === 'success') {
        showToast('Password updated successfully', 'success');
        setShowForgotPassword(false);
        setForgotStep(1);
        setForgotData({ email: '', otp: '', newPassword: '', confirmPassword: '' });
      } else if (response.data && response.data.status === 'blocked') {
        showError('Your account is blocked. Contact admin.');
      } else {
        showError(response.data?.message || 'Reset failed');
      }
    } catch (error) {
      showError('Server error while resetting password');
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-indigo-50 via-white to-indigo-100 flex items-center justify-center">
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

      <div className="w-full max-w-md p-8 bg-white rounded-2xl shadow-xl border border-gray-200 mt-16">
        {!showForgotPassword ? (
          <div>
            <h1 className="text-3xl font-bold text-center text-indigo-700 mb-6">Login</h1>
            
            <div className="space-y-6">
              <div>
                <label className="block text-gray-700 font-semibold mb-1">
                  Email <span className="text-red-500">*</span>
                </label>
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleChange}
                  required
                  className="w-full px-4 py-2 border rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  placeholder="Enter your email"
                />
              </div>

              <div>
                <label className="block text-gray-700 font-semibold mb-1">
                  Password <span className="text-red-500">*</span>
                </label>
                <input
                  type="password"
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  required
                  className="w-full px-4 py-2 border rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  placeholder="Enter your password"
                />
              </div>

              <button
                onClick={handleSubmit}
                disabled={loading}
                className="w-full bg-indigo-600 text-white font-semibold py-3 rounded-lg shadow hover:bg-indigo-700 transition duration-300 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {loading ? 'Logging in...' : 'Submit'}
              </button>
            </div>

            <p className="text-center mt-4">
              <button
                onClick={() => setShowForgotPassword(true)}
                className="text-indigo-600 hover:underline"
              >
                Forgot Password?
              </button>
            </p>

            <p className="text-center text-sm text-gray-500 mt-6">
              Don't have an account?{' '}
              <Link to="/register" className="text-indigo-600 font-semibold hover:underline">
                Register here
              </Link>
            </p>
          </div>
        ) : (
          <div>
            <h2 className="text-2xl font-bold text-center text-indigo-700 mb-6">
              Forgot Password
            </h2>

            {forgotStep === 1 ? (
              <div className="space-y-4">
                <div>
                  <label className="block text-gray-700 font-semibold mb-1">Email</label>
                  <input
                    type="email"
                    name="email"
                    value={forgotData.email}
                    onChange={handleForgotChange}
                    required
                    className="w-full px-4 py-2 border rounded-lg"
                    placeholder="Enter your registered email"
                  />
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={handleSendOtp}
                    className="flex-1 bg-indigo-600 text-white py-2 rounded-lg hover:bg-indigo-700"
                  >
                    Send OTP
                  </button>
                  <button
                    onClick={() => setShowForgotPassword(false)}
                    className="flex-1 bg-gray-400 text-white py-2 rounded-lg hover:bg-gray-500"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            ) : (
              <div className="space-y-3">
                <input
                  type="text"
                  name="otp"
                  value={forgotData.otp}
                  onChange={handleForgotChange}
                  placeholder="Enter OTP"
                  className="w-full px-4 py-2 border rounded-lg"
                />
                <input
                  type="password"
                  name="newPassword"
                  value={forgotData.newPassword}
                  onChange={handleForgotChange}
                  placeholder="New Password"
                  className="w-full px-4 py-2 border rounded-lg"
                />
                <input
                  type="password"
                  name="confirmPassword"
                  value={forgotData.confirmPassword}
                  onChange={handleForgotChange}
                  placeholder="Confirm Password"
                  className="w-full px-4 py-2 border rounded-lg"
                />
                <div className="flex gap-2">
                  <button
                    onClick={handleResetPassword}
                    className="flex-1 bg-indigo-600 text-white py-2 rounded-lg hover:bg-indigo-700"
                  >
                    Reset Password
                  </button>
                  <button
                    onClick={() => {
                      setShowForgotPassword(false);
                      setForgotStep(1);
                    }}
                    className="flex-1 bg-gray-400 text-white py-2 rounded-lg hover:bg-gray-500"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default Login;