package com.nocountry.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for message metrics aggregated by week of the month.
 * Includes response rate calculated for each week.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyMetricsDTO {

    /**
     * Week number within the month (1-5)
     */
    private Integer weekOfMonth;

    /**
     * Month (1-12)
     */
    private Integer month;

    /**
     * Year
     */
    private Integer year;

    /**
     * Start date of the week (Monday)
     */
    private LocalDate weekStart;

    /**
     * End date of the week (Sunday)
     */
    private LocalDate weekEnd;

    /**
     * Total de mensajes enviados (OUTBOUND) en esta semana
     */
    private Long totalMessagesSent;

    /**
     * Total de mensajes recibidos (INBOUND) en esta semana
     */
    private Long totalMessagesReceived;

    /**
     * Conversaciones iniciadas en esta semana
     */
    private Long conversationsStarted;

    /**
     * Conversaciones que recibieron respuesta en esta semana
     */
    private Long conversationsWithResponse;

    /**
     * Tasa de respuesta (%) para esta semana
     */
    private Double responseRate;
}
