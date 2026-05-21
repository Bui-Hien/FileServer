import { createSlice } from '@reduxjs/toolkit';

const initialState = {
  user: null,
  accessToken: null,
  isAuthenticated: false,
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    loginSuccess: (state, action) => {
      // action.payload contains { accessToken, refreshToken, username, roles, permissions } or freshUser
      state.accessToken = action.payload.accessToken || state.accessToken;
      state.user = {
        ...state.user,
        username: action.payload.username || (state.user ? state.user.username : null),
        roles: action.payload.roles || (state.user ? state.user.roles : []),
        permissions: action.payload.permissions || (state.user ? state.user.permissions : []),
        usedStorage: action.payload.usedStorage !== undefined ? action.payload.usedStorage : (state.user ? state.user.usedStorage : 0),
        maxStorage: action.payload.maxStorage !== undefined ? action.payload.maxStorage : (state.user ? state.user.maxStorage : 0)
      };
      state.isAuthenticated = true;

      // Save refresh token to cookie
      if (action.payload.refreshToken) {
        document.cookie = `refreshToken=${action.payload.refreshToken}; path=/; max-age=604800; SameSite=Strict; Secure`;
      }
    },
    logout: (state) => {
      state.user = null;
      state.accessToken = null;
      state.isAuthenticated = false;

      // Delete refresh token cookie
      document.cookie = "refreshToken=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT";
    },
    refreshSuccess: (state, action) => {
      state.accessToken = action.payload.accessToken;
      state.isAuthenticated = true;
      if (action.payload.refreshToken) {
        document.cookie = `refreshToken=${action.payload.refreshToken}; path=/; max-age=604800; SameSite=Strict; Secure`;
      }
    }
  }
});

export const { loginSuccess, logout, refreshSuccess } = authSlice.actions;
export default authSlice.reducer;
