package com.nocountry.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para métricas de mensajes agrupadas por canal (WhatsApp o Email)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageMetricsByChannelDTO {

    private ChannelMetrics whatsapp;
    private ChannelMetrics email;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelMetrics {
        private Long totalMessagesSent;
        private Long totalMessagesReceived;
        private Long conversationsWithResponse;
        private Long totalConversationsStarted;
        private Double responseRate;
    }
}
