package com.cognito.virtual.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "opciones", schema = "public")
public class Opcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nombre;

    @Column(nullable = false, unique = true, length = 255)
    private String codigo;

    @Column(length = 100)
    private String icono;

    @Column(length = 500)
    private String descripcion;

    @Column(length = 500)
    private String proyecto;

    @OneToMany(mappedBy = "opcion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<RoleOpcion> roleOpciones = new HashSet<>();
}