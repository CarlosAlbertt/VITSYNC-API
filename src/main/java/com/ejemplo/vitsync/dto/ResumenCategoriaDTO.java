package com.ejemplo.vitsync.dto;

import com.ejemplo.vitsync.enums.CategoriaSalud;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResumenCategoriaDTO {
    private CategoriaSalud nombre;
    private Double porcentaje;
    private Double variacion;
}
