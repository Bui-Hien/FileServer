import React, { useState, useEffect } from 'react';
import {
  Box, Paper, Tabs, Tab, Typography, Button, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Chip, IconButton, Dialog, DialogTitle,
  DialogContent, DialogActions, TextField, MenuItem, Select, FormControl,
  InputLabel, Grid, Checkbox, ListItemText, LinearProgress, Snackbar, Alert
} from '@mui/material';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import SecurityIcon from '@mui/icons-material/Security';
import HistoryIcon from '@mui/icons-material/History';
import KeyIcon from '@mui/icons-material/Key';
import AddCardIcon from '@mui/icons-material/AddCard';

import { adminService } from '../services/adminService';

const AdminPanel = () => {
  const [activeTab, setActiveTab] = useState(0);

  // Users, Roles, Permissions and Logs states
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [systemPermissions, setSystemPermissions] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);

  // Toast notification
  const [toast, setToast] = useState({ open: false, message: '', severity: 'success' });

  // Create user state
  const [userDialogOpen, setUserDialogOpen] = useState(false);
  const [newUser, setNewUser] = useState({
    username: '',
    email: '',
    password: '',
    fullName: '',
    maxStorageGB: 10,
    roleCodes: []
  });

  // Assign roles state
  const [rolesDialogOpen, setRolesDialogOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);
  const [assignedRoleCodes, setAssignedRoleCodes] = useState([]);

  // Manage role permissions state
  const [rolePermsDialogOpen, setRolePermsDialogOpen] = useState(false);
  const [selectedRole, setSelectedRole] = useState(null);
  const [assignedPermCodes, setAssignedPermCodes] = useState([]);

  // Create Role state
  const [createRoleDialogOpen, setCreateRoleDialogOpen] = useState(false);
  const [newRole, setNewRole] = useState({ code: '', name: '' });

  // Create Permission state
  const [createPermDialogOpen, setCreatePermDialogOpen] = useState(false);
  const [newPerm, setNewPerm] = useState({ code: '', name: '' });

  useEffect(() => {
    fetchUsersData();
    fetchRolesAndPermissions();
    fetchAuditLogs();
  }, []);

  const showToast = (message, severity = 'success') => {
    setToast({ open: true, message, severity });
  };

  const fetchUsersData = async () => {
    try {
      const data = await adminService.getAllUsers();
      setUsers(data);
    } catch (err) {
      console.error(err);
    }
  };

  const fetchRolesAndPermissions = async () => {
    try {
      const rolesData = await adminService.getAllRoles();
      setRoles(rolesData);

      const permsData = await adminService.getAllPermissions();
      setSystemPermissions(permsData);
    } catch (err) {
      console.error(err);
    }
  };

  const fetchAuditLogs = async () => {
    try {
      const logs = await adminService.getAllAuditLogs();
      setAuditLogs(logs);
    } catch (err) {
      console.error(err);
    }
  };

  // Tạo người dùng mới
  const handleCreateUser = async () => {
    try {
      const quotaBytes = newUser.maxStorageGB * 1024 * 1024 * 1024;
      await adminService.createUser({
        username: newUser.username.trim(),
        email: newUser.email.trim(),
        password: newUser.password,
        fullName: newUser.fullName.trim(),
        maxStorage: quotaBytes,
        roleCodes: newUser.roleCodes
      });
      showToast('Tạo tài khoản người dùng thành công!');
      setUserDialogOpen(false);
      setNewUser({
        username: '',
        email: '',
        password: '',
        fullName: '',
        maxStorageGB: 10,
        roleCodes: []
      });
      fetchUsersData();
    } catch (err) {
      showToast('Tạo người dùng thất bại! Tên đăng nhập có thể đã bị trùng.', 'error');
    }
  };

  // Tạo Vai trò mới (Roles)
  const handleCreateRole = async () => {
    if (!newRole.code.trim() || !newRole.name.trim()) return;
    try {
      await adminService.createRole({
        code: newRole.code.trim().toUpperCase(),
        name: newRole.name.trim()
      });
      showToast('Khởi tạo vai trò mới thành công!');
      setCreateRoleDialogOpen(false);
      setNewRole({ code: '', name: '' });
      fetchRolesAndPermissions();
    } catch (err) {
      showToast('Tạo vai trò thất bại! Mã vai trò có thể đã tồn tại.', 'error');
    }
  };

  // Tạo Quyền hạn mới (System Static Permissions)
  const handleCreatePermission = async () => {
    if (!newPerm.code.trim() || !newPerm.name.trim()) return;
    try {
      await adminService.createPermission({
        code: newPerm.code.trim().toUpperCase(),
        name: newPerm.name.trim()
      });
      showToast('Đăng ký quyền hạn hệ thống mới thành công!');
      setCreatePermDialogOpen(false);
      setNewPerm({ code: '', name: '' });
      fetchRolesAndPermissions();
    } catch (err) {
      showToast('Tạo quyền hạn thất bại! Mã quyền hạn có thể đã tồn tại.', 'error');
    }
  };

  // Mở Dialog gán vai trò cho User
  const handleOpenAssignRoles = (user) => {
    setSelectedUser(user);
    setAssignedRoleCodes(user.roles ? Array.from(user.roles) : []);
    setRolesDialogOpen(true);
  };

  // Lưu vai trò được gán cho User
  const handleSaveUserRoles = async () => {
    try {
      await adminService.assignRoles(selectedUser.id, assignedRoleCodes);
      showToast('Cập nhật vai trò người dùng thành công!');
      setRolesDialogOpen(false);
      fetchUsersData();
    } catch (err) {
      showToast('Lỗi khi cập nhật vai trò người dùng!', 'error');
    }
  };

  // Mở Dialog quản lý quyền cho Vai trò
  const handleOpenRolePermissions = (role) => {
    setSelectedRole(role);
    setAssignedPermCodes(role.permissions ? Array.from(role.permissions) : []);
    setRolePermsDialogOpen(true);
  };

  // Lưu quyền hạn được gán cho Vai trò (RBAC)
  const handleSaveRolePermissions = async () => {
    try {
      await adminService.assignPermissionsToRole(selectedRole.id, assignedPermCodes);
      showToast(`Cập nhật quyền hạn cho vai trò "${selectedRole.code}" thành công!`);
      setRolePermsDialogOpen(false);
      fetchRolesAndPermissions();
    } catch (err) {
      showToast('Lỗi khi cập nhật quyền hạn vai trò!', 'error');
    }
  };

  return (
    <Box>
      <Typography variant="h4" fontWeight="bold" color="#0f172a" sx={{ mb: 4 }}>
        Hệ thống Quản trị & Điều phối Admin
      </Typography>

      <Paper sx={{ borderRadius: 3, mb: 4, overflow: 'hidden', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.05)' }}>
        <Tabs
          value={activeTab}
          onChange={(e, val) => setActiveTab(val)}
          indicatorColor="primary"
          textColor="primary"
          variant="fullWidth"
          sx={{ borderBottom: '1px solid #e2e8f0', bgcolor: '#f8fafc' }}
        >
          <Tab icon={<PersonAddIcon />} label="Quản lý Người dùng" sx={{ py: 2, fontWeight: 'bold', textTransform: 'none' }} />
          <Tab icon={<SecurityIcon />} label="Vai trò & Quyền tĩnh" sx={{ py: 2, fontWeight: 'bold', textTransform: 'none' }} />
          <Tab icon={<HistoryIcon />} label="Nhật ký hoạt động" sx={{ py: 2, fontWeight: 'bold', textTransform: 'none' }} />
        </Tabs>

        {/* Tab 1: Users Management */}
        {activeTab === 0 && (
          <Box sx={{ p: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
              <Typography variant="h6" fontWeight="bold" color="#334155">Danh sách tài khoản nhân viên</Typography>
              <Button
                variant="contained"
                startIcon={<PersonAddIcon />}
                onClick={() => setUserDialogOpen(true)}
                sx={{ textTransform: 'none', borderRadius: 2, fontWeight: 'bold' }}
              >
                Thêm tài khoản
              </Button>
            </Box>

            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Username</TableCell>
                    <TableCell>Họ và tên</TableCell>
                    <TableCell>Email</TableCell>
                    <TableCell>Vai trò gán</TableCell>
                    <TableCell>Dung lượng lưu trữ (Quota)</TableCell>
                    <TableCell align="right">Hành động</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {users.map((usr) => {
                    const usagePercentage = usr.maxStorage ? Math.min((usr.usedStorage / usr.maxStorage) * 100, 100) : 0;
                    return (
                      <TableRow key={usr.id} hover>
                        <TableCell fontWeight="semibold" color="#0f172a">{usr.username}</TableCell>
                        <TableCell>{usr.fullName}</TableCell>
                        <TableCell>{usr.email}</TableCell>
                        <TableCell>
                          <Typography variant="body2" fontWeight="medium" color="text.primary">
                            {usr.roleNames || 'Chưa gán'}
                          </Typography>
                        </TableCell>
                        <TableCell sx={{ width: 220 }}>
                          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                            <Typography variant="caption" color="text.secondary">
                              {usr.formattedUsedStorage}
                            </Typography>
                            <Typography variant="caption" fontWeight="bold">
                              {usr.formattedMaxStorage} ({usagePercentage.toFixed(0)}%)
                            </Typography>
                          </Box>
                          <LinearProgress
                            variant="determinate"
                            value={usagePercentage}
                            sx={{ height: 6, borderRadius: 3 }}
                          />
                        </TableCell>
                        <TableCell align="right">
                          <Button
                            size="small"
                            variant="outlined"
                            startIcon={<KeyIcon />}
                            onClick={() => handleOpenAssignRoles(usr)}
                            sx={{ textTransform: 'none', borderRadius: 1.5, fontWeight: 'bold' }}
                          >
                            Phân vai trò
                          </Button>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </TableContainer>
          </Box>
        )}

        {/* Tab 2: Roles and Permissions Management */}
        {activeTab === 1 && (
          <Box sx={{ p: 3 }}>
            <Grid container spacing={4}>
              {/* Left Column: Roles list */}
              <Grid item xs={12} md={7}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                  <Typography variant="h6" fontWeight="bold" color="#334155">
                    Danh sách Vai trò & Quyền hạn (RBAC)
                  </Typography>
                  <Button
                    variant="outlined"
                    startIcon={<SecurityIcon />}
                    onClick={() => setCreateRoleDialogOpen(true)}
                    sx={{ textTransform: 'none', borderRadius: 2, fontWeight: 'bold', px: 2 }}
                  >
                    Thêm vai trò
                  </Button>
                </Box>

                <TableContainer component={Paper} sx={{ boxShadow: 'none', border: '1px solid #e2e8f0', borderRadius: 3 }}>
                  <Table>
                    <TableHead sx={{ bgcolor: '#f8fafc' }}>
                      <TableRow>
                        <TableCell>Mã vai trò</TableCell>
                        <TableCell>Tên hiển thị</TableCell>
                        <TableCell>Quyền tĩnh liên kết</TableCell>
                        <TableCell align="right">Hành động</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {roles.map((role) => (
                        <TableRow key={role.id} hover>
                          <TableCell><Chip label={role.code} color="secondary" size="small" sx={{ fontWeight: 'bold' }} /></TableCell>
                          <TableCell fontWeight="medium">{role.name}</TableCell>
                          <TableCell>
                            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, maxWidth: 300 }}>
                              {role.permissions && role.permissions.map(p => (
                                <Chip key={p} label={p} size="small" variant="outlined" sx={{ fontSize: '0.75rem' }} />
                              ))}
                              {(!role.permissions || role.permissions.length === 0) && (
                                <Typography variant="caption" color="text.secondary">Chưa gán quyền</Typography>
                              )}
                            </Box>
                          </TableCell>
                          <TableCell align="right">
                            <Button
                              size="small"
                              variant="outlined"
                              onClick={() => handleOpenRolePermissions(role)}
                              sx={{ textTransform: 'none', borderRadius: 1.5, fontWeight: 'bold' }}
                            >
                              Gán quyền
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Grid>

              {/* Right Column: System static permissions list */}
              <Grid item xs={12} md={5}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                  <Typography variant="h6" fontWeight="bold" color="#334155">
                    Danh mục Quyền hạn hệ thống
                  </Typography>
                  <Button
                    variant="outlined"
                    startIcon={<KeyIcon />}
                    color="secondary"
                    onClick={() => setCreatePermDialogOpen(true)}
                    sx={{ textTransform: 'none', borderRadius: 2, fontWeight: 'bold', px: 2 }}
                  >
                    Thêm quyền
                  </Button>
                </Box>

                <TableContainer component={Paper} sx={{ boxShadow: 'none', border: '1px solid #e2e8f0', borderRadius: 3 }}>
                  <Table>
                    <TableHead sx={{ bgcolor: '#f8fafc' }}>
                      <TableRow>
                        <TableCell>Mã quyền</TableCell>
                        <TableCell>Mô tả hành động</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {systemPermissions.map((perm) => (
                        <TableRow key={perm.id}>
                          <TableCell><Chip label={perm.code} size="small" sx={{ fontWeight: 'bold' }} /></TableCell>
                          <TableCell variant="body2">{perm.name}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Grid>
            </Grid>
          </Box>
        )}

        {/* Tab 3: Audit Logs */}
        {activeTab === 2 && (
          <Box sx={{ p: 3 }}>
            <Typography variant="h6" fontWeight="bold" color="#334155" sx={{ mb: 3 }}>
              Nhật ký kiểm toán hệ thống (Audit Logs)
            </Typography>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Thời gian</TableCell>
                    <TableCell>Tài khoản tác động</TableCell>
                    <TableCell>Hành động</TableCell>
                    <TableCell>Tài nguyên</TableCell>
                    <TableCell>ID Tài nguyên</TableCell>
                    <TableCell>Địa chỉ IP</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {auditLogs.map((log) => (
                    <TableRow key={log.id} hover>
                      <TableCell>{new Date(log.createdAt).toLocaleString()}</TableCell>
                      <TableCell fontWeight="semibold">{log.username}</TableCell>
                      <TableCell>
                        <Chip
                          label={log.action}
                          size="small"
                          color={log.action.includes('DELETE') ? 'error' : log.action.includes('UPLOAD') ? 'success' : 'default'}
                          sx={{ fontWeight: 'bold' }}
                        />
                      </TableCell>
                      <TableCell>{log.resourceType}</TableCell>
                      <TableCell>{log.resourceId || '-'}</TableCell>
                      <TableCell>{log.ipAddress || '-'}</TableCell>
                    </TableRow>
                  ))}
                  {auditLogs.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={6} align="center" sx={{ py: 8 }}>
                        <Typography color="text.secondary">Chưa có nhật ký ghi nhận hoạt động nào!</Typography>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </Box>
        )}
      </Paper>

      {/* Dialog Tạo Người Dùng Mới */}
      <Dialog open={userDialogOpen} onClose={() => setUserDialogOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle sx={{ fontWeight: 'bold' }}>Tạo tài khoản mới</DialogTitle>
        <DialogContent>
          <Grid container spacing={2.5} sx={{ mt: 1 }}>
            <Grid item xs={6}>
              <TextField
                label="Username"
                fullWidth
                value={newUser.username}
                onChange={(e) => setNewUser({ ...newUser, username: e.target.value })}
              />
            </Grid>
            <Grid item xs={6}>
              <TextField
                label="Mật khẩu"
                type="password"
                fullWidth
                value={newUser.password}
                onChange={(e) => setNewUser({ ...newUser, password: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Họ và tên"
                fullWidth
                value={newUser.fullName}
                onChange={(e) => setNewUser({ ...newUser, fullName: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Email"
                type="email"
                fullWidth
                value={newUser.email}
                onChange={(e) => setNewUser({ ...newUser, email: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Hạn mức lưu trữ (Quota GB)"
                type="number"
                fullWidth
                value={newUser.maxStorageGB}
                onChange={(e) => setNewUser({ ...newUser, maxStorageGB: parseInt(e.target.value) || 10 })}
              />
            </Grid>
            <Grid item xs={12}>
              <FormControl fullWidth>
                <InputLabel id="new-user-roles-label">Vai trò ban đầu</InputLabel>
                <Select
                  labelId="new-user-roles-label"
                  multiple
                  value={newUser.roleCodes}
                  onChange={(e) => setNewUser({ ...newUser, roleCodes: e.target.value })}
                  renderValue={(selected) => selected.join(', ')}
                >
                  {roles.map((r) => (
                    <MenuItem key={r.code} value={r.code}>
                      <Checkbox checked={newUser.roleCodes.indexOf(r.code) > -1} />
                      <ListItemText primary={r.name} />
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions sx={{ p: 2.5 }}>
          <Button onClick={() => setUserDialogOpen(false)} color="inherit">Hủy bỏ</Button>
          <Button onClick={handleCreateUser} variant="contained">Tạo tài khoản</Button>
        </DialogActions>
      </Dialog>

      {/* Dialog Tạo Vai Trò Mới */}
      <Dialog open={createRoleDialogOpen} onClose={() => setCreateRoleDialogOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle sx={{ fontWeight: 'bold' }}>Tạo vai trò mới</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
          <TextField
            label="Mã vai trò (VD: EDITOR)"
            fullWidth
            value={newRole.code}
            onChange={(e) => setNewRole({ ...newRole, code: e.target.value })}
            helperText="Nhập viết hoa không chứa dấu cách"
          />
          <TextField
            label="Tên hiển thị (VD: Biên tập viên)"
            fullWidth
            value={newRole.name}
            onChange={(e) => setNewRole({ ...newRole, name: e.target.value })}
          />
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setCreateRoleDialogOpen(false)} color="inherit">Hủy bỏ</Button>
          <Button onClick={handleCreateRole} variant="contained" disabled={!newRole.code.trim() || !newRole.name.trim()}>Khởi tạo</Button>
        </DialogActions>
      </Dialog>

      {/* Dialog Tạo Quyền Hạn Mới */}
      <Dialog open={createPermDialogOpen} onClose={() => setCreatePermDialogOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle sx={{ fontWeight: 'bold' }}>Đăng ký quyền hạn mới</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
          <TextField
            label="Mã quyền hạn (VD: FILE_DELETE)"
            fullWidth
            value={newPerm.code}
            onChange={(e) => setNewPerm({ ...newPerm, code: e.target.value })}
            helperText="Mã tĩnh viết hoa, ví dụ: ACTION_CODE"
          />
          <TextField
            label="Mô tả quyền (VD: Cho phép xóa tệp)"
            fullWidth
            value={newPerm.name}
            onChange={(e) => setNewPerm({ ...newPerm, name: e.target.value })}
          />
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setCreatePermDialogOpen(false)} color="inherit">Hủy bỏ</Button>
          <Button onClick={handleCreatePermission} variant="contained" disabled={!newPerm.code.trim() || !newPerm.name.trim()}>Đăng ký</Button>
        </DialogActions>
      </Dialog>

      {/* Dialog Gán Vai Trò cho Người Dùng */}
      <Dialog open={rolesDialogOpen} onClose={() => setRolesDialogOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle sx={{ fontWeight: 'bold' }}>
          Phân vai trò: {selectedUser?.username}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ mt: 2 }}>
            <FormControl fullWidth>
              <InputLabel id="assign-roles-label">Danh sách vai trò</InputLabel>
              <Select
                labelId="assign-roles-label"
                multiple
                value={assignedRoleCodes}
                onChange={(e) => setAssignedRoleCodes(e.target.value)}
                renderValue={(selected) => selected.join(', ')}
              >
                {roles.map((r) => (
                  <MenuItem key={r.code} value={r.code}>
                    <Checkbox checked={assignedRoleCodes.indexOf(r.code) > -1} />
                    <ListItemText primary={r.name} />
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setRolesDialogOpen(false)} color="inherit">Hủy</Button>
          <Button onClick={handleSaveUserRoles} variant="contained">Cập nhật</Button>
        </DialogActions>
      </Dialog>

      {/* Dialog Gán Quyền cho Vai trò (RBAC) */}
      <Dialog open={rolePermsDialogOpen} onClose={() => setRolePermsDialogOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle sx={{ fontWeight: 'bold' }}>
          Cập nhật quyền: Vai trò {selectedRole?.code}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ mt: 2 }}>
            <FormControl fullWidth>
              <InputLabel id="assign-perms-label">Danh mục quyền hạn</InputLabel>
              <Select
                labelId="assign-perms-label"
                multiple
                value={assignedPermCodes}
                onChange={(e) => setAssignedPermCodes(e.target.value)}
                renderValue={(selected) => selected.join(', ')}
              >
                {systemPermissions.map((p) => (
                  <MenuItem key={p.code} value={p.code}>
                    <Checkbox checked={assignedPermCodes.indexOf(p.code) > -1} />
                    <ListItemText primary={`${p.code} (${p.name})`} />
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setRolePermsDialogOpen(false)} color="inherit">Hủy</Button>
          <Button onClick={handleSaveRolePermissions} variant="contained">Lưu cấu hình</Button>
        </DialogActions>
      </Dialog>

      {/* Snackbar alerts */}
      <Snackbar
        open={toast.open}
        autoHideDuration={6000}
        onClose={() => setToast({ ...toast, open: false })}
      >
        <Alert onClose={() => setToast({ ...toast, open: false })} severity={toast.severity} sx={{ borderRadius: 2 }}>
          {toast.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default AdminPanel;
