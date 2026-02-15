package com.nocountry.backend.repository;

import com.nocountry.backend.entity.Message;
import com.nocountry.backend.enums.Channel;
import com.nocountry.backend.enums.Direction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

        /**
         * Busca todos los mensajes asociados a un ID de conversación específico,
         * ordenados por fecha de envío de forma ascendente.
         */
        List<Message> findByConversationIdOrderBySentAtAsc(Long conversationId);

        /**
         * Busca todos los mensajes asociados a un ID de conversación específico.
         * La convención de nombres 'findByConversationId' funciona automáticamente
         * con la entidad Message que tiene la relación 'conversation'.
         */
        List<Message> findByConversationId(Long conversationId);

        /**
         * Busca un mensaje por su ID externo (WhatsApp message_id).
         * Usado para encontrar mensajes citados en respuestas.
         */
        Optional<Message> findByExternalMessageId(String externalMessageId);

        /**
         * Cuenta el total de mensajes por dirección (INBOUND o OUTBOUND).
         */
        Long countByMessageDirection(Direction direction);

        /**
         * Obtiene los IDs únicos de conversaciones que tienen al menos un mensaje
         * OUTBOUND.
         * Usado para calcular el total de conversaciones iniciadas por usuarios.
         */
        @Query("SELECT DISTINCT m.conversation.id FROM Message m WHERE m.messageDirection = 'OUTBOUND'")
        List<Long> findConversationIdsWithOutboundMessages();

        /**
         * Obtiene los IDs únicos de conversaciones que tienen al menos un mensaje
         * INBOUND
         * después de haber enviado un mensaje OUTBOUND.
         * Usado para calcular la tasa de respuesta.
         */
        @Query("SELECT DISTINCT m1.conversation.id FROM Message m1 " +
                        "WHERE m1.messageDirection = 'INBOUND' " +
                        "AND EXISTS (SELECT m2 FROM Message m2 " +
                        "WHERE m2.conversation.id = m1.conversation.id " +
                        "AND m2.messageDirection = 'OUTBOUND' " +
                        "AND m2.sentAt < m1.sentAt)")
        List<Long> findConversationIdsWithInboundMessagesAfterOutbound();

        // ==================== QUERIES PARA MÉTRICAS POR CANAL ====================

        /**
         * Cuenta mensajes por dirección y canal específico.
         */
        Long countByMessageDirectionAndConversation_Channel(
                        Direction direction,
                        Channel channel);

        /**
         * Obtiene IDs de conversaciones con mensajes OUTBOUND filtradas por canal.
         */
        @Query("SELECT DISTINCT m.conversation.id FROM Message m " +
                        "WHERE m.messageDirection = 'OUTBOUND' " +
                        "AND m.conversation.channel = :channel")
        List<Long> findConversationIdsWithOutboundMessagesByChannel(
                        @Param("channel") Channel channel);

        /**
         * Obtiene IDs de conversaciones con respuestas INBOUND filtradas por canal.
         */
        @Query("SELECT DISTINCT m1.conversation.id FROM Message m1 " +
                        "WHERE m1.messageDirection = 'INBOUND' " +
                        "AND m1.conversation.channel = :channel " +
                        "AND EXISTS (SELECT m2 FROM Message m2 " +
                        "WHERE m2.conversation.id = m1.conversation.id " +
                        "AND m2.messageDirection = 'OUTBOUND' " +
                        "AND m2.sentAt < m1.sentAt)")
        List<Long> findConversationIdsWithInboundMessagesAfterOutboundByChannel(
                        @Param("channel") Channel channel);

        // ==================== QUERIES PARA MÉTRICAS POR PERÍODO ====================

        /**
         * Obtiene métricas agrupadas por fecha y canal con filtro de fechas.
         */
        @Query("SELECT CAST(m.sentAt AS LocalDate) as date, " +
                        "m.conversation.channel as channel, " +
                        "SUM(CASE WHEN m.messageDirection = 'OUTBOUND' THEN 1 ELSE 0 END) as sent, " +
                        "SUM(CASE WHEN m.messageDirection = 'INBOUND' THEN 1 ELSE 0 END) as received " +
                        "FROM Message m " +
                        "WHERE m.sentAt IS NOT NULL " +
                        "AND (:startDate IS NULL OR CAST(m.sentAt AS LocalDate) >= :startDate) " +
                        "AND (:endDate IS NULL OR CAST(m.sentAt AS LocalDate) <= :endDate) " +
                        "AND (:channel IS NULL OR m.conversation.channel = :channel) " +
                        "GROUP BY CAST(m.sentAt AS LocalDate), m.conversation.channel " +
                        "ORDER BY date DESC")
        List<Object[]> findMetricsGroupedByDateAndChannelWithFilters(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("channel") Channel channel);

        // ==================== QUERIES PARA MÉTRICAS POR SEMANA ====================

        /**
         * Obtiene todos los mensajes con su fecha para agrupar por semana en el
         * servicio.
         * Retorna: [fecha, canal, mensajes_enviados, mensajes_recibidos]
         */
        @Query("SELECT CAST(m.sentAt AS LocalDate) as date, " +
                        "m.conversation.channel as channel, " +
                        "SUM(CASE WHEN m.messageDirection = 'OUTBOUND' THEN 1 ELSE 0 END) as sent, " +
                        "SUM(CASE WHEN m.messageDirection = 'INBOUND' THEN 1 ELSE 0 END) as received " +
                        "FROM Message m " +
                        "WHERE m.sentAt IS NOT NULL " +
                        "AND (:startDate IS NULL OR CAST(m.sentAt AS LocalDate) >= :startDate) " +
                        "AND (:endDate IS NULL OR CAST(m.sentAt AS LocalDate) <= :endDate) " +
                        "AND (:channel IS NULL OR m.conversation.channel = :channel) " +
                        "GROUP BY CAST(m.sentAt AS LocalDate), m.conversation.channel " +
                        "ORDER BY date DESC")
        List<Object[]> findDailyMetricsForWeeklyGrouping(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("channel") Channel channel);

        /**
         * Obtiene conversaciones iniciadas con su fecha para agrupar por semana.
         * Retorna: [fecha, conversation_id]
         */
        @Query("SELECT CAST(MIN(m.sentAt) AS LocalDate) as date, m.conversation.id " +
                        "FROM Message m " +
                        "WHERE m.messageDirection = 'OUTBOUND' " +
                        "AND m.sentAt IS NOT NULL " +
                        "AND (:startDate IS NULL OR CAST(m.sentAt AS LocalDate) >= :startDate) " +
                        "AND (:endDate IS NULL OR CAST(m.sentAt AS LocalDate) <= :endDate) " +
                        "AND (:channel IS NULL OR m.conversation.channel = :channel) " +
                        "GROUP BY m.conversation.id")
        List<Object[]> findConversationsStartedDates(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("channel") Channel channel);

        /**
         * Obtiene conversaciones con respuesta y su fecha para agrupar por semana.
         * Retorna: [fecha, conversation_id]
         */
        @Query("SELECT CAST(MIN(m1.sentAt) AS LocalDate) as date, m1.conversation.id " +
                        "FROM Message m1 " +
                        "WHERE m1.messageDirection = 'INBOUND' " +
                        "AND m1.sentAt IS NOT NULL " +
                        "AND EXISTS (SELECT m2 FROM Message m2 " +
                        "WHERE m2.conversation.id = m1.conversation.id " +
                        "AND m2.messageDirection = 'OUTBOUND' " +
                        "AND m2.sentAt < m1.sentAt) " +
                        "AND (:startDate IS NULL OR CAST(m1.sentAt AS LocalDate) >= :startDate) " +
                        "AND (:endDate IS NULL OR CAST(m1.sentAt AS LocalDate) <= :endDate) " +
                        "AND (:channel IS NULL OR m1.conversation.channel = :channel) " +
                        "GROUP BY m1.conversation.id")
        List<Object[]> findConversationsWithResponseDates(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("channel") Channel channel);
}
