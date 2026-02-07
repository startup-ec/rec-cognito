package com.cognito.virtual.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "role_opciones")
public class RoleOpcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id")
    private int role_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opcion_id", nullable = false)
    private Opcion opcion;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "activo")
    @Builder.Default
    private Boolean activo = true;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}