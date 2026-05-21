package com.buihien.fileserver.folder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    List<Folder> findByOwnerIdAndIsDeletedFalse(Long ownerId);
    List<Folder> findByParentIdAndIsDeletedFalse(Long parentId);
    List<Folder> findByPathStartingWithAndIsDeletedFalse(String pathPrefix);
}
