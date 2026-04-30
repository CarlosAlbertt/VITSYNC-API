package com.ejemplo.vitsync.dto;

import com.ejemplo.vitsync.enums.CategoriaSalud;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SaludCategoriaDetalleResponse {
    private CategoriaSalud categoria;
    private Double porcentaje;
    private List<IndicadorSaludDTO> indicadores;
    private List<PuntoHistoricoDTO> historico;
}
