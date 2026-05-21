import axios from 'axios';
import { store } from '../store';
import { loginSuccess, logout } from '../store/authSlice';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Helper to extract a cookie value by name
const getCookie = (name) => {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(';').shift();
};

// Request Interceptor: Attach Access Token from Redux store RAM
api.interceptors.request.use(
  (config) => {
    const state = store.getState();
    const token = state.auth.accessToken;
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response Interceptor: Handle silent refresh on 401 Unauthorized
api.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error) => {
    const originalRequest = error.config;

    // Avoid infinite loop if auth requests fail or if it's already retried
    if (
      error.response &&
      error.response.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url.includes('/auth/login') &&
      !originalRequest.url.includes('/auth/refresh')
    ) {
      originalRequest._retry = true;

      const refreshToken = getCookie('refreshToken');
      if (refreshToken) {
        try {
          // Perform silent token refresh
          const response = await axios.post('http://localhost:8080/api/auth/refresh', {
            refreshToken,
          });

          const { accessToken, username, roles, permissions } = response.data;

          // Save new tokens to RAM store and cookie
          store.dispatch(
            loginSuccess({
              accessToken,
              refreshToken: response.data.refreshToken || refreshToken,
              username,
              roles,
              permissions,
            })
          );

          // Update header and retry the original request
          originalRequest.headers['Authorization'] = `Bearer ${accessToken}`;
          return api(originalRequest);
        } catch (refreshError) {
          // Refresh token expired or invalid: force logout
          store.dispatch(logout());
          return Promise.reject(refreshError);
        }
      } else {
        // No refresh token: force logout
        store.dispatch(logout());
      }
    }

    return Promise.reject(error);
  }
);

export default api;
