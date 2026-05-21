import api from './api';

export const fileService = {
  uploadFile: async (file, folderId = null) => {
    const formData = new FormData();
    formData.append('file', file);
    if (folderId) {
      formData.append('folderId', folderId);
    }
    const response = await api.post('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },
  uploadFileChunk: async (chunk, uploadId, chunkIndex, totalChunks, fileName, folderId = null) => {
    const formData = new FormData();
    formData.append('file', chunk);
    formData.append('uploadId', uploadId);
    formData.append('chunkIndex', chunkIndex);
    formData.append('totalChunks', totalChunks);
    formData.append('fileName', fileName);
    if (folderId) {
      formData.append('folderId', folderId);
    }
    const response = await api.post('/files/upload-chunk', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },
  getFilesByFolder: async (folderId = null) => {
    const response = await api.get('/files', {
      params: folderId ? { folderId } : {},
    });
    return response.data;
  },
  getFileVersions: async (id) => {
    const response = await api.get(`/files/${id}/versions`);
    return response.data;
  },
  deleteFile: async (id) => {
    await api.delete(`/files/${id}`);
  },
  getDownloadUrl: (id) => {
    return `http://localhost:8080/api/files/${id}/download`;
  },
  getDownloadVersionUrl: (versionId) => {
    return `http://localhost:8080/api/files/versions/${versionId}/download`;
  },
  downloadFile: async (id, fileName) => {
    const response = await api.get(`/files/${id}/download`, {
      responseType: 'blob'
    });
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', fileName);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },
  downloadFileVersion: async (versionId, fileName) => {
    const response = await api.get(`/files/versions/${versionId}/download`, {
      responseType: 'blob'
    });
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', fileName);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  }
};
