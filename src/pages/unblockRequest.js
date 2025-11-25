import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import api from '../utils/api';
import { showToast, showError, showSuccess } from '../utils/toast';

const UnblockRequest = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [formData, setFormData] = useState({
    username: '',
    userId: '',
    email: '',
    reason: ''
  });

  const [deactivationInfo, setDeactivationInfo] = useState(null);
  const [loading, setLoading] = useState(false);

  // Load initial user information
  useEffect(() => {
    const rawUser = localStorage.getItem('user');

    if (rawUser) {
      try {
        const user = JSON.parse(rawUser);

        const resolvedUserId = String(
          user.id ||
          user.userId ||
          user.user_id ||
          searchParams.get('userId') ||
          ''
        );

        setFormData(prev => ({
          ...prev,
          username: user.name || user.fullname || user.username || "Unknown User",
          userId: resolvedUserId,
          email: user.email || ""
        }));

        if (resolvedUserId) {
          fetchDeactivationInfo(resolvedUserId);
        }

      } catch (error) {
        console.error("Failed to parse stored user data:", error);
      }
    }

  }, [searchParams]);

  // Fetch existing deactivation info from backend
  const fetchDeactivationInfo = async (userId) => {
    try {
      const response = await api.post(
        "/AdminServlet",
        new URLSearchParams({ action: "getUserDeactivationInfo", userId }),
        { headers: { "Content-Type": "application/x-www-form-urlencoded" } }
      );

      if (response.data && response.data.deactivationMessage) {
        setDeactivationInfo(response.data);
      }

    } catch (error) {
      console.warn("Failed to fetch deactivation info:", error);
    }
  };

  const handleChange = (e) => {
    setFormData(prev => ({
      ...prev,
      [e.target.name]: e.target.value
    }));
  };

  // Submit unblock request
  const handleSubmit = async (e) => {
    e.preventDefault();

    // Validate userId
    if (!String(formData.userId).trim()) {
      showError("You must be logged in to submit an unblock request");
      navigate("/login");
      return;
    }

    // Validate reason
    if (!formData.reason || formData.reason.trim().length < 10) {
      showError("Please provide a detailed reason (at least 10 characters)");
      return;
    }

    setLoading(true);

    try {
      const response = await api.post(
        "/UnblockRequestServlet",
        new URLSearchParams(formData),
        { headers: { "Content-Type": "application/x-www-form-urlencoded" } }
      );

      if (response.data?.status === "success") {
        showSuccess("Unblock request successfully submitted! You'll be notified via email.");
        setTimeout(() => navigate("/login"), 2000);
      } else {
        showError(response.data?.message || "Failed to submit request.");
      }

    } catch (error) {
      showError("Failed to submit request. Please try again later.");
      console.error("Submit error:", error);

    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-100 flex items-center justify-center p-4">
      <div className="bg-white p-8 rounded-lg shadow-lg max-w-2xl w-full">

        {/* Header Section */}
        <div className="text-center mb-6">
          <svg xmlns="http://www.w3.org/2000/svg" className="h-16 w-16 text-yellow-500 mx-auto" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>

          <h1 className="text-3xl font-bold text-gray-800 mt-4">Account Deactivated</h1>
          <p className="text-gray-600 mt-2">Your account has been disabled by an administrator.</p>
        </div>

        {/* Admin message */}
        {deactivationInfo && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
            <h3 className="font-semibold text-red-800 mb-1">Admin Message:</h3>
            <p className="text-red-700">{deactivationInfo.deactivationMessage}</p>
            {deactivationInfo.deactivatedAt && (
              <p className="text-xs text-red-600 mt-2">
                Deactivated on: {new Date(deactivationInfo.deactivatedAt).toLocaleString()}
              </p>
            )}
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Username</label>
            <input
              type="text"
              name="username"
              value={formData.username}
              readOnly
              className="w-full bg-gray-100 border-gray-300 border rounded-lg px-4 py-2 cursor-not-allowed"
            />
          </div>

          <input type="hidden" name="userId" value={String(formData.userId)} />

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Your Email *</label>
            <input
              type="email"
              name="email"
              value={formData.email}
              required
              onChange={handleChange}
              className="w-full border-gray-300 border rounded-lg px-4 py-2 focus:ring-2 focus:ring-green-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Reason *</label>
            <textarea
              name="reason"
              value={formData.reason}
              required
              onChange={handleChange}
              rows="5"
              maxLength="1000"
              className="w-full border-gray-300 border rounded-lg px-4 py-2 focus:ring-2 focus:ring-green-500"
              placeholder="Explain why you think your account should be restored..."
            />
            <p className="text-xs text-gray-500">Maximum 1000 characters</p>
          </div>

          <div className="flex space-x-3 pt-4">
            <button
              type="submit"
              disabled={loading}
              className="flex-1 bg-green-600 hover:bg-green-700 text-white font-semibold py-3 rounded-lg disabled:opacity-50"
            >
              {loading ? "Submitting..." : "Submit Unblock Request"}
            </button>

            <button
              type="button"
              onClick={() => navigate("/login")}
              className="flex-1 bg-gray-500 hover:bg-gray-600 text-white font-semibold py-3 rounded-lg"
            >
              Back to Login
            </button>
          </div>

        </form>

        <div className="text-center mt-6 text-sm text-gray-600">
          Your request will be reviewed by our admin team.
          <br />
          Notification will be sent to your registered email.
        </div>

      </div>
    </div>
  );
};

export default UnblockRequest;
