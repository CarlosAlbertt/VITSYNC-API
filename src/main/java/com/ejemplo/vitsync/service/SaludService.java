package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.dto.*;
import com.ejemplo.vitsync.enums.CategoriaSalud;
import com.ejemplo.vitsync.enums.EstadoIndicador;
import com.ejemplo.vitsync.model.IndicadorSalud;
import com.ejemplo.vitsync.repository.IndicadorSaludRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SaludService {

    private final IndicadorSaludRepository repository;

    public SaludService(IndicadorSaludRepository repository) {
        this.repository = repository;
    }

    // ── Resumen (todas las categorías) ──────────────────────────────────────

    public SaludResumenResponse getResumenPaciente(Long pacienteId) {
        List<IndicadorSalud> todos = repository.findByPacienteId(pacienteId);
        LocalDate hace30dias = LocalDate.now().minusDays(30);

        List<ResumenCategoriaDTO> categorias = Arrays.stream(CategoriaSalud.values())
                .map(cat -> {
                    List<IndicadorSalud> deCategoria = todos.stream()
                            .filter(i -> i.getCategoria() == cat)
                            .collect(Collectors.toList());

                    Double porcentajeActual  = porcentajeActual(deCategoria);
                    Double porcentajeAnterior = porcentajeAntes(deCategoria, hace30dias);
                    Double variacion = (porcentajeActual != null && porcentajeAnterior != null)
                            ? redondear(porcentajeActual - porcentajeAnterior)
                            : null;

                    return ResumenCategoriaDTO.builder()
                            .nombre(cat)
                            .porcentaje(porcentajeActual)
                            .variacion(variacion)
                            .build();
                })
                .collect(Collectors.toList());

        return SaludResumenResponse.builder().categorias(categorias).build();
    }

    // ── Detalle de categoría ────────────────────────────────────────────────

    public SaludCategoriaDetalleResponse getDetalleCategoria(Long pacienteId, CategoriaSalud categoria) {
        List<IndicadorSalud> todos = repository.findByPacienteIdAndCategoria(pacienteId, categoria);

        // Indicador más reciente por nombre → estado actual
        Map<String, IndicadorSalud> masRecientes = masRecientesPorNombre(todos);
        List<IndicadorSaludDTO> indicadores = masRecientes.values().stream()
                .sorted(Comparator.comparing(IndicadorSalud::getNombre))
                .map(IndicadorSaludDTO::fromEntity)
                .collect(Collectors.toList());

        Double porcentaje = masRecientes.isEmpty()
                ? null
                : calcularPorcentaje(new ArrayList<>(masRecientes.values()));

        // Histórico: porcentaje por fecha para el gráfico
        Map<LocalDate, List<IndicadorSalud>> porFecha = todos.stream()
                .collect(Collectors.groupingBy(IndicadorSalud::getFecha));

        List<PuntoHistoricoDTO> historico = porFecha.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> PuntoHistoricoDTO.builder()
                        .fecha(e.getKey())
                        .porcentaje(calcularPorcentaje(e.getValue()))
                        .build())
                .collect(Collectors.toList());

        return SaludCategoriaDetalleResponse.builder()
                .categoria(categoria)
                .porcentaje(porcentaje)
                .indicadores(indicadores)
                .historico(historico)
                .build();
    }

    // ── Guardar / actualizar indicador ──────────────────────────────────────

    public IndicadorSalud guardarIndicador(Long pacienteId, CategoriaSalud categoria,
                                           IndicadorSaludRequest request) {
        LocalDate fecha = request.getFecha() != null ? request.getFecha() : LocalDate.now();

        // Upsert: si ya existe un indicador con el mismo nombre y fecha, se actualiza
        IndicadorSalud indicador = repository
                .findByPacienteIdAndCategoriaAndNombreAndFecha(pacienteId, categoria, request.getNombre(), fecha)
                .orElseGet(IndicadorSalud::new);

        indicador.setPacienteId(pacienteId);
        indicador.setCategoria(categoria);
        indicador.setNombre(request.getNombre());
        indicador.setValor(request.getValor());
        indicador.setUnidad(request.getUnidad());
        indicador.setFecha(fecha);
        indicador.setEstado(request.getEstado());

        return repository.save(indicador);
    }

    // ── Helpers privados ────────────────────────────────────────────────────

    /** Porcentaje basado en el indicador más reciente por nombre (estado actual). */
    private Double porcentajeActual(List<IndicadorSalud> indicadores) {
        if (indicadores.isEmpty()) return null;
        Map<String, IndicadorSalud> masRecientes = masRecientesPorNombre(indicadores);
        return calcularPorcentaje(new ArrayList<>(masRecientes.values()));
    }

    /** Porcentaje del estado más reciente antes de la fecha dada (para calcular variación). */
    private Double porcentajeAntes(List<IndicadorSalud> indicadores, LocalDate antes) {
        List<IndicadorSalud> previos = indicadores.stream()
                .filter(i -> !i.getFecha().isAfter(antes))
                .collect(Collectors.toList());
        if (previos.isEmpty()) return null;
        Map<String, IndicadorSalud> masRecientes = masRecientesPorNombre(previos);
        return calcularPorcentaje(new ArrayList<>(masRecientes.values()));
    }

    /** Devuelve el indicador más reciente por cada nombre. */
    private Map<String, IndicadorSalud> masRecientesPorNombre(List<IndicadorSalud> indicadores) {
        return indicadores.stream()
                .collect(Collectors.toMap(
                        IndicadorSalud::getNombre,
                        i -> i,
                        (a, b) -> a.getFecha().isAfter(b.getFecha()) ? a : b
                ));
    }

    /** NORMAL→100, ALERTA→50, CRITICO→0; promedio redondeado a 1 decimal. */
    private double calcularPorcentaje(List<IndicadorSalud> indicadores) {
        if (indicadores.isEmpty()) return 0.0;
        double suma = indicadores.stream()
                .mapToDouble(i -> switch (i.getEstado()) {
                    case NORMAL  -> 100.0;
                    case ALERTA  -> 50.0;
                    case CRITICO -> 0.0;
                })
                .sum();
        return redondear(suma / indicadores.size());
    }

    private double redondear(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}
