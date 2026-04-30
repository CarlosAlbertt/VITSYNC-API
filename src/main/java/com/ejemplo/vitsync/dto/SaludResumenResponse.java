package com.ejemplo.vitsync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SaludResumenResponse {
    private List<ResumenCategoriaDTO> categorias;
}
