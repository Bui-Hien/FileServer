import api from './api';

export const folderService = {
  createFolder: async (name, parentId = null) => {
    const response = await api.post('/folders', { name, parentId });
    return response.data;
  },
  getFolders: async () => {
    const response = await api.get('/folders');
    return response.data;
  },
  deleteFolder: async (id) => {
    await api.delete(`/folders/${id}`);
  },
  renameFolder: async (id, newName) => {
    const response = await api.put(`/folders/${id}/rename?name=${encodeURIComponent(newName)}`);
    return response.data;
  }
};
