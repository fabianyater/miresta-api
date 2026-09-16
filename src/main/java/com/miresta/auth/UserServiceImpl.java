package com.miresta.auth;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @Override
    public UserResponse createUser(CreateUserRequest request, String currentUserEmail) {
        User actingUser = requireCurrentUser(currentUserEmail);

        if (request.role() != Role.MESERO && actingUser.getRole() != Role.OWNER) {
            throw new HierarchyAccessDeniedException("Solo el owner puede crear usuarios con rol " + request.role() + ".");
        }

        userRepository.findByEmail(request.email()).ifPresent(u -> {
            throw new EntityExistsException("Ya existe un usuario con ese correo: " + request.email());
        });

        User user = new User();
        user.setEmail(request.email());
        user.setName(request.name() != null ? request.name().trim() : "");
        user.setDisplayName(resolveDisplayName(request.displayName(), request.name(), request.email()));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setActive(true);
        user.setCreatedAt(Instant.now());

        User saved = userRepository.save(user);

        return toResponse(saved);
    }

    @Override
    public List<UserResponse> getUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @Override
    public UserResponse updateUser(Long id, UpdateUserRequest request, String currentUserEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + id));

        // Resolved by id, not by the (possibly stale, if this same request also
        // changes the email) current email — so "am I editing myself" stays correct.
        User actingUser = requireCurrentUser(currentUserEmail);
        boolean actingIsOwner = actingUser.getRole() == Role.OWNER;
        boolean isSelf = actingUser.getId().equals(id);
        Role roleBeforeChange = user.getRole();

        if (roleBeforeChange == Role.OWNER && !actingIsOwner && !isSelf) {
            throw new HierarchyAccessDeniedException("Solo el owner puede modificar a otro owner.");
        }
        if (request.role() != null && request.role() != roleBeforeChange
                && request.role() != Role.MESERO && !actingIsOwner) {
            throw new HierarchyAccessDeniedException("Solo el owner puede asignar el rol " + request.role() + ".");
        }
        if (isSelf && request.active() != null && !request.active()) {
            throw new IllegalStateException("No puedes desactivar tu propia cuenta.");
        }
        if (isSelf && request.role() != null && request.role() != roleBeforeChange) {
            throw new IllegalStateException("No puedes cambiar tu propio rol.");
        }

        if (request.email() != null && !request.email().equalsIgnoreCase(user.getEmail())) {
            userRepository.findByEmail(request.email()).ifPresent(existing -> {
                throw new EntityExistsException("Ya existe un usuario con ese correo: " + request.email());
            });
            user.setEmail(request.email());
        }
        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }
        if (request.displayName() != null) {
            user.setDisplayName(resolveDisplayName(request.displayName(), user.getName(), user.getEmail()));
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }

        boolean stillActiveOwner = user.getRole() == Role.OWNER && user.isActive();
        long otherActiveOwners = userRepository.countByRoleAndActiveTrueAndIdNot(Role.OWNER, id);
        if (otherActiveOwners + (stillActiveOwner ? 1 : 0) == 0) {
            throw new IllegalStateException("Debe quedar al menos un owner activo.");
        }

        return toResponse(userRepository.save(user));
    }

    @Transactional
    @Override
    public UserResponse updateOwnProfile(UpdateOwnProfileRequest request, String currentUserEmail) {
        User self = requireCurrentUser(currentUserEmail);
        UpdateUserRequest restricted = new UpdateUserRequest(
                null, request.name(), request.displayName(), request.password(), null, null);
        return updateUser(self.getId(), restricted, currentUserEmail);
    }

    @Transactional
    @Override
    public void deleteUser(Long id, String currentUserEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + id));

        User actingUser = requireCurrentUser(currentUserEmail);

        if (user.getId().equals(actingUser.getId())) {
            throw new IllegalStateException("No puedes eliminar tu propia cuenta.");
        }

        if (user.getRole() == Role.OWNER) {
            if (actingUser.getRole() != Role.OWNER) {
                throw new HierarchyAccessDeniedException("Solo el owner puede eliminar a otro owner.");
            }
            long otherActiveOwners = userRepository.countByRoleAndActiveTrueAndIdNot(Role.OWNER, id);
            if (otherActiveOwners == 0) {
                throw new IllegalStateException("Debe quedar al menos un owner activo.");
            }
        }

        userRepository.delete(user);
    }

    private User requireCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario actual no encontrado: " + email));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(), user.getEmail(), user.getName(), user.getDisplayName(),
                user.getRole(), user.isActive());
    }

    /**
     * Nombre corto para el badge de Pedidos y el ticket: el que se indique, o la
     * primera palabra del nombre completo, o la parte local del correo como red de
     * seguridad — nunca vacío.
     */
    private String resolveDisplayName(String displayName, String name, String email) {
        if (displayName != null && !displayName.isBlank()) {
            return displayName.trim();
        }
        if (name != null && !name.isBlank()) {
            return name.trim().split("\\s+")[0];
        }
        return email.split("@")[0];
    }
}
