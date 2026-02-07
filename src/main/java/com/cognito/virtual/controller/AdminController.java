package com.cognito.virtual.controller;

import com.cognito.virtual.dto.*;
import com.cognito.virtual.entity.*;
import com.cognito.virtual.mapper.FunctionsMapper;
import com.cognito.virtual.mapper.PermissionDTO;
import com.cognito.virtual.mapper.RoleDTO;
import com.cognito.virtual.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final FunctionsMapper utilsMapper;
    private final com.cognito.virtual.service.CognitoService cognitoService;
    private final FunctionsMapper functionsMapper;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserInfo>>> getAllUsers() {
        List<UserEntity> users = userRepository.findAll();

        List<UserInfo> userInfos = users.stream()
                .map(this::buildUserInfo)
                .collect(toList());

        return ResponseEntity.ok(
                ApiResponse.success("Usuarios obtenidos exitosamente", userInfos));
    }

    @GetMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<RoleInfo>>> getUserRoles(@PathVariable Long userId) {
        try {
            Optional<UserEntity> userOptional = userRepository.findById(userId);

            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Usuario no encontrado"));
            }

            UserEntity user = userOptional.get();

            List<RoleInfo> userRoles = user.getRoles().stream()
                    .map(this::buildRoleInfo)
                    .collect(toList());

            return ResponseEntity.ok(
                    ApiResponse.success("Roles del usuario obtenidos exitosamente", userRoles));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error interno del servidor: " + e.getMessage()));
        }
    }

    @PostMapping("/users/{userId}/roles/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> assignRoleToUser(
            @PathVariable Long userId,
            @PathVariable Long roleId) {

        Optional<UserEntity> userOpt = userRepository.findById(userId);
        Optional<RoleEntity> roleOpt = roleRepository.findById(roleId);

        if (userOpt.isEmpty() || roleOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Usuario o rol no encontrado"));
        }

        if (!userOpt.get().getRoles().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El usuario ya tiene asignado un rol"));
        }

        UserEntity user = userOpt.get();
        RoleEntity role = roleOpt.get();

        user.getRoles().add(role);
        userRepository.save(user);

        return ResponseEntity.ok(
                ApiResponse.success("Rol asignado exitosamente"));
    }

    @DeleteMapping("/users/{userId}/roles/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> removeRoleFromUser(
            @PathVariable Long userId,
            @PathVariable Long roleId) {

        Optional<UserEntity> userOpt = userRepository.findById(userId);
        Optional<RoleEntity> roleOpt = roleRepository.findById(roleId);

        if (userOpt.isEmpty() || roleOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Usuario o rol no encontrado"));
        }

        UserEntity user = userOpt.get();
        RoleEntity role = roleOpt.get();

        user.getRoles().remove(role);
        userRepository.save(user);

        return ResponseEntity.ok(
                ApiResponse.success("Rol removido exitosamente"));
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<RoleDTO>>> getAllRoles() {
        List<RoleDTO> roles = roleRepository.findAll().stream()
                .map(utilsMapper::mapToDTO)
                .toList();
        return ResponseEntity.ok(
                ApiResponse.success("Roles obtenidos exitosamente", roles));
    }

    @GetMapping("/permisos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PermissionDTO>>> getAllPermisos() {
        List<PermissionDTO> permisos = permissionRepository.findAll().stream()
                .map(utilsMapper::mapToDTO)
                .toList();
        return ResponseEntity.ok(
                ApiResponse.success("Permisos obtenidos exitosamente", permisos));
    }


    @PostMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleEntity>> createRole(
            @RequestBody RoleRequest request) {

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El nombre del rol es requerido"));
        }

        if (roleRepository.existsByName(request.getName().trim())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El rol ya existe"));
        }

        RoleEntity role = new RoleEntity();
        role.setName(request.getName().trim().toUpperCase());
        role.setDescription(request.getDescription());

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            List<PermissionEntity> permissions = permissionRepository.findAllById(request.getPermissionIds());
            role.setPermissions(new HashSet<>(permissions));
        }

        roleRepository.save(role);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Rol creado exitosamente", null));
    }

    private UserInfo buildUserInfo(UserEntity user) {
        Set<String> roles = user.getRoles().stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toSet());

        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(PermissionEntity::getName)
                .collect(Collectors.toSet());

        return UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .status(user.getStatus())
                .enabled(isUserEnabled(user))  // Método helper
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLogin(user.getLastLogin())
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    private Boolean isUserEnabled(UserEntity user) {
        return user.getStatus() == UserStatus.CONFIRMED && user.getEmailVerified();
    }

    private RoleInfo buildRoleInfo(RoleEntity roleEntity) {
        return RoleInfo.builder()
                .id(roleEntity.getId())
                .name(roleEntity.getName())
                .description(roleEntity.getDescription())
                .build();
    }

    @GetMapping("/opciones")
    public ResponseEntity<List<OpcionDTO>> getAllOpciones() {
        List<OpcionDTO> opciones = cognitoService.getAllOpciones();
        return ResponseEntity.ok(opciones);
    }

    @GetMapping("/roles/{roleName}/opciones/{proyecto}")
    public ResponseEntity<Set<OpcionDTO>> getOpcionesByRole(@PathVariable("roleName") String role, @PathVariable("proyecto") String proyecto) {
        Set<OpcionDTO> opciones = cognitoService.getOpcionesByRoleId(role, proyecto);
        return ResponseEntity.ok(opciones);
    }

}