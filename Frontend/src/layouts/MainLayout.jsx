import React, { useEffect } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import { AppBar, Toolbar, Typography, Container, Box, Button, Avatar, LinearProgress } from '@mui/material';
import FolderSharedIcon from '@mui/icons-material/FolderShared';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import CloudQueueIcon from '@mui/icons-material/CloudQueue';
import { loginSuccess, logout } from '../store/authSlice';
import { authService } from '../services/authService';

const MainLayout = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch();
  const user = useSelector((state) => state.auth.user);
  const accessToken = useSelector((state) => state.auth.accessToken);

  const getCookie = (name) => {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
  };

  useEffect(() => {
    const initOrFetch = async () => {
      try {
        if (!user) {
          const refreshToken = getCookie('refreshToken');
          if (refreshToken) {
            try {
              const refreshResult = await authService.refresh(refreshToken);
              dispatch(loginSuccess(refreshResult));
              return;
            } catch (err) {
              console.error('Silent refresh failed on init', err);
            }
          }
          // Redirect to real login
          navigate('/login');
        } else {
          // Tự động làm mới thông tin dung lượng Quota của User khi đổi trang
          const freshUser = await authService.getMe();
          dispatch(loginSuccess({
            ...freshUser,
            accessToken: accessToken,
            refreshToken: getCookie('refreshToken')
          }));
        }
      } catch (err) {
        console.error('Không thể khởi tạo phiên làm việc', err);
        navigate('/login');
      }
    };

    initOrFetch();
  }, [location.pathname]);

  if (!user) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
        <Typography variant="h6">Đang chuẩn bị không gian lưu trữ...</Typography>
      </Box>
    );
  }

  // Tính toán hạn mức dung lượng
  const usedStorageBytes = user.usedStorage || 0;
  const maxStorageBytes = user.maxStorage || (10 * 1024 * 1024 * 1024); // Mặc định 10GB nếu chưa tải xong
  const usedGB = (usedStorageBytes / (1024 * 1024 * 1024)).toFixed(2);
  const maxGB = (maxStorageBytes / (1024 * 1024 * 1024)).toFixed(0);
  const storagePercentage = Math.min((usedStorageBytes / maxStorageBytes) * 100, 100);

  const isAdmin = user.roles && user.roles.some((r) => r.code === 'ADMIN' || r === 'ADMIN');

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh', bgcolor: '#f8fafc' }}>
      <AppBar position="sticky" elevation={0} sx={{ borderBottom: '1px solid #e2e8f0', bgcolor: 'white', color: '#1e293b' }}>
        <Toolbar sx={{ justifyContent: 'space-between', px: { xs: 2, md: 4 } }}>
          <Box sx={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }} onClick={() => navigate('/')}>
            <FolderSharedIcon sx={{ fontSize: 32, color: '#2563eb', mr: 1.5 }} />
            <Typography variant="h6" fontWeight="bold" sx={{ color: '#0f172a', letterSpacing: 0.5 }}>
              FILE SERVER
            </Typography>
          </Box>

          <Box sx={{ display: 'flex', alignItems: 'center', gap: { xs: 1, md: 2 } }}>
            {/* Quota Progress Bar */}
            <Box sx={{ display: { xs: 'none', md: 'flex' }, flexDirection: 'column', width: 150, mr: 2 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 0.5 }}>
                <Typography variant="caption" color="text.secondary" sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                  <CloudQueueIcon sx={{ fontSize: 14 }} /> {usedGB} / {maxGB} GB
                </Typography>
              </Box>
              <LinearProgress
                variant="determinate"
                value={storagePercentage}
                sx={{
                  height: 6,
                  borderRadius: 3,
                  bgcolor: '#f1f5f9',
                  '& .MuiLinearProgress-bar': {
                    bgcolor: storagePercentage > 85 ? '#ef4444' : storagePercentage > 60 ? '#f59e0b' : '#3b82f6',
                    borderRadius: 3,
                  }
                }}
              />
            </Box>

            {/* Navigation links */}
            <Button
              onClick={() => navigate('/')}
              variant={location.pathname === '/' ? 'contained' : 'text'}
              sx={{
                borderRadius: 2,
                textTransform: 'none',
                fontWeight: 'bold',
                boxShadow: 'none',
                '&:hover': { boxShadow: 'none' }
              }}
            >
              Explorer
            </Button>

            {isAdmin && (
              <Button
                startIcon={<AdminPanelSettingsIcon />}
                onClick={() => navigate('/admin')}
                variant={location.pathname === '/admin' ? 'contained' : 'text'}
                color="secondary"
                sx={{
                  borderRadius: 2,
                  textTransform: 'none',
                  fontWeight: 'bold',
                  boxShadow: 'none',
                  '&:hover': { boxShadow: 'none' }
                }}
              >
                Admin Panel
              </Button>
            )}

            <Box sx={{ display: 'flex', alignItems: 'center', borderLeft: '1px solid #e2e8f0', pl: 2, ml: 1, gap: 1.5 }}>
              <Avatar
                sx={{
                  bgcolor: '#2563eb',
                  fontWeight: 'bold',
                  fontSize: '0.875rem',
                  width: 36,
                  height: 36
                }}
              >
                {user.username.substring(0, 2).toUpperCase()}
              </Avatar>
              <Button
                variant="outlined"
                color="error"
                size="small"
                onClick={() => {
                  dispatch(logout());
                  navigate('/login');
                }}
                sx={{
                  borderRadius: 2,
                  textTransform: 'none',
                  fontWeight: 'bold',
                }}
              >
                Đăng xuất
              </Button>
            </Box>
          </Box>
        </Toolbar>
      </AppBar>

      <Container maxWidth="xl" sx={{ flexGrow: 1, py: 4 }}>
        <Outlet />
      </Container>
    </Box>
  );
};

export default MainLayout;
