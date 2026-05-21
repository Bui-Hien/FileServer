import api from './api';

export const adminService = {
  // Users management
  getAllUsers: async () => {
    const response = await api.get('/users');
    return response.data;
  },
  getUserById: async (id) => {
    const response = await api.get(`/users/${id}`);
    return response.data;
  },
  createUser: async (userPayload) => {
    const response = await api.post('/users', userPayload);
    return response.data;
  },
  updateUser: async (id, userPayload) => {
    const response = await api.post(`/users/${id}`, userPayload); // Note: backend could be PUT or POST, let's verify if UserController uses PUT. Ah, UserController uses PUT /api/users/{id}! Yes. Let's use PUT!
    return response.data;
  },
  updateUserPut: async (id, userPayload) => {
    const response = await api.put(`/users/${id}`, userPayload);
    return response.data;
  },
  assignRoles: async (id, roleCodes) => {
    const response = await api.post(`/users/${id}/roles`, roleCodes);
    return response.data;
  },

  // Roles management
  getAllRoles: async () => {
    const response = await api.get('/roles');
    return response.data;
  },
  createRole: async (rolePayload) => {
    const response = await api.post('/roles', rolePayload);
    return response.data;
  },
  assignPermissionsToRole: async (id, permissionCodes) => {
    const response = await api.post(`/roles/${id}/permissions`, permissionCodes);
    return response.data;
  },

  // System Permissions
  getAllPermissions: async () => {
    const response = await api.get('/permissions');
    return response.data;
  },
  createPermission: async (permPayload) => {
    const response = await api.post('/permissions', permPayload);
    return response.data;
  },

  // Audit Logs
  getAllAuditLogs: async () => {
    const response = await api.get('/audit-logs');
    return response.data;
  },
  getAuditLogsByUser: async (userId) => {
    const response = await api.get(`/audit-logs/user/${userId}`);
    return response.data;
  },
  getAuditLogsByResource: async (resourceType, resourceId) => {
    const response = await api.get('/audit-logs/resource', {
      params: { resourceType, resourceId }
    });
    return response.data;
  }
};
