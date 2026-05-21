import React, { useState, useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  Box, Grid, Paper, Typography, Breadcrumbs, Link, Button, Table, TableBody,
  TableCell, TableContainer, TableHead, TableRow, IconButton, Dialog, DialogTitle,
  DialogContent, DialogActions, TextField, MenuItem, Select, FormControl, InputLabel,
  Snackbar, Alert, CircularProgress, Chip, Switch, FormControlLabel, Tooltip
} from '@mui/material';
import FolderIcon from '@mui/icons-material/Folder';
import InsertDriveFileIcon from '@mui/icons-material/InsertDriveFile';
import CreateNewFolderIcon from '@mui/icons-material/CreateNewFolder';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import ShareIcon from '@mui/icons-material/Share';
import HistoryIcon from '@mui/icons-material/History';
import DownloadIcon from '@mui/icons-material/Download';
import HomeIcon from '@mui/icons-material/Home';
import AccountTreeIcon from '@mui/icons-material/AccountTree';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';

import { folderService } from '../services/folderService';
import { fileService } from '../services/fileService';
import { permissionService } from '../services/permissionService';
import { adminService } from '../services/adminService';
import { loginSuccess } from '../store/authSlice';
import { authService } from '../services/authService';

// Component Đệ quy vẽ từng Nhánh Thư Mục VÀ TỆP TIN trên cây thư mục bên trái (IDE-Style View Tree)
const FolderTreeNode = ({
  node,
  childrenMap,
  filesCacheMap,
  onFetchFolderFiles,
  currentId,
  onSelectFolder,
  onSelectFile,
  level = 0
}) => {
  const [expanded, setExpanded] = useState(false);
  const children = childrenMap[node.id] || [];
  const folderFiles = filesCacheMap[node.id] || [];
  const isSelected = currentId === node.id;

  const handleToggleExpand = async (e) => {
    e.stopPropagation();
    const newExpanded = !expanded;
    setExpanded(newExpanded);

    // Lazy load files inside this folder when expanded for optimal performance
    if (newExpanded) {
      await onFetchFolderFiles(node.id);
    }
  };

  const handleSelectFolder = () => {
    onSelectFolder(node.id);
  };

  return (
    <Box>
      {/* Folder Row */}
      <Box
        onClick={handleSelectFolder}
        sx={{
          display: 'flex',
          alignItems: 'center',
          pl: level * 1.5 + 1,
          py: 0.6,
          pr: 1,
          cursor: 'pointer',
          borderRadius: 2,
          bgcolor: isSelected ? 'rgba(37, 99, 235, 0.08)' : 'transparent',
          color: isSelected ? '#2563eb' : '#334155',
          fontWeight: isSelected ? 'bold' : 'normal',
          transition: 'all 0.15s',
          mb: 0.3,
          '&:hover': { bgcolor: 'rgba(0, 0, 0, 0.03)' }
        }}
      >
        <IconButton
          size="small"
          onClick={handleToggleExpand}
          sx={{
            p: 0.25,
            mr: 0.5,
            color: '#64748b'
          }}
        >
          {expanded ? <ExpandMoreIcon fontSize="small" /> : <ChevronRightIcon fontSize="small" />}
        </IconButton>
        <FolderIcon sx={{ color: '#f59e0b', mr: 1, fontSize: 18 }} />
        <Typography variant="body2" sx={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', fontSize: '0.85rem' }}>
          {node.name}
        </Typography>
      </Box>

      {/* Expanded items */}
      {expanded && (
        <Box sx={{ borderLeft: '1px dashed #cbd5e1', ml: level * 1.5 + 2.5 }}>
          {/* Subfolders */}
          {children.map(child => (
            <FolderTreeNode
              key={child.id}
              node={child}
              childrenMap={childrenMap}
              filesCacheMap={filesCacheMap}
              onFetchFolderFiles={onFetchFolderFiles}
              currentId={currentId}
              onSelectFolder={onSelectFolder}
              onSelectFile={onSelectFile}
              level={level + 1}
            />
          ))}

          {/* Files in this folder */}
          {folderFiles.map(file => (
            <Box
              key={file.id}
              onClick={(e) => {
                e.stopPropagation();
                onSelectFile(file);
              }}
              sx={{
                display: 'flex',
                alignItems: 'center',
                pl: (level + 1) * 1.5 + 2.5,
                py: 0.4,
                pr: 1,
                cursor: 'pointer',
                borderRadius: 2,
                color: '#64748b',
                mb: 0.2,
                transition: 'all 0.15s',
                '&:hover': { bgcolor: 'rgba(37, 99, 235, 0.05)', color: '#2563eb' }
              }}
            >
              <InsertDriveFileIcon sx={{ mr: 1, fontSize: 15, color: '#94a3b8' }} />
              <Typography variant="body2" sx={{ fontSize: '0.78rem', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {file.fileName}
              </Typography>
            </Box>
          ))}
        </Box>
      )}
    </Box>
  );
};

const Dashboard = () => {
  const dispatch = useDispatch();
  const currentUser = useSelector((state) => state.auth.user);

  // Thư mục hiện tại đang mở (null đại diện cho Root)
  const [currentFolderId, setCurrentFolderId] = useState(null);
  const [breadcrumbs, setBreadcrumbs] = useState([]);

  // Danh sách Folders & Files hiển thị bên phải
  const [folders, setFolders] = useState([]);
  const [files, setFiles] = useState([]);

  // Cấu trúc cây dữ liệu bên trái
  const [allFoldersList, setAllFoldersList] = useState([]);
  const [folderTreeRoot, setFolderTreeRoot] = useState([]);
  const [folderChildrenMap, setFolderChildrenMap] = useState({});
  const [allFoldersMap, setAllFoldersMap] = useState({});

  // Caching Files per folder mapping (folderId -> files list) to support File/Folder Tree
  const [filesCacheMap, setFilesCacheMap] = useState({});

  // Loading states
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);

  // Trạng thái thông báo (Toast Messages)
  const [toast, setToast] = useState({ open: false, message: '', severity: 'success' });

  // Dialog Tạo Thư mục mới
  const [folderDialogOpen, setFolderDialogOpen] = useState(false);
  const [newFolderName, setNewFolderName] = useState('');

  // Dialog Đổi tên thư mục
  const [renameDialogOpen, setRenameDialogOpen] = useState(false);
  const [renameFolderId, setRenameFolderId] = useState(null);
  const [renameFolderName, setRenameFolderName] = useState('');

  // Dialog Phiên bản (Version History)
  const [versionDialogOpen, setVersionDialogOpen] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [fileVersions, setFileVersions] = useState([]);

  // Dialog Chia sẻ (Share / ACL)
  const [shareDialogOpen, setShareDialogOpen] = useState(false);
  const [allUsersList, setAllUsersList] = useState([]);
  const [selectedItem, setSelectedItem] = useState(null);
  const [shareResourceType, setShareResourceType] = useState('FILE');
  const [activePermissions, setActivePermissions] = useState([]);
  const [loadingActivePerms, setLoadingActivePerms] = useState(false);
  const [sharePayload, setSharePayload] = useState({
    userIds: [],
    shareWithEveryone: false,
    permissionCode: 'FILE_READ',
    allow: true
  });

  useEffect(() => {
    fetchDirectoryData();
    fetchAllUsers();
  }, [currentFolderId]);

  // Quét danh sách người dùng để chia sẻ
  const fetchAllUsers = async () => {
    try {
      const users = await adminService.getAllUsers();
      setAllUsersList(users.filter(u => u.id !== currentUser.id));
    } catch (err) {
      console.error(err);
    }
  };

  // Nạp dữ liệu File và Folder
  const fetchDirectoryData = async () => {
    setLoading(true);
    try {
      // 1. Lấy tất cả thư mục hoạt động của User
      const allFolders = await folderService.getFolders();
      setAllFoldersList(allFolders);

      // Tạo map tra cứu nhanh
      const map = {};
      allFolders.forEach(f => { map[f.id] = f; });
      setAllFoldersMap(map);

      // Xây dựng cấu trúc cây thư mục (Children Map)
      const childrenMap = {};
      allFolders.forEach(f => { childrenMap[f.id] = []; });

      const roots = [];
      allFolders.forEach(f => {
        if (f.parentId) {
          if (!childrenMap[f.parentId]) {
            childrenMap[f.parentId] = [];
          }
          childrenMap[f.parentId].push(f);
        } else {
          roots.push(f);
        }
      });
      setFolderTreeRoot(roots);
      setFolderChildrenMap(childrenMap);

      // 2. Lọc các thư mục con hiển thị ở bảng bên phải
      const filteredFolders = allFolders.filter(f => {
        if (f.parentId === currentFolderId) return true;
        // Nếu đang ở Root (currentFolderId === null) và thư mục cha của f không truy cập được (không nằm trong allFolders)
        if (currentFolderId === null && f.parentId) {
          const parentAccessible = allFolders.some(p => p.id === f.parentId);
          return !parentAccessible;
        }
        return false;
      });
      setFolders(filteredFolders);

      // 3. Lấy danh sách tệp tin trong thư mục hiện tại hiển thị ở bảng bên phải
      const folderFiles = await fileService.getFilesByFolder(currentFolderId);
      setFiles(folderFiles);

      // Cập nhật caching cho thư mục hiện tại
      setFilesCacheMap(prev => ({ ...prev, [currentFolderId]: folderFiles }));

      // 4. Xây dựng Breadcrumbs
      buildBreadcrumbs(currentFolderId, map);
    } catch (err) {
      console.error(err);
      showToast('Lỗi tải tệp tin/thư mục. Có thể bạn không có quyền xem thư mục này!', 'error');
    } finally {
      setLoading(false);
    }
  };

  // Hàm Lazy load File khi bấm mở thư mục ở ViewTree bên trái
  const fetchFolderFilesForTree = async (folderId) => {
    if (filesCacheMap[folderId]) return; // Đã load rồi -> Skip để tối ưu hiệu năng
    try {
      const folderFiles = await fileService.getFilesByFolder(folderId);
      setFilesCacheMap(prev => ({ ...prev, [folderId]: folderFiles }));
    } catch (err) {
      console.error('Không thể tải tệp cho ViewTree: ' + folderId, err);
    }
  };

  const buildBreadcrumbs = (folderId, map) => {
    if (!folderId) {
      setBreadcrumbs([]);
      return;
    }
    const crumbs = [];
    let current = map[folderId];
    while (current) {
      crumbs.unshift({ id: current.id, name: current.name });
      current = current.parentId ? map[current.parentId] : null;
    }
    setBreadcrumbs(crumbs);
  };

  const showToast = (message, severity = 'success') => {
    setToast({ open: true, message, severity });
  };

  // Mở Dialog đổi tên
  const handleOpenRename = (id, currentName) => {
    const cleanName = currentName.startsWith("(Share) ") ? currentName.substring(8) : currentName;
    setRenameFolderId(id);
    setRenameFolderName(cleanName);
    setRenameDialogOpen(true);
  };

  // Thực hiện đổi tên thư mục
  const handleRenameFolder = async () => {
    if (!renameFolderName.trim()) return;
    try {
      await folderService.renameFolder(renameFolderId, renameFolderName.trim());
      showToast('Đổi tên thư mục thành công!');
      setRenameDialogOpen(false);
      fetchDirectoryData();
    } catch (err) {
      console.error(err);
      showToast(err.response?.data?.message || 'Đổi tên thư mục thất bại! Bạn không có quyền đổi tên thư mục này.', 'error');
    }
  };

  // Tạo thư mục mới
  const handleCreateFolder = async () => {
    if (!newFolderName.trim()) return;
    try {
      await folderService.createFolder(newFolderName.trim(), currentFolderId);
      showToast('Tạo thư mục con thành công!');
      setNewFolderName('');
      setFolderDialogOpen(false);
      fetchDirectoryData();
    } catch (err) {
      console.error(err);
      showToast('Tạo thư mục thất bại. Bạn không có quyền (FOLDER_CREATE)!', 'error');
    }
  };

  // Tải file lên
  const handleFileUpload = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    setUploading(true);
    const startUploadTime = Date.now();
    try {
      const response = await fileService.uploadFile(file, currentFolderId);
      const uploadDuration = Date.now() - startUploadTime;

      if (uploadDuration < 250) {
        showToast('⚡ [DEDUPLICATION] Tệp tin trùng khớp Checksum được lưu siêu tốc (tái sử dụng dữ liệu vật lý)!', 'success');
      } else {
        showToast('Tải tệp tin lên thành công!', 'success');
      }

      // Refresh lại hạn mức
      const freshUser = await authService.getMe();
      dispatch(loginSuccess(freshUser));

      fetchDirectoryData();
    } catch (err) {
      console.error(err);
      const errorMsg = err.response?.data?.message || err.message || '';
      if (errorMsg.includes('bị cấm')) {
        showToast('🛡️ [CẢNH BÁO BẢO MẬT] Định dạng tệp tin độc hại bị cấm để phòng chống tấn công RCE!', 'error');
      } else if (errorMsg.includes('hạn mức')) {
        showToast('⚠️ Bạn đã vượt quá hạn mức dung lượng cho phép!', 'warning');
      } else {
        showToast('Tải lên thất bại. Bạn không có quyền ghi file (FILE_WRITE) hoặc tệp tin không hợp lệ!', 'error');
      }
    } finally {
      setUploading(false);
    }
  };

  // Xóa thư mục
  const handleDeleteFolder = async (id, name) => {
    if (window.confirm(`Bạn có chắc chắn muốn xóa thư mục "${name}" và toàn bộ nội dung bên trong nó?`)) {
      try {
        await folderService.deleteFolder(id);
        showToast('Xóa thư mục thành công!');
        fetchDirectoryData();
      } catch (err) {
        showToast('Xóa thư mục thất bại. Bạn không có quyền (FOLDER_DELETE)!', 'error');
      }
    }
  };

  // Xóa tệp tin
  const handleDeleteFile = async (id, name) => {
    if (window.confirm(`Bạn có chắc chắn muốn xóa tệp tin "${name}"?`)) {
      try {
        await fileService.deleteFile(id);
        showToast('Xóa tệp tin thành công!');

        const freshUser = await authService.getMe();
        dispatch(loginSuccess(freshUser));

        fetchDirectoryData();
      } catch (err) {
        showToast('Xóa tệp tin thất bại. Bạn không có quyền (FILE_DELETE)!', 'error');
      }
    }
  };

  // Tải xuống tệp tin an toàn qua Axios (có truyền Authorization header)
  const handleDownloadFile = async (id, fileName) => {
    try {
      showToast(`Đang chuẩn bị tải xuống: ${fileName}...`, 'info');
      await fileService.downloadFile(id, fileName);
    } catch (err) {
      console.error(err);
      showToast('Tải xuống tệp tin thất bại! Bạn không có quyền truy cập hoặc phiên làm việc đã hết hạn.', 'error');
    }
  };

  // Tải xuống phiên bản lịch sử tệp tin an toàn
  const handleDownloadFileVersion = async (versionId, originalName, version) => {
    try {
      const baseName = originalName.includes(".") ? originalName.substring(0, originalName.lastIndexOf(".")) : originalName;
      const ext = originalName.includes(".") ? originalName.substring(originalName.lastIndexOf(".")) : "";
      const downloadName = `${baseName}_v${version}${ext}`;

      showToast(`Đang tải xuống phiên bản v${version}...`, 'info');
      await fileService.downloadFileVersion(versionId, downloadName);
    } catch (err) {
      console.error(err);
      showToast('Tải xuống phiên bản lịch sử thất bại!', 'error');
    }
  };

  // Xem lịch sử phiên bản tệp tin
  const handleViewVersions = async (file) => {
    setSelectedFile(file);
    try {
      const versions = await fileService.getFileVersions(file.id);
      setFileVersions(versions);
      setVersionDialogOpen(true);
    } catch (err) {
      showToast('Không thể tải lịch sử phiên bản của tệp tin này!', 'error');
    }
  };

  // Mở Dialog chia sẻ
  const handleOpenShare = async (item, type = 'FILE') => {
    setSelectedItem(item);
    setShareResourceType(type);
    setSharePayload({
      userIds: [],
      shareWithEveryone: false,
      permissionCode: 'FILE_READ',
      allow: true
    });
    setShareDialogOpen(true);

    // Tải danh sách phân quyền hiện có của tài nguyên
    setLoadingActivePerms(true);
    try {
      const perms = await permissionService.getActivePermissions(item.id, type);
      setActivePermissions(perms);
    } catch (err) {
      console.error(err);
      showToast('Không thể tải danh sách phân quyền hiện tại!', 'error');
    } finally {
      setLoadingActivePerms(false);
    }
  };

  // Thu hồi phân quyền (Revoke ACL)
  const handleRevokePermission = async (permId) => {
    if (window.confirm('Bạn có chắc chắn muốn thu hồi quyền truy cập này không?')) {
      try {
        await permissionService.deletePermission(permId);
        showToast('Thu hồi quyền truy cập thành công!');

        // Cập nhật lại bảng danh sách
        const perms = await permissionService.getActivePermissions(selectedItem.id, shareResourceType);
        setActivePermissions(perms);

        fetchDirectoryData();
      } catch (err) {
        showToast('Thu hồi thất bại! Chỉ admin hoặc chủ sở hữu mới có quyền thu hồi.', 'error');
      }
    }
  };

  // Quay lại thư mục cha
  const handleGoToParent = () => {
    if (breadcrumbs.length <= 1) {
      setCurrentFolderId(null);
    } else {
      const parentCrumb = breadcrumbs[breadcrumbs.length - 2];
      setCurrentFolderId(parentCrumb.id);
    }
  };

  // Thực hiện chia sẻ
  const handleShare = async () => {
    try {
      await permissionService.shareResource(selectedItem.id, {
        resourceType: shareResourceType,
        userIds: sharePayload.userIds,
        shareWithEveryone: sharePayload.shareWithEveryone,
        permissionCode: sharePayload.permissionCode,
        allow: sharePayload.allow
      });
      showToast(`Chia sẻ ${shareResourceType === 'FILE' ? 'tệp tin' : 'thư mục'} thành công!`);

      // Cập nhật lại bảng danh sách ngay trên Dialog
      const perms = await permissionService.getActivePermissions(selectedItem.id, shareResourceType);
      setActivePermissions(perms);

      // Reset input fields
      setSharePayload({
        userIds: [],
        shareWithEveryone: false,
        permissionCode: 'FILE_READ',
        allow: true
      });

      fetchDirectoryData(); // Làm mới dữ liệu sau khi chia sẻ
    } catch (err) {
      showToast(`Chia sẻ thất bại! Chỉ admin hoặc chủ sở hữu mới được quyền chia sẻ ${shareResourceType === 'FILE' ? 'tệp tin' : 'thư mục'} này.`, 'error');
    }
  };

  // Xem nhanh thông tin tệp tin khi Click chọn tệp trên ViewTree bên trái
  const handleSelectFileFromTree = (file) => {
    setSelectedFile(file);
    showToast(`Đã chọn tệp tin: ${file.fileName}. Kích thước: ${formatSize(file.size)}`, 'info');
  };

  const formatSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  return (
    <Box>
      <Grid container spacing={3}>

        {/* COLUMN BÊN TRÁI: CÂY PHÂN CẤP THƯ MỤC VÀ TỆP TIN (IDE-Style View Tree) */}
        <Grid item xs={12} md={3}>
          <Paper sx={{ p: 2.5, borderRadius: 3, minHeight: '75vh', border: '1px solid #e2e8f0', boxShadow: 'none' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 2, pb: 1.5, borderBottom: '1px solid #e2e8f0' }}>
              <AccountTreeIcon sx={{ color: '#2563eb', mr: 1, fontSize: 22 }} />
              <Typography variant="h6" fontWeight="bold" color="#0f172a">
                IDE ViewTree
              </Typography>
            </Box>

            {/* Nút điều hướng về Thư mục Gốc (Root) */}
            <Box
              onClick={() => setCurrentFolderId(null)}
              sx={{
                display: 'flex',
                alignItems: 'center',
                py: 0.8,
                px: 1.5,
                mb: 2,
                cursor: 'pointer',
                borderRadius: 2,
                bgcolor: currentFolderId === null ? 'rgba(37, 99, 235, 0.08)' : 'transparent',
                color: currentFolderId === null ? '#2563eb' : '#475569',
                fontWeight: currentFolderId === null ? 'bold' : 'normal',
                '&:hover': { bgcolor: 'rgba(0, 0, 0, 0.03)' }
              }}
            >
              <HomeIcon sx={{ mr: 1.5, fontSize: 18, color: currentFolderId === null ? '#2563eb' : '#64748b' }} />
              <Typography variant="body2" sx={{ fontSize: '0.85rem' }}>Thư mục gốc (Root)</Typography>
            </Box>

            {/* Danh sách các cây thư mục & tệp tin đệ quy */}
            <Box sx={{ maxHeight: '60vh', overflowY: 'auto' }}>
              {/* Vẽ nhánh đệ quy */}
              {folderTreeRoot.map(node => (
                <FolderTreeNode
                  key={node.id}
                  node={node}
                  childrenMap={folderChildrenMap}
                  filesCacheMap={filesCacheMap}
                  onFetchFolderFiles={fetchFolderFilesForTree}
                  currentId={currentFolderId}
                  onSelectFolder={setCurrentFolderId}
                  onSelectFile={handleSelectFileFromTree}
                />
              ))}

              {/* Hiển thị danh sách file thuộc Root ở ngay dưới nhánh Gốc của ViewTree */}
              {filesCacheMap[null]?.map(file => (
                <Box
                  key={file.id}
                  onClick={(e) => {
                    e.stopPropagation();
                    handleSelectFileFromTree(file);
                  }}
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    pl: 2.5,
                    py: 0.4,
                    pr: 1,
                    cursor: 'pointer',
                    borderRadius: 2,
                    color: '#64748b',
                    mb: 0.2,
                    transition: 'all 0.15s',
                    '&:hover': { bgcolor: 'rgba(37, 99, 235, 0.05)', color: '#2563eb' }
                  }}
                >
                  <InsertDriveFileIcon sx={{ mr: 1, fontSize: 15, color: '#94a3b8' }} />
                  <Typography variant="body2" sx={{ fontSize: '0.78rem', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {file.fileName}
                  </Typography>
                </Box>
              ))}

              {folderTreeRoot.length === 0 && (!filesCacheMap[null] || filesCacheMap[null].length === 0) && (
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 4, textAlign: 'center' }}>
                  Chưa có dữ liệu.
                </Typography>
              )}
            </Box>
          </Paper>
        </Grid>

        {/* COLUMN BÊN PHẢI: CHI TIẾT BẢNG FILES & ACTIONS */}
        <Grid item xs={12} md={9}>
          {/* Breadcrumbs & Actions Toolbar */}
          <Paper sx={{ p: 2, mb: 3, borderRadius: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 2, border: '1px solid #e2e8f0', boxShadow: 'none' }}>
            <Breadcrumbs aria-label="breadcrumb">
              <Link
                underline="hover"
                sx={{ display: 'flex', alignItems: 'center', cursor: 'pointer', color: '#475569', fontWeight: 'medium' }}
                onClick={() => setCurrentFolderId(null)}
              >
                <HomeIcon sx={{ mr: 0.5, fontSize: 18 }} />
                Home
              </Link>
              {breadcrumbs.map((crumb) => (
                <Link
                  key={crumb.id}
                  underline="hover"
                  sx={{ cursor: 'pointer', color: '#475569', fontWeight: 'medium' }}
                  onClick={() => setCurrentFolderId(crumb.id)}
                >
                  {crumb.name}
                </Link>
              ))}
            </Breadcrumbs>

            <Box sx={{ display: 'flex', gap: 1.5 }}>
              <Button
                variant="outlined"
                disabled={currentFolderId !== null && currentFolderId < 0}
                startIcon={<CreateNewFolderIcon />}
                onClick={() => setFolderDialogOpen(true)}
                sx={{ textTransform: 'none', borderRadius: 2, fontWeight: 'bold' }}
              >
                Thư mục mới
              </Button>

              <Button
                component="label"
                variant="contained"
                disabled={uploading || (currentFolderId !== null && currentFolderId < 0)}
                startIcon={uploading ? <CircularProgress size={20} color="inherit" /> : <CloudUploadIcon />}
                sx={{ textTransform: 'none', borderRadius: 2, fontWeight: 'bold' }}
              >
                {uploading ? 'Đang tải lên...' : 'Tải file lên'}
                <input type="file" hidden onChange={handleFileUpload} />
              </Button>
            </Box>
          </Paper>

          {/* Files Explorer Table */}
          <TableContainer component={Paper} sx={{ borderRadius: 3, overflow: 'hidden', border: '1px solid #e2e8f0', boxShadow: 'none' }}>
            {loading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 300 }}>
                <CircularProgress />
              </Box>
            ) : (
              <Table>
                <TableHead sx={{ bgcolor: '#f8fafc' }}>
                  <TableRow>
                    <TableCell><Typography fontWeight="bold" color="#475569" variant="body2">Tên tài nguyên</Typography></TableCell>
                    <TableCell><Typography fontWeight="bold" color="#475569" variant="body2">Phiên bản</Typography></TableCell>
                    <TableCell><Typography fontWeight="bold" color="#475569" variant="body2">Kích thước</Typography></TableCell>
                    <TableCell><Typography fontWeight="bold" color="#475569" variant="body2">Người sở hữu</Typography></TableCell>
                    <TableCell><Typography fontWeight="bold" color="#475569" variant="body2">Mã checksum MD5</Typography></TableCell>
                    <TableCell align="right"><Typography fontWeight="bold" color="#475569" variant="body2">Hành động</Typography></TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {/* Quay lại thư mục cha (..) */}
                  {currentFolderId !== null && (
                    <TableRow
                      hover
                      sx={{ cursor: 'pointer', bgcolor: '#f8fafc' }}
                      onClick={handleGoToParent}
                    >
                      <TableCell sx={{ display: 'flex', alignItems: 'center', gap: 1.5, py: 1.5 }}>
                        <FolderIcon sx={{ color: '#64748b' }} />
                        <Typography fontWeight="semibold" color="#475569" variant="body2">
                          .. (Quay lại thư mục cha)
                        </Typography>
                      </TableCell>
                      <TableCell>-</TableCell>
                      <TableCell>-</TableCell>
                      <TableCell>-</TableCell>
                      <TableCell>-</TableCell>
                      <TableCell align="right">-</TableCell>
                    </TableRow>
                  )}

                  {/* Folders */}
                  {folders.map((folder) => (
                    <TableRow
                      key={folder.id}
                      hover
                      sx={{ cursor: 'pointer' }}
                      onClick={() => setCurrentFolderId(folder.id)}
                    >
                      <TableCell sx={{ display: 'flex', alignItems: 'center', gap: 1.5, py: 1.8 }}>
                        <FolderIcon sx={{ color: '#f59e0b' }} />
                        <Typography fontWeight="semibold" color="#0f172a" variant="body2">
                          {folder.name}
                        </Typography>
                      </TableCell>
                      <TableCell>-</TableCell>
                      <TableCell>-</TableCell>
                      <TableCell>{folder.ownerName || 'Hệ thống'}</TableCell>
                      <TableCell>-</TableCell>
                      <TableCell align="right" onClick={(e) => e.stopPropagation()}>
                        <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 0.5 }}>
                          {folder.id > 0 && (
                            <Tooltip title="Đổi tên thư mục">
                              <IconButton onClick={() => handleOpenRename(folder.id, folder.name)} color="primary" size="small">
                                <EditIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}

                          <Tooltip title="Chia sẻ thư mục">
                            <IconButton onClick={() => handleOpenShare(folder, 'FOLDER')} color="success" size="small">
                              <ShareIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>

                          <Tooltip title="Xóa thư mục">
                            <IconButton onClick={() => handleDeleteFolder(folder.id, folder.name)} color="error" size="small">
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </Box>
                      </TableCell>
                    </TableRow>
                  ))}

                  {/* Files */}
                  {files.map((file) => (
                    <TableRow
                      key={file.id}
                      hover
                      sx={{ cursor: 'pointer' }}
                      onClick={() => handleDownloadFile(file.id, file.fileName)}
                    >
                      <TableCell sx={{ py: 1.8 }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                          <InsertDriveFileIcon sx={{ color: '#2563eb' }} />
                          <Typography fontWeight="semibold" color="#0f172a" variant="body2">
                            {file.fileName}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip label={`v${file.version}`} size="small" color="primary" variant="outlined" sx={{ fontWeight: 'bold', height: 20, fontSize: '0.7rem' }} />
                      </TableCell>
                      <TableCell>{formatSize(file.size)}</TableCell>
                      <TableCell>{file.ownerName || 'Hệ thống'}</TableCell>
                      <TableCell>
                        <Typography variant="body2" color="text.secondary" fontFamily="monospace" sx={{ fontSize: '0.8rem' }}>
                          {file.checksum ? file.checksum.substring(0, 12) + '...' : '-'}
                        </Typography>
                      </TableCell>
                      <TableCell align="right" onClick={(e) => e.stopPropagation()}>
                        <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 0.5 }}>
                          <Tooltip title="Tải xuống">
                            <IconButton
                              onClick={() => handleDownloadFile(file.id, file.fileName)}
                              color="primary"
                              size="small"
                            >
                              <DownloadIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>

                          <Tooltip title="Lịch sử phiên bản">
                            <IconButton onClick={() => handleViewVersions(file)} color="info" size="small">
                              <HistoryIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>

                          <Tooltip title="Chia sẻ">
                            <IconButton onClick={() => handleOpenShare(file, 'FILE')} color="success" size="small">
                              <ShareIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>

                          <Tooltip title="Xóa tệp">
                            <IconButton onClick={() => handleDeleteFile(file.id, file.fileName)} color="error" size="small">
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </Box>
                      </TableCell>
                    </TableRow>
                  ))}

                  {folders.length === 0 && files.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={6} align="center" sx={{ py: 8 }}>
                        <Typography color="text.secondary" variant="body2">
                          Thư mục trống. Hãy tạo thư mục con hoặc tải tệp tin lên!
                        </Typography>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            )}
          </TableContainer>
        </Grid>
      </Grid>

      {/* Dialog Tạo Thư Mục Mới */}
      <Dialog open={folderDialogOpen} onClose={() => setFolderDialogOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle sx={{ fontWeight: 'bold' }}>Tạo thư mục mới</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Tên thư mục"
            fullWidth
            variant="outlined"
            value={newFolderName}
            onChange={(e) => setNewFolderName(e.target.value)}
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setFolderDialogOpen(false)} color="inherit">Hủy bỏ</Button>
          <Button onClick={handleCreateFolder} variant="contained" disabled={!newFolderName.trim()}>Tạo mới</Button>
        </DialogActions>
      </Dialog>

      {/* Dialog Đổi Tên Thư Mục */}
      <Dialog open={renameDialogOpen} onClose={() => setRenameDialogOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle sx={{ fontWeight: 'bold' }}>Đổi tên thư mục</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Tên thư mục mới"
            fullWidth
            variant="outlined"
            value={renameFolderName}
            onChange={(e) => setRenameFolderName(e.target.value)}
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setRenameDialogOpen(false)} color="inherit">Hủy bỏ</Button>
          <Button onClick={handleRenameFolder} variant="contained" disabled={!renameFolderName.trim()}>Lưu thay đổi</Button>
        </DialogActions>
      </Dialog>

      {/* Dialog Lịch Sử Phiên Bản (Versioning) */}
      <Dialog open={versionDialogOpen} onClose={() => setVersionDialogOpen(false)} fullWidth maxWidth="md">
        <DialogTitle sx={{ fontWeight: 'bold' }}>
          Lịch sử phiên bản: {selectedFile?.fileName}
        </DialogTitle>
        <DialogContent>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Số hiệu</TableCell>
                  <TableCell>Kích thước</TableCell>
                  <TableCell>Mã băm Checksum (MD5)</TableCell>
                  <TableCell>Thời gian tạo</TableCell>
                  <TableCell align="right">Hành động</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {fileVersions.map((ver) => (
                  <TableRow key={ver.id}>
                    <TableCell>
                      <Chip label={`v${ver.version}`} size="small" variant="outlined" />
                    </TableCell>
                    <TableCell>{formatSize(ver.size)}</TableCell>
                    <TableCell fontFamily="monospace" sx={{ fontSize: '0.85rem' }}>{ver.checksum}</TableCell>
                    <TableCell>{new Date(ver.createdAt || Date.now()).toLocaleString()}</TableCell>
                    <TableCell align="right">
                      <Box sx={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: 1.5 }}>
                        <Chip label="Đã lưu lịch sử" size="small" variant="outlined" sx={{ color: 'text.secondary' }} />
                        <Button
                          size="small"
                          color="info"
                          variant="outlined"
                          startIcon={<DownloadIcon />}
                          onClick={() => handleDownloadFileVersion(ver.id, selectedFile.fileName, ver.version)}
                          sx={{ textTransform: 'none', borderRadius: 2 }}
                        >
                          Tải xuống
                        </Button>
                      </Box>
                    </TableCell>
                  </TableRow>
                ))}
                {selectedFile && (
                  <TableRow selected>
                    <TableCell>
                      <Chip label={`v${selectedFile.version}`} size="small" color="primary" />
                    </TableCell>
                    <TableCell>{formatSize(selectedFile.size)}</TableCell>
                    <TableCell fontFamily="monospace" sx={{ fontSize: '0.85rem' }}>{selectedFile.checksum}</TableCell>
                    <TableCell>Hiện tại</TableCell>
                    <TableCell align="right">
                      <Button
                        size="small"
                        color="primary"
                        variant="contained"
                        startIcon={<DownloadIcon />}
                        onClick={() => handleDownloadFile(selectedFile.id, selectedFile.fileName)}
                        sx={{ textTransform: 'none', borderRadius: 2 }}
                      >
                        Tải xuống
                      </Button>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setVersionDialogOpen(false)}>Đóng</Button>
        </DialogActions>
      </Dialog>

      {/* Dialog Chia sẻ Tệp tin / Thư mục */}
      <Dialog open={shareDialogOpen} onClose={() => setShareDialogOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle sx={{ fontWeight: 'bold' }}>
          Chia sẻ {shareResourceType === 'FILE' ? 'tệp tin' : 'thư mục'}: {selectedItem?.fileName || selectedItem?.name}
        </DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3, mt: 2 }}>
            <FormControlLabel
              control={
                <Switch
                  checked={sharePayload.shareWithEveryone}
                  onChange={(e) => setSharePayload({ ...sharePayload, shareWithEveryone: e.target.checked })}
                />
              }
              label={
                <Box>
                  <Typography fontWeight="bold" variant="body2">Chia sẻ công cộng (Everyone Share)</Typography>
                  <Typography variant="caption" color="text.secondary">Cho phép toàn bộ tài khoản trong hệ thống truy cập {shareResourceType === 'FILE' ? 'tệp tin' : 'thư mục'}</Typography>
                </Box>
              }
            />

            {!sharePayload.shareWithEveryone && (
              <FormControl fullWidth>
                <InputLabel id="users-share-label">Chọn người nhận chia sẻ</InputLabel>
                <Select
                  labelId="users-share-label"
                  multiple
                  value={sharePayload.userIds}
                  onChange={(e) => setSharePayload({ ...sharePayload, userIds: e.target.value })}
                  label="Chọn người nhận chia sẻ"
                  renderValue={(selected) => (
                    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                      {selected.map((value) => {
                        const usr = allUsersList.find(u => u.id === value);
                        return <Chip key={value} label={usr ? usr.username : value} size="small" />;
                      })}
                    </Box>
                  )}
                >
                  {allUsersList.map((user) => (
                    <MenuItem key={user.id} value={user.id}>
                      {user.username} ({user.fullName})
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            )}

            <FormControl fullWidth>
              <InputLabel id="perm-code-label">Quyền truy cập</InputLabel>
              <Select
                labelId="perm-code-label"
                value={sharePayload.permissionCode}
                onChange={(e) => setSharePayload({ ...sharePayload, permissionCode: e.target.value })}
                label="Quyền truy cập"
              >
                {shareResourceType === 'FILE' ? [
                  <MenuItem key="FILE_READ" value="FILE_READ">FILE_READ (Chỉ xem và tải xuống)</MenuItem>,
                  <MenuItem key="FILE_WRITE" value="FILE_WRITE">FILE_WRITE (Cho phép cập nhật phiên bản)</MenuItem>
                ] : [
                  <MenuItem key="FILE_READ" value="FILE_READ">FILE_READ (Xem nội dung thư mục)</MenuItem>,
                  <MenuItem key="FILE_WRITE" value="FILE_WRITE">FILE_WRITE (Tải tệp tin lên thư mục)</MenuItem>,
                  <MenuItem key="FOLDER_CREATE" value="FOLDER_CREATE">FOLDER_CREATE (Tạo thư mục con bên trong)</MenuItem>,
                  <MenuItem key="FOLDER_DELETE" value="FOLDER_DELETE">FOLDER_DELETE (Xóa thư mục con bên trong)</MenuItem>
                ]}
              </Select>
            </FormControl>

            <FormControl fullWidth>
              <InputLabel id="allow-deny-label">Hành động</InputLabel>
              <Select
                labelId="allow-deny-label"
                value={sharePayload.allow}
                onChange={(e) => setSharePayload({ ...sharePayload, allow: e.target.value })}
                label="Hành động"
              >
                <MenuItem value={true}>CHO PHÉP (Allow Access)</MenuItem>
                <MenuItem value={false}>CHẶN TRỰC TIẾP (Deny Override)</MenuItem>
              </Select>
            </FormControl>

            {/* Danh sách phân quyền hiện tại của tài nguyên */}
            <Typography variant="subtitle2" fontWeight="bold" color="text.secondary" sx={{ mt: 3, borderTop: '1px solid #e2e8f0', pt: 2, mb: 1 }}>
              Danh sách tài khoản được chia sẻ hiện tại ({activePermissions.length})
            </Typography>

            {loadingActivePerms ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', py: 2 }}>
                <CircularProgress size={24} />
              </Box>
            ) : activePermissions.length === 0 ? (
              <Typography variant="body2" color="text.secondary" sx={{ fontStyle: 'italic', textAlign: 'center', py: 2 }}>
                Chưa chia sẻ cho ai. Tài nguyên này hiện đang riêng tư.
              </Typography>
            ) : (
              <TableContainer component={Paper} sx={{ maxHeight: 200, overflowY: 'auto', border: '1px solid #e2e8f0', boxShadow: 'none', borderRadius: 2 }}>
                <Table size="small">
                  <TableHead sx={{ bgcolor: '#f8fafc' }}>
                    <TableRow>
                      <TableCell sx={{ py: 1 }}><Typography fontWeight="bold" variant="caption" color="text.secondary">Người nhận</Typography></TableCell>
                      <TableCell sx={{ py: 1 }}><Typography fontWeight="bold" variant="caption" color="text.secondary">Quyền</Typography></TableCell>
                      <TableCell sx={{ py: 1 }}><Typography fontWeight="bold" variant="caption" color="text.secondary">Hành động</Typography></TableCell>
                      <TableCell sx={{ py: 1 }} align="right"><Typography fontWeight="bold" variant="caption" color="text.secondary">Thu hồi</Typography></TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {activePermissions.map((perm) => (
                      <TableRow key={perm.id} hover>
                        <TableCell sx={{ py: 0.75 }}>
                          <Typography variant="body2" fontWeight="medium" color="#334155">
                            {perm.userId === null ? '🌏 MỌI NGƯỜI (Everyone)' : `👤 ${perm.username}`}
                          </Typography>
                        </TableCell>
                        <TableCell sx={{ py: 0.75 }}>
                          <Chip label={perm.permissionCode} size="small" variant="outlined" sx={{ height: 18, fontSize: '0.65rem', fontWeight: 'bold' }} />
                        </TableCell>
                        <TableCell sx={{ py: 0.75 }}>
                          <Chip
                            label={perm.allow ? 'CHO PHÉP' : 'CHẶN'}
                            color={perm.allow ? 'success' : 'error'}
                            size="small"
                            sx={{ height: 18, fontSize: '0.65rem', fontWeight: 'bold', color: '#fff' }}
                          />
                        </TableCell>
                        <TableCell sx={{ py: 0.75 }} align="right">
                          <IconButton onClick={() => handleRevokePermission(perm.id)} color="error" size="small" sx={{ p: 0.25 }}>
                            <DeleteIcon fontSize="inherit" />
                          </IconButton>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </Box>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setShareDialogOpen(false)} color="inherit">Đóng lại</Button>
          <Button onClick={handleShare} variant="contained" color="success">Chia sẻ mới</Button>
        </DialogActions>
      </Dialog>

      {/* Snackbar alerts */}
      <Snackbar
        open={toast.open}
        autoHideDuration={6000}
        onClose={() => setToast({ ...toast, open: false })}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
      >
        <Alert
          onClose={() => setToast({ ...toast, open: false })}
          severity={toast.severity}
          sx={{ width: '100%', borderRadius: 2, fontWeight: 'bold' }}
        >
          {toast.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default Dashboard;
