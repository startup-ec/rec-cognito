package com.cognito.virtual.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpcionDTO {
    private Long id;
    private String nombre;
    private String codigo;
    private String icono;
    private String descripcion;
}