package com.buihien.fileserver.file;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {
    List<FileEntity> findByFolderIdAndIsDeletedFalse(Long folderId);
    List<FileEntity> findByOwnerIdAndIsDeletedFalse(Long ownerId);
    Optional<FileEntity> findByIdAndIsDeletedFalse(Long id);
    Optional<FileEntity> findFirstByChecksumAndIsDeletedFalse(String checksum);
}
