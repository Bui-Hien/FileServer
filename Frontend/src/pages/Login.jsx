import React, { useState } from 'react';
import { useDispatch } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { Box, Card, CardContent, Typography, TextField, Button, Alert, Grid, Avatar } from '@mui/material';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import { loginSuccess } from '../store/authSlice';
import { authService } from '../services/authService';

const Login = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const handleLogin = async (selectedUsername) => {
    const userToLogin = selectedUsername || username;
    const passwordToLogin = selectedUsername ? '123456' : password;

    if (!userToLogin.trim()) {
      setError('Vui lòng nhập tên đăng nhập!');
      return;
    }
    if (!passwordToLogin.trim()) {
      setError('Vui lòng nhập mật khẩu!');
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const loginResult = await authService.login(userToLogin.trim(), passwordToLogin.trim());
      dispatch(loginSuccess(loginResult));
      navigate('/');
    } catch (err) {
      console.error(err);
      setError('Đăng nhập thất bại. Vui lòng kiểm tra lại tài khoản và mật khẩu!');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)',
        padding: 3,
      }}
    >
      <Card
        sx={{
          maxWidth: 450,
          width: '100%',
          borderRadius: 4,
          boxShadow: '0 20px 25px -5px rgb(0 0 0 / 0.5), 0 8px 10px -6px rgb(0 0 0 / 0.5)',
          bgcolor: 'rgba(30, 41, 59, 0.7)',
          backdropFilter: 'blur(12px)',
          border: '1px solid rgba(255, 255, 255, 0.1)',
          color: 'white',
        }}
      >
        <CardContent sx={{ p: 4 }}>
          <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', mb: 3 }}>
            <Avatar sx={{ m: 1, bgcolor: 'primary.main', width: 56, height: 56 }}>
              <LockOutlinedIcon fontSize="large" />
            </Avatar>
            <Typography component="h1" variant="h4" fontWeight="bold" sx={{ mt: 1, letterSpacing: 0.5 }}>
              FILE SERVER
            </Typography>
            <Typography variant="body2" sx={{ color: 'slate.400', mt: 0.5 }}>
              Hệ thống lưu trữ & Phân quyền ACL tối ưu
            </Typography>
          </Box>

          {error && (
            <Alert severity="error" sx={{ mb: 3, borderRadius: 2 }}>
              {error}
            </Alert>
          )}

          <TextField
            margin="normal"
            required
            fullWidth
            id="username"
            label="Tên đăng nhập"
            name="username"
            autoComplete="username"
            autoFocus
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            disabled={loading}
            sx={{
              '& .MuiOutlinedInput-root': {
                color: 'white',
                '& fieldset': { borderColor: 'rgba(255,255,255,0.2)' },
                '&:hover fieldset': { borderColor: 'primary.main' },
              },
              '& .MuiInputLabel-root': { color: 'rgba(255,255,255,0.6)' },
              mb: 2
            }}
          />

          <TextField
            margin="normal"
            required
            fullWidth
            name="password"
            label="Mật khẩu"
            type="password"
            id="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            disabled={loading}
            sx={{
              '& .MuiOutlinedInput-root': {
                color: 'white',
                '& fieldset': { borderColor: 'rgba(255,255,255,0.2)' },
                '&:hover fieldset': { borderColor: 'primary.main' },
              },
              '& .MuiInputLabel-root': { color: 'rgba(255,255,255,0.6)' },
              mb: 3
            }}
          />

          <Button
            type="submit"
            fullWidth
            variant="contained"
            size="large"
            disabled={loading}
            onClick={() => handleLogin(null)}
            sx={{
              py: 1.5,
              borderRadius: 2,
              fontWeight: 'bold',
              textTransform: 'none',
              fontSize: '1rem',
              boxShadow: '0 4px 6px -1px rgba(59, 130, 246, 0.5)',
            }}
          >
            {loading ? 'Đang xác thực...' : 'Đăng nhập'}
          </Button>

          <Box sx={{ mt: 4, pt: 3, borderTop: '1px solid rgba(255, 255, 255, 0.1)' }}>
            <Typography variant="body2" align="center" sx={{ color: 'rgba(255,255,255,0.5)', mb: 2 }}>
              Hoặc đăng nhập nhanh bằng các tài khoản mặc định:
            </Typography>

            <Grid container spacing={2}>
              <Grid item xs={4}>
                <Button
                  fullWidth
                  variant="outlined"
                  size="small"
                  onClick={() => handleLogin('admin')}
                  sx={{
                    color: '#60a5fa',
                    borderColor: 'rgba(96, 165, 250, 0.3)',
                    textTransform: 'none',
                    '&:hover': { bgcolor: 'rgba(96, 165, 250, 0.1)' }
                  }}
                >
                  Admin
                </Button>
              </Grid>
              <Grid item xs={4}>
                <Button
                  fullWidth
                  variant="outlined"
                  size="small"
                  onClick={() => handleLogin('editor1')}
                  sx={{
                    color: '#34d399',
                    borderColor: 'rgba(52, 211, 153, 0.3)',
                    textTransform: 'none',
                    '&:hover': { bgcolor: 'rgba(52, 211, 153, 0.1)' }
                  }}
                >
                  Editor
                </Button>
              </Grid>
              <Grid item xs={4}>
                <Button
                  fullWidth
                  variant="outlined"
                  size="small"
                  onClick={() => handleLogin('viewer1')}
                  sx={{
                    color: '#fbbf24',
                    borderColor: 'rgba(251, 191, 36, 0.3)',
                    textTransform: 'none',
                    '&:hover': { bgcolor: 'rgba(251, 191, 36, 0.1)' }
                  }}
                >
                  Viewer
                </Button>
              </Grid>
            </Grid>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
};

export default Login;
