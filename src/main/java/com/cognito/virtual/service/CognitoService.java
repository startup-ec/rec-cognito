    package com.cognito.virtual.service;

    import com.cognito.virtual.dto.*;
    import com.cognito.virtual.entity.*;
    import com.cognito.virtual.repository.*;
    import com.cognito.virtual.util.JwtUtil;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.transaction.annotation.Transactional;

    import java.util.Comparator;
    import java.util.LinkedHashSet;
    import java.util.List;
    import java.util.Set;
    import java.util.stream.Collectors;

    @org.springframework.stereotype.Service
    @lombok.RequiredArgsConstructor
    public class CognitoService {

        private static final Logger log = LoggerFactory.getLogger(CognitoService.class);

        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final OpcionRepository opcionRepository;
        private final RefreshTokenRepository refreshTokenRepository;
        private final JwtUtil jwtUtil;
        private final EmailService emailService;

        @Value("${cognito.jwt.refresh-expiration}")
        private Long refreshTokenExpiration;

        @Value("${cognito.jwt.expiration}")
        private Long TokenExpiration;

        @Value("${cognito.mock.isValidMock}")
        private boolean isValidMock;


        public ApiResponse<AuthResponse> signUp(SignUpRequest request) {
            try {
                // Validar si el usuario ya existe
                if (userRepository.existsByUsername(request.getUsername())) {
                    return ApiResponse.error("Username ya existe");
                }

                if (userRepository.existsByEmail(request.getEmail())) {
                    return ApiResponse.error("Email ya está registrado");
                }

                // Crear nuevo usuario
                UserEntity user = UserEntity.builder()
                        .username(request.getUsername())
                        .email(request.getEmail())
                        .password(org.mindrot.jbcrypt.BCrypt.hashpw(request.getPassword(), org.mindrot.jbcrypt.BCrypt.gensalt()))
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .phoneNumber(request.getPhoneNumber())
                        .status(UserStatus.UNCONFIRMED)
                        .confirmationCode(generateConfirmationCode())
                        .build();

                // Asignar rol por defecto
                RoleEntity defaultRole = roleRepository.findByName("USER")
                        .orElseGet(() -> createDefaultRole("USER", "Usuario básico"));
                user.getRoles().add(defaultRole);

                user = userRepository.save(user);

                // Enviar email de confirmación
                emailService.sendConfirmationEmail(user.getEmail(), user.getConfirmationCode());

                // Generar tokens (opcional - algunos sistemas esperan confirmar primero)
                String accessToken = jwtUtil.generateAccessToken(user);
                String refreshToken = createRefreshToken(user);

                AuthResponse authResponse = AuthResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .expiresIn(TokenExpiration) // 24 horas
                        .userInfo(buildUserInfo(user))
                        .build();

                return ApiResponse.success("Usuario registrado exitosamente", authResponse);

            } catch (Exception e) {
                log.error("Error en signUp: ", e);
                return ApiResponse.error("Error interno del servidor");
            }
        }

        public ApiResponse<String> confirmSignUp(ConfirmSignUpRequest request) {
            try {
                java.util.Optional<UserEntity> userOpt = userRepository.findByUsername(request.getUsername());

                if (userOpt.isEmpty()) {
                    return ApiResponse.error("Usuario no encontrado");
                }
                UserEntity user = userOpt.get();

                //Ref 1. Aplicar Moocks
                if(!isValidMock){
                    if (!request.getConfirmationCode().equals(user.getConfirmationCode())) {
                        return ApiResponse.error("Código de confirmación inválido");
                    }
                }

                user.setStatus(UserStatus.CONFIRMED);
                user.setEmailVerified(true);
                user.setPhoneVerified(true);
                user.setConfirmationCode(null);
                userRepository.save(user);

                return ApiResponse.success("Usuario confirmado exitosamente");

            } catch (Exception e) {
                log.error("Error en confirmSignUp: ", e);
                return ApiResponse.error("Error interno del servidor");
            }
        }

        public ApiResponse<AuthResponse> signIn(AuthRequest request) {
            try {
                java.util.Optional<UserEntity> userOpt = userRepository.findByUsernameOrEmail(request.getUsername(), request.getUsername());

                if (userOpt.isEmpty()) {
                    return ApiResponse.error("Credenciales inválidas");
                }

                UserEntity user = userOpt.get();

                if (!org.mindrot.jbcrypt.BCrypt.checkpw(request.getPassword(), user.getPassword())) {
                    return ApiResponse.error("Credenciales inválidas");
                }

                if (user.getStatus() == UserStatus.UNCONFIRMED) {
                    return ApiResponse.error("Usuario no confirmado");
                }

                if (user.getStatus() == UserStatus.ARCHIVED) {
                    return ApiResponse.error("Usuario archivado");
                }

                // Actualizar último login
                user.setLastLogin(java.time.LocalDateTime.now());
                userRepository.save(user);

                // Generar tokens
                String accessToken = jwtUtil.generateAccessToken(user);
                String refreshToken = createRefreshToken(user);

                AuthResponse authResponse = AuthResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        //.expiresIn(86400L)
                        .expiresIn(TokenExpiration)
                        .userInfo(buildUserInfo(user))
                        .build();

                return ApiResponse.success("Inicio de sesión exitoso", authResponse);

            } catch (Exception e) {
                log.error("Error en signIn: ", e);
                return ApiResponse.error("Error interno del servidor");
            }
        }

        public ApiResponse<AuthResponse> refreshToken(RefreshTokenRequest request) {
            try {
                java.util.Optional<RefreshTokenEntity> refreshTokenOpt =
                        refreshTokenRepository.findByToken(request.getRefreshToken());

                if (refreshTokenOpt.isEmpty()) {
                    return ApiResponse.error("Refresh token inválido");
                }

                RefreshTokenEntity refreshTokenEntity = refreshTokenOpt.get();

                if (refreshTokenEntity.isExpired() || refreshTokenEntity.getRevoked()) {
                    return ApiResponse.error("Refresh token expirado o revocado");
                }

                UserEntity user = refreshTokenEntity.getUser();

                // Generar nuevo access token
                String newAccessToken = jwtUtil.generateAccessToken(user);
                String newRefreshToken = createRefreshToken(user);

                // Revocar el refresh token anterior
                refreshTokenEntity.setRevoked(true);
                refreshTokenRepository.save(refreshTokenEntity);

                AuthResponse authResponse = AuthResponse.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(newRefreshToken)
                        //.expiresIn(86400L)
                        .expiresIn(TokenExpiration)
                        .userInfo(buildUserInfo(user))
                        .build();

                return ApiResponse.success("Token renovado exitosamente", authResponse);

            } catch (Exception e) {
                log.error("Error en refreshToken: ", e);
                return ApiResponse.error("Error interno del servidor");
            }
        }

        public ApiResponse<String> changePassword(ChangePasswordRequest request) {
            try {
                if (!jwtUtil.validateToken(request.getAccessToken())) {
                    return ApiResponse.error("Token inválido");
                }

                String username = jwtUtil.getUsernameFromToken(request.getAccessToken());
                java.util.Optional<UserEntity> userOpt = userRepository.findByUsername(username);

                if (userOpt.isEmpty()) {
                    return ApiResponse.error("Usuario no encontrado");
                }

                UserEntity user = userOpt.get();

                if (!org.mindrot.jbcrypt.BCrypt.checkpw(request.getPreviousPassword(), user.getPassword())) {
                    return ApiResponse.error("Password anterior incorrecta");
                }

                user.setPassword(org.mindrot.jbcrypt.BCrypt.hashpw(request.getProposedPassword(), org.mindrot.jbcrypt.BCrypt.gensalt()));
                userRepository.save(user);

                emailService.sendPasswordChangeNotification(user.getEmail());

                return ApiResponse.success("Password cambiada exitosamente");

            } catch (Exception e) {
                log.error("Error en changePassword: ", e);
                return ApiResponse.error("Error interno del servidor");
            }
        }

        public ApiResponse<String> forgotPassword(ForgotPasswordRequest request) {
            try {
                java.util.Optional<UserEntity> userOpt =
                        userRepository.findByUsernameOrEmail(request.getUsername(), request.getUsername());

                if (userOpt.isEmpty()) {
                    // Por seguridad, no revelar si el usuario existe
                    return ApiResponse.success("Si el usuario existe, se enviará un código de recuperación");
                }

                UserEntity user = userOpt.get();
                String resetToken = generateConfirmationCode();
                user.setResetToken(resetToken);
                userRepository.save(user);

                    emailService.sendPasswordResetEmail(user.getEmail(), resetToken);

                return ApiResponse.success("Código de recuperación enviado");

            } catch (Exception e) {
                log.error("Error en forgotPassword: ", e);
                return ApiResponse.error("Error interno del servidor");
            }
        }

        public ApiResponse<String> confirmForgotPassword(ConfirmForgotPasswordRequest request) {
            try {
                java.util.Optional<UserEntity> userOpt =
                        userRepository.findByUsername(request.getUsername());

                if (userOpt.isEmpty()) {
                    return ApiResponse.error("Usuario no encontrado");
                }

                UserEntity user = userOpt.get();

                //Ref 1. Aplicar Moocks
                if(!isValidMock){
                    if (!request.getConfirmationCode().equals(user.getResetToken())) {
                        return ApiResponse.error("Código de confirmación inválido");
                    }
                }
                user.setPassword(org.mindrot.jbcrypt.BCrypt.hashpw(request.getPassword(), org.mindrot.jbcrypt.BCrypt.gensalt()));
                user.setResetToken(null);
                userRepository.save(user);

                // Revocar todos los refresh tokens del usuario
                java.util.List<RefreshTokenEntity> userTokens =
                        refreshTokenRepository.findByUserAndRevokedFalse(user);
                userTokens.forEach(token -> token.setRevoked(true));
                refreshTokenRepository.saveAll(userTokens);

                return ApiResponse.success("Password restablecida exitosamente");

            } catch (Exception e) {
                log.error("Error en confirmForgotPassword: ", e);
                return ApiResponse.error("Error interno del servidor");
            }
        }

        private String createRefreshToken(UserEntity user) {
            String token = jwtUtil.generateRefreshToken();

            RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
                    .token(token)
                    .user(user)
                    .expiresAt(java.time.Instant.now().plusMillis(refreshTokenExpiration))
                    .build();

            refreshTokenRepository.save(refreshToken);
            return token;
        }

        private UserInfo buildUserInfo(UserEntity user) {
            java.util.Set<String> roles = user.getRoles().stream()
                    .map(RoleEntity::getName)
                    .collect(java.util.stream.Collectors.toSet());

            java.util.Set<String> permissions = user.getRoles().stream()
                    .flatMap(role -> role.getPermissions().stream())
                    .map(PermissionEntity::getName)
                    .collect(java.util.stream.Collectors.toSet());

            return UserInfo.builder()
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .phoneNumber(user.getPhoneNumber())
                    .emailVerified(user.getEmailVerified())
                    .phoneVerified(user.getPhoneVerified())
                    .roles(roles)
                    .permissions(permissions)
                    .id(user.getId())
                    .build();
        }

        private String generateConfirmationCode() {
            return String.valueOf((int) (Math.random() * 900000) + 100000);
        }

        private RoleEntity createDefaultRole(String name, String description) {
            RoleEntity role = RoleEntity.builder()
                    .name(name)
                    .description(description)
                    .build();
            return roleRepository.save(role);
        }

        public ApiResponse<UserInfo> updateUser(UpdateUserRequest request) {
            try {
                java.util.Optional<UserEntity> userOpt = userRepository.findByUsername(request.getUsername());

                if (userOpt.isEmpty()) {
                    return ApiResponse.error("Usuario no encontrado");
                }

                UserEntity user = userOpt.get();

                // Validar cambio de email si se proporciona uno nuevo
                if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
                    if (userRepository.existsByEmail(request.getEmail())) {
                        return ApiResponse.error("Email ya está registrado por otro usuario");
                    }
                    user.setEmail(request.getEmail());
                    //user.setEmailVerified(false); // Requiere nueva verificación

                    // Generar nuevo código de confirmación para el email
                    //user.setConfirmationCode(generateConfirmationCode());
                    //emailService.sendConfirmationEmail(user.getEmail(), user.getConfirmationCode());
                }

                // Actualizar campos opcionales si se proporcionan
                if (request.getFirstName() != null) {
                    user.setFirstName(request.getFirstName());
                }

                if (request.getLastName() != null) {
                    user.setLastName(request.getLastName());
                }

                if (request.getPhoneNumber() != null) {
                    user.setPhoneNumber(request.getPhoneNumber());
                    //user.setPhoneVerified(false); // Requiere nueva verificación
                }

                if (request.getStatus() != null) {
                    try {
                        UserStatus status = UserStatus.valueOf(request.getStatus().name());
                        user.setStatus(status);
                    } catch (IllegalArgumentException e) {
                        return ApiResponse.error("Estado de usuario inválido");
                    }
                }

                // Actualizar fecha de modificación
                user.setUpdatedAt(java.time.LocalDateTime.now());

                user = userRepository.save(user);

                UserInfo userInfo = buildUserInfo(user);

                String message = request.getEmail() != null && !user.getEmailVerified() ?
                        "Usuario actualizado. Se ha enviado código de verificación al nuevo email" :
                        "Usuario actualizado exitosamente";

                return ApiResponse.success(message, userInfo);

            } catch (Exception e) {
                log.error("Error en updateUser: ", e);
                return ApiResponse.error("Error interno del servidor");
            }
        }

        @Transactional(readOnly = true)
        public List<OpcionDTO> getAllOpciones() {
            return opcionRepository.findAll().stream()
                    .map(this::convertToDTO)
                    .sorted(Comparator.comparing(OpcionDTO::getNombre, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public Set<OpcionDTO> getOpcionesByRoleId(String roleName, String proyecto) {
            Long roleId = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName))
                    .getId();

            return opcionRepository.findByRoleIdAndProyecto(roleId, proyecto).stream()
                    .map(this::convertToDTO)
                    .sorted(Comparator.comparing(OpcionDTO::getNombre)) // Ordenar alfabéticamente
                    .collect(Collectors.toCollection(LinkedHashSet::new)); // Mantener el orden
        }
        @Transactional(readOnly = true)
        public Set<OpcionDTO> getOpcionesByRoleIdDirect(Long roleId) {
            return opcionRepository.findByRoleId(roleId).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toSet());
        }

        private OpcionDTO convertToDTO(Opcion entity) {
            return OpcionDTO.builder()
                    .id(entity.getId())
                    .nombre(entity.getNombre())
                    .codigo(entity.getCodigo())
                    .icono(entity.getIcono())
                    .descripcion(entity.getDescripcion())
                    .build();
        }

    }