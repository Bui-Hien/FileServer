import api from './api';

export const permissionService = {
  assignPermission: async (resourceId, payload) => {
    const response = await api.post(`/resources/${resourceId}/permissions`, payload);
    return response.data;
  },
  shareResource: async (resourceId, payload) => {
    const response = await api.post(`/resources/${resourceId}/share`, payload);
    return response.data;
  },
  checkPermission: async (resourceId, params) => {
    const response = await api.get(`/resources/${resourceId}/permissions/check`, { params });
    return response.data;
  },
  getActivePermissions: async (resourceId, resourceType) => {
    const response = await api.get(`/resources/${resourceId}/permissions`, { params: { resourceType } });
    return response.data;
  },
  deletePermission: async (permissionId) => {
    const response = await api.delete(`/resources/permissions/${permissionId}`);
    return response.data;
  }
};
