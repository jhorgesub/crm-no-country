package com.nocountry.backend.controller;

import com.nocountry.backend.dto.MessageMetricsByChannelDTO;
import com.nocountry.backend.dto.MessageMetricsByPeriodDTO;
import com.nocountry.backend.dto.MessageMetricsDTO;
import com.nocountry.backend.dto.WeeklyMetricsDTO;
import com.nocountry.backend.enums.Channel;
import com.nocountry.backend.services.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador REST para métricas de mensajes.
 * Proporciona endpoints para obtener estadísticas sobre mensajes enviados y
 * tasa de respuesta.
 */
@RestController
@RequestMapping("/api/messages/metrics")
@RequiredArgsConstructor
public class MessageMetricsController {

    private final MessageService messageService;

    /**
     * GET /api/messages/metrics
     * 
     * Obtiene métricas generales de mensajes:
     * - Total de mensajes enviados (OUTBOUND)
     * - Total de mensajes recibidos (INBOUND)
     * - Total de conversaciones iniciadas
     * - Conversaciones que recibieron respuesta
     * - Tasa de respuesta (%)
     * 
     * @return MessageMetricsDTO con las métricas calculadas
     */
    @GetMapping
    public ResponseEntity<MessageMetricsDTO> getMessageMetrics() {
        MessageMetricsDTO metrics = messageService.getMessageMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /api/messages/metrics/by-channel
     * 
     * Obtiene métricas separadas por canal de comunicación (WhatsApp y Email):
     * - Métricas de WhatsApp (mensajes enviados, recibidos, tasa de respuesta)
     * - Métricas de Email (mensajes enviados, recibidos, tasa de respuesta)
     * 
     * @return MessageMetricsByChannelDTO con métricas por canal
     */
    @GetMapping("/by-channel")
    public ResponseEntity<MessageMetricsByChannelDTO> getMetricsByChannel() {
        MessageMetricsByChannelDTO metrics = messageService.getMetricsByChannel();
        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /api/messages/metrics/by-period
     * 
     * Obtiene métricas agrupadas por período (día/semana) con filtros opcionales.
     * Útil para gráficos de actividad de comunicación y tendencias.
     * 
     * @param startDate Fecha de inicio (opcional, formato: YYYY-MM-DD)
     * @param endDate   Fecha de fin (opcional, formato: YYYY-MM-DD)
     * @param channel   Canal a filtrar (opcional: WHATSAPP o EMAIL)
     * @return Lista de MessageMetricsByPeriodDTO agrupadas por fecha y canal
     */
    @GetMapping("/by-period")
    public ResponseEntity<List<MessageMetricsByPeriodDTO>> getMetricsByPeriod(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @RequestParam(required = false) Channel channel) {

        List<MessageMetricsByPeriodDTO> metrics = messageService.getMetricsByPeriod(startDate, endDate, channel);
        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /api/messages/metrics/by-week
     * Obtiene métricas agrupadas por semana del mes actual.
     * Calcula la tasa de respuesta para cada semana.
     *
     * @param channel Canal opcional para filtrar (WHATSAPP o EMAIL)
     * @return Lista de métricas semanales del mes actual
     */
    @GetMapping("/by-week")
    public ResponseEntity<List<WeeklyMetricsDTO>> getMetricsByWeek(
            @RequestParam(required = false) Channel channel) {

        log.info("📊 GET /api/messages/metrics/by-week - channel: {}", channel);

        List<WeeklyMetricsDTO> metrics = messageService.getMetricsByWeek(channel);
        return ResponseEntity.ok(metrics);
    }
}
