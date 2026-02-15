package com.nocountry.backend.dto;

import com.nocountry.backend.enums.Channel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para métricas de mensajes agrupadas por período (día/semana)
 * Usado para gráficos de tendencias y actividad de comunicación
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageMetricsByPeriodDTO {

    /**
     * Fecha del período (día o inicio de semana)
     */
    private LocalDate period;

    /**
     * Canal de comunicación (WHATSAPP o EMAIL)
     */
    private Channel channel;

    /**
     * Total de mensajes enviados (OUTBOUND) en este período
     */
    private Long messagesSent;

    /**
     * Total de mensajes recibidos (INBOUND) en este período
     */
    private Long messagesReceived;

    /**
     * Día de la semana (opcional, para agrupación semanal)
     * Valores: "Monday", "Tuesday", etc.
     */
    private String dayOfWeek;
}
