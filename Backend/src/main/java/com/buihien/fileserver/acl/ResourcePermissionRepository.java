package com.buihien.fileserver.acl;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourcePermissionRepository extends JpaRepository<ResourcePermission, Long> {
    List<ResourcePermission> findByResourceTypeAndResourceId(String resourceType, Long resourceId);
    List<ResourcePermission> findByResourceTypeAndResourceIdAndUserId(String resourceType, Long resourceId, Long userId);
    List<ResourcePermission> findByUserId(Long userId);
    List<ResourcePermission> findByUserNull();
    List<ResourcePermission> findByResourceTypeAndUserId(String resourceType, Long userId);
    List<ResourcePermission> findByResourceTypeAndUserNull(String resourceType);
    Optional<ResourcePermission> findByResourceTypeAndResourceIdAndUserIdAndPermissionCode(
            String resourceType, Long resourceId, Long userId, String permissionCode);
    Optional<ResourcePermission> findByResourceTypeAndResourceIdAndUserNullAndPermissionCode(
            String resourceType, Long resourceId, String permissionCode);
}
