package com.ejemplo.vitsync.dto;

import com.ejemplo.vitsync.enums.EstadoIndicador;
import com.ejemplo.vitsync.model.IndicadorSalud;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IndicadorSaludDTO {

    private Long id;
    private String nombre;
    private Double valor;
    private String unidad;
    private LocalDate fecha;
    private EstadoIndicador estado;

    public static IndicadorSaludDTO fromEntity(IndicadorSalud i) {
        return IndicadorSaludDTO.builder()
                .id(i.getId())
                .nombre(i.getNombre())
                .valor(i.getValor())
                .unidad(i.getUnidad())
                .fecha(i.getFecha())
                .estado(i.getEstado())
                .build();
    }
}
