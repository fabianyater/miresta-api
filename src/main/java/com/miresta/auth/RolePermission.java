package com.miresta.auth;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "role_permission")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@IdClass(RolePermission.Id.class)
public class RolePermission {

    @jakarta.persistence.Id
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @jakarta.persistence.Id
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false)
    private Permission permission;

    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Id implements Serializable {
        private Role role;
        private Permission permission;
    }
}
