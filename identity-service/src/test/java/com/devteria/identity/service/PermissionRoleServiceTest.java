package com.devteria.identity.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.devteria.identity.dto.request.PermissionRequest;
import com.devteria.identity.dto.request.RoleRequest;
import com.devteria.identity.entity.Permission;
import com.devteria.identity.entity.Role;
import com.devteria.identity.repository.PermissionRepository;
import com.devteria.identity.repository.RoleRepository;

// Regression test cho lỗ hổng broken-access-control tìm được 2026-09-17 (audit toàn hệ thống,
// docs/api-catalog.md): PermissionService/RoleService trước đây không có @PreAuthorize nào,
// bất kỳ user đăng nhập nào (không cần ADMIN) đều tạo/xoá được Permission/Role. Khoá lại bằng
// @PreAuthorize("hasRole('ADMIN')") giống pattern UserService đã dùng (BE-15).
@SpringBootTest
@TestPropertySource("/test.properties")
class PermissionRoleServiceTest {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RoleService roleService;

    @MockitoBean
    private PermissionRepository permissionRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @Test
    @WithMockUser(roles = "USER")
    void permissionCreate_withoutAdminRole_accessDenied() {
        assertThrows(
                AccessDeniedException.class,
                () -> permissionService.create(
                        PermissionRequest.builder().name("BOOK_CREATE").build()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void permissionCreate_withAdminRole_success() {
        when(permissionRepository.save(any()))
                .thenReturn(Permission.builder().name("BOOK_CREATE").build());

        var response = permissionService.create(
                PermissionRequest.builder().name("BOOK_CREATE").build());

        Assertions.assertThat(response.getName()).isEqualTo("BOOK_CREATE");
    }

    @Test
    @WithMockUser(roles = "USER")
    void permissionGetAll_withoutAdminRole_accessDenied() {
        assertThrows(AccessDeniedException.class, () -> permissionService.getAll());
    }

    @Test
    @WithMockUser(roles = "USER")
    void permissionDelete_withoutAdminRole_accessDenied() {
        assertThrows(AccessDeniedException.class, () -> permissionService.delete("BOOK_CREATE"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void roleCreate_withoutAdminRole_accessDenied() {
        assertThrows(
                AccessDeniedException.class,
                () -> roleService.create(RoleRequest.builder()
                        .name("ADMIN")
                        .permissions(Set.of())
                        .build()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void roleCreate_withAdminRole_success() {
        when(permissionRepository.findAllById(any())).thenReturn(List.of());
        when(roleRepository.save(any())).thenReturn(Role.builder().name("ADMIN").build());

        var response = roleService.create(
                RoleRequest.builder().name("ADMIN").permissions(Set.of()).build());

        Assertions.assertThat(response.getName()).isEqualTo("ADMIN");
    }

    @Test
    @WithMockUser(roles = "USER")
    void roleGetAll_withoutAdminRole_accessDenied() {
        assertThrows(AccessDeniedException.class, () -> roleService.getAll());
    }

    @Test
    @WithMockUser(roles = "USER")
    void roleDelete_withoutAdminRole_accessDenied() {
        assertThrows(AccessDeniedException.class, () -> roleService.delete("ADMIN"));
    }
}
