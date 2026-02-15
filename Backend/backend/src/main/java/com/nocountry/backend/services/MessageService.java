package com.nocountry.backend.services;

import com.nocountry.backend.dto.CreateMessageDTO;
import com.nocountry.backend.dto.MessageDTO;
import com.nocountry.backend.dto.MessageMetricsByChannelDTO;
import com.nocountry.backend.dto.MessageMetricsByPeriodDTO;
import com.nocountry.backend.dto.MessageMetricsDTO;
import com.nocountry.backend.dto.WeeklyMetricsDTO;
import com.nocountry.backend.entity.Conversation;
import com.nocountry.backend.entity.Message;
import com.nocountry.backend.enums.Channel;
import com.nocountry.backend.enums.Direction;
import com.nocountry.backend.mappers.MessageMapper;
import com.nocountry.backend.repository.ConversationRepository;
import com.nocountry.backend.repository.MessageRepository;
import com.nocountry.backend.services.whatsapp.WhatsAppApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final WhatsAppApiService whatsAppApiService;
    private final EmailService emailService;
    private final MessageMapper messageMapper;

    // --- CREATE OUTBOUND MESSAGE (POST) ---
    @Transactional
    public MessageDTO createOutboundMessage(CreateMessageDTO dto) {
        log.info("📨 Creating outbound message: type={}, mediaUrl={}, content={}, replyToMessageId={}",
                dto.messageType(), dto.mediaUrl(), dto.content(), dto.replyToMessageId());

        Conversation conversation = conversationRepository.findById(dto.conversationId())
                .orElseThrow(() -> new RuntimeException("Conversación no encontrada"));

        // 1. Guardar el mensaje en la DB con estado inicial
        Message message = messageMapper.toEntity(dto);
        message.setConversation(conversation);
        message.setSentAt(LocalDateTime.now());

        // Set media fields if present
        if (dto.mediaUrl() != null) {
            message.setMediaUrl(dto.mediaUrl());
        }
        if (dto.mediaFileName() != null) {
            message.setMediaFileName(dto.mediaFileName());
        }
        if (dto.mediaCaption() != null) {
            message.setMediaCaption(dto.mediaCaption());
        }

        // Handle reply-to message
        String replyToExternalId = null;
        if (dto.replyToMessageId() != null) {
            Message replyToMessage = messageRepository.findById(dto.replyToMessageId())
                    .orElse(null);
            if (replyToMessage != null) {
                message.setReplyToMessage(replyToMessage);
                replyToExternalId = replyToMessage.getExternalMessageId();
                log.info("📝 Replying to message ID: {} (external: {})", dto.replyToMessageId(), replyToExternalId);
            }
        }

        Message savedMessage = messageRepository.save(message);

        // 2. Si el canal es WhatsApp, llamar a la API externa
        if (conversation.getChannel() == Channel.WHATSAPP) {

            String recipientPhoneNumber = conversation.getCrm_lead().getPhone();
            Map<String, String> metaResponse;

            // Send based on message type
            if (dto.messageType() == com.nocountry.backend.enums.MessageType.TEXT) {
                metaResponse = whatsAppApiService.sendTextMessage(
                        recipientPhoneNumber,
                        dto.content(),
                        replyToExternalId);
            } else if (dto.mediaUrl() != null) {
                // Send media message (IMAGE, VIDEO, AUDIO, DOCUMENT)
                log.info("📎 Sending media to WhatsApp: type={}, url={}", dto.messageType(), dto.mediaUrl());
                metaResponse = whatsAppApiService.sendMediaMessage(
                        recipientPhoneNumber,
                        dto.mediaUrl(),
                        dto.messageType(),
                        dto.mediaCaption(),
                        dto.mediaFileName(),
                        replyToExternalId);
            } else {
                metaResponse = java.util.Collections.emptyMap();
            }

            // Update with external message ID if available
            String externalId = metaResponse.get("external_message_id");
            if (externalId != null) {
                savedMessage.setExternalMessageId(externalId);
                messageRepository.save(savedMessage);
            }
        } else if (conversation.getChannel() == Channel.EMAIL) {
            // 2b. Si el canal es Email, enviar el correo real
            String recipientEmail = conversation.getCrm_lead().getEmail();
            if (recipientEmail != null && !recipientEmail.isBlank()) {
                String subject = dto.subject() != null ? dto.subject() : "Mensaje del CRM";
                String htmlBody = dto.content();

                // Buscar el último mensaje INBOUND para obtener el Message-ID para threading
                String inReplyTo = null;
                String references = null;

                java.util.List<Message> conversationMessages = messageRepository
                        .findByConversationIdOrderBySentAtAsc(dto.conversationId());

                // Buscar el último mensaje entrante (del lead)
                for (int i = conversationMessages.size() - 1; i >= 0; i--) {
                    Message msg = conversationMessages.get(i);
                    if (msg.getMessageDirection() == Direction.INBOUND
                            && msg.getExternalMessageId() != null) {
                        inReplyTo = msg.getExternalMessageId();
                        // Construir References (todos los Message-IDs del thread)
                        StringBuilder refs = new StringBuilder();
                        for (Message m : conversationMessages) {
                            if (m.getExternalMessageId() != null && !m.getExternalMessageId().isBlank()) {
                                if (refs.length() > 0)
                                    refs.append(" ");
                                refs.append(m.getExternalMessageId());
                            }
                        }
                        references = refs.toString();
                        break;
                    }
                }

                try {
                    emailService.sendHtmlEmail(recipientEmail, subject, htmlBody, inReplyTo, references);
                } catch (Exception e) {
                    throw new RuntimeException("Error al enviar email: " + e.getMessage(), e);
                }
            } else {
                throw new RuntimeException("El lead no tiene email configurado");
            }
        }

        // 3. Actualizar la conversación (último mensaje)
        conversation.setLastMessageText(dto.content());
        conversation.setLastMessageAt(savedMessage.getSentAt());
        conversation.setLastMessageDirection(dto.messageDirection());
        conversationRepository.save(conversation);

        return messageMapper.toDTO(savedMessage);
    }

    // --- READ ALL BY CONVERSATION ID (GET) ---
    public List<MessageDTO> findMessagesByConversationId(Long conversationId) {
        List<Message> messages = messageRepository.findByConversationId(conversationId);

        return messages.stream()
                .map(messageMapper::toDTO)
                .collect(Collectors.toList());
    }

    // --- SAVE INBOUND MESSAGE (FROM WEBHOOK) ---
    @Transactional
    public MessageDTO saveInboundMessage(
            Conversation conversation,
            String content,
            String externalMessageId,
            LocalDateTime timestamp) {

        // Crear entidad Message para mensaje INBOUND
        Message message = Message.builder()
                .conversation(conversation)
                .senderType(com.nocountry.backend.enums.SenderType.LEAD)
                .senderLeadId(conversation.getCrm_lead().getId())
                .messageDirection(com.nocountry.backend.enums.Direction.INBOUND)
                .messageType(com.nocountry.backend.enums.MessageType.TEXT)
                .content(content)
                .externalMessageId(externalMessageId)
                .sentAt(timestamp)
                .build();

        Message savedMessage = messageRepository.save(message);

        return messageMapper.toDTO(savedMessage);
    }

    // --- SAVE INBOUND MEDIA MESSAGE (FROM WEBHOOK - WITH MEDIA SUPPORT) ---
    @Transactional
    public MessageDTO saveInboundMediaMessage(
            Conversation conversation,
            String content,
            String externalMessageId,
            LocalDateTime timestamp,
            com.nocountry.backend.enums.MessageType messageType,
            String mediaUrl,
            String mediaFileName,
            String mimeType,
            String mediaCaption,
            Message replyToMessage) {

        Message message = Message.builder()
                .conversation(conversation)
                .senderType(com.nocountry.backend.enums.SenderType.LEAD)
                .senderLeadId(conversation.getCrm_lead().getId())
                .messageDirection(com.nocountry.backend.enums.Direction.INBOUND)
                .messageType(messageType)
                .content(content)
                .mediaUrl(mediaUrl)
                .mediaFileName(mediaFileName)
                .mediaType(mimeType)
                .mediaCaption(mediaCaption)
                .externalMessageId(externalMessageId)
                .sentAt(timestamp)
                .replyToMessage(replyToMessage)
                .build();

        Message savedMessage = messageRepository.save(message);

        return messageMapper.toDTO(savedMessage);
    }

    // --- SAVE INBOUND MEDIA MESSAGE (FROM WEBHOOK - WITHOUT REPLY - BACKWARDS
    // COMPAT) ---
    @Transactional
    public MessageDTO saveInboundMediaMessage(
            Conversation conversation,
            String content,
            String externalMessageId,
            LocalDateTime timestamp,
            com.nocountry.backend.enums.MessageType messageType,
            String mediaUrl,
            String mediaFileName,
            String mimeType,
            String mediaCaption) {
        return saveInboundMediaMessage(conversation, content, externalMessageId, timestamp,
                messageType, mediaUrl, mediaFileName, mimeType, mediaCaption, null);
    }

    // --- FIND MESSAGE BY EXTERNAL MESSAGE ID (FOR QUOTED MESSAGES) ---
    public Message findByExternalMessageId(String externalMessageId) {
        if (externalMessageId == null || externalMessageId.isEmpty()) {
            return null;
        }
        return messageRepository.findByExternalMessageId(externalMessageId).orElse(null);
    }

    // --- GET MESSAGE METRICS ---
    /**
     * Calcula métricas generales de mensajes:
     * - Total de mensajes enviados (OUTBOUND)
     * - Total de mensajes recibidos (INBOUND)
     * - Conversaciones iniciadas
     * - Conversaciones con respuesta
     * - Tasa de respuesta
     */
    public MessageMetricsDTO getMessageMetrics() {
        log.info("📊 Calculando métricas de mensajes...");

        // 1. Contar mensajes OUTBOUND (enviados por usuarios)
        Long totalMessagesSent = messageRepository.countByMessageDirection(Direction.OUTBOUND);
        log.info("📤 Total mensajes enviados (OUTBOUND): {}", totalMessagesSent);

        // 2. Contar mensajes INBOUND (recibidos de leads)
        Long totalMessagesReceived = messageRepository.countByMessageDirection(Direction.INBOUND);
        log.info("📥 Total mensajes recibidos (INBOUND): {}", totalMessagesReceived);

        // 3. Obtener conversaciones que tienen mensajes OUTBOUND
        List<Long> conversationsWithOutbound = messageRepository.findConversationIdsWithOutboundMessages();
        Long totalConversationsStarted = (long) conversationsWithOutbound.size();
        log.info("💬 Total conversaciones iniciadas: {}", totalConversationsStarted);

        // 4. Obtener conversaciones que recibieron respuesta INBOUND después de
        // OUTBOUND
        List<Long> conversationsWithResponse = messageRepository.findConversationIdsWithInboundMessagesAfterOutbound();
        Long totalConversationsWithResponse = (long) conversationsWithResponse.size();
        log.info("✅ Conversaciones con respuesta: {}", totalConversationsWithResponse);

        // 5. Calcular tasa de respuesta
        Double responseRate = 0.0;
        if (totalConversationsStarted > 0) {
            responseRate = (totalConversationsWithResponse.doubleValue() / totalConversationsStarted.doubleValue())
                    * 100.0;
        }
        log.info("📈 Tasa de respuesta: {:.2f}%", responseRate);

        // 6. Construir y retornar el DTO
        return MessageMetricsDTO.builder()
                .totalMessagesSent(totalMessagesSent)
                .totalMessagesReceived(totalMessagesReceived)
                .conversationsWithResponse(totalConversationsWithResponse)
                .totalConversationsStarted(totalConversationsStarted)
                .responseRate(Math.round(responseRate * 100.0) / 100.0) // Redondear a 2 decimales
                .build();
    }

    // --- GET MESSAGE METRICS BY CHANNEL ---
    /**
     * Calcula métricas de mensajes separadas por canal (WhatsApp y Email)
     */
    public MessageMetricsByChannelDTO getMetricsByChannel() {
        log.info("📊 Calculando métricas por canal...");

        // Calcular métricas para WhatsApp
        MessageMetricsByChannelDTO.ChannelMetrics whatsappMetrics = calculateMetricsForChannel(Channel.WHATSAPP);

        // Calcular métricas para Email
        MessageMetricsByChannelDTO.ChannelMetrics emailMetrics = calculateMetricsForChannel(Channel.EMAIL);

        return MessageMetricsByChannelDTO.builder()
                .whatsapp(whatsappMetrics)
                .email(emailMetrics)
                .build();
    }

    /**
     * Método auxiliar para calcular métricas de un canal específico
     */
    private MessageMetricsByChannelDTO.ChannelMetrics calculateMetricsForChannel(
            Channel channel) {

        log.info("📱 Calculando métricas para canal: {}", channel);

        // Contar mensajes por dirección y canal
        Long totalSent = messageRepository.countByMessageDirectionAndConversation_Channel(
                Direction.OUTBOUND, channel);
        Long totalReceived = messageRepository.countByMessageDirectionAndConversation_Channel(
                Direction.INBOUND, channel);

        // Obtener conversaciones iniciadas en este canal
        List<Long> conversationsStarted = messageRepository
                .findConversationIdsWithOutboundMessagesByChannel(channel);
        Long totalConversationsStarted = (long) conversationsStarted.size();

        // Obtener conversaciones con respuesta en este canal
        List<Long> conversationsWithResponse = messageRepository
                .findConversationIdsWithInboundMessagesAfterOutboundByChannel(channel);
        Long totalConversationsWithResponse = (long) conversationsWithResponse.size();

        // Calcular tasa de respuesta
        Double responseRate = 0.0;
        if (totalConversationsStarted > 0) {
            responseRate = (totalConversationsWithResponse.doubleValue() /
                    totalConversationsStarted.doubleValue()) * 100.0;
        }

        log.info("✅ Canal {}: {} enviados, {} recibidos, tasa {}%",
                channel, totalSent, totalReceived, String.format("%.2f", responseRate));

        return MessageMetricsByChannelDTO.ChannelMetrics.builder()
                .totalMessagesSent(totalSent)
                .totalMessagesReceived(totalReceived)
                .conversationsWithResponse(totalConversationsWithResponse)
                .totalConversationsStarted(totalConversationsStarted)
                .responseRate(Math.round(responseRate * 100.0) / 100.0)
                .build();
    }

    // --- GET MESSAGE METRICS BY PERIOD ---
    /**
     * Obtiene métricas agrupadas por período (día/semana) con filtros opcionales
     */
    public List<MessageMetricsByPeriodDTO> getMetricsByPeriod(
            LocalDate startDate,
            LocalDate endDate,
            Channel channel) {

        log.info("📊 Obteniendo métricas por período: startDate={}, endDate={}, channel={}",
                startDate, endDate, channel);

        // Obtener datos agrupados del repositorio
        List<Object[]> results = messageRepository.findMetricsGroupedByDateAndChannelWithFilters(
                startDate, endDate, channel);

        // Transformar Object[] a DTO
        return results.stream()
                .map(row -> {
                    LocalDate date = (LocalDate) row[0];
                    Channel channelValue = (Channel) row[1];
                    Long sent = ((Number) row[2]).longValue();
                    Long received = ((Number) row[3]).longValue();

                    // Obtener día de la semana
                    String dayOfWeek = date.getDayOfWeek().toString();

                    return MessageMetricsByPeriodDTO.builder()
                            .period(date)
                            .channel(channelValue)
                            .messagesSent(sent)
                            .messagesReceived(received)
                            .dayOfWeek(dayOfWeek)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // --- GET MESSAGE METRICS BY WEEK OF MONTH ---
    /**
     * Obtiene métricas agrupadas por semana del mes actual con tasa de respuesta
     * calculada.
     * Agrupa mensajes por semanas dentro del mes actual (1-5 semanas).
     */
    public List<WeeklyMetricsDTO> getMetricsByWeek(Channel channel) {

        // Siempre usar el mes actual
        LocalDate now = LocalDate.now();
        LocalDate startDate = now.withDayOfMonth(1); // Primer día del mes actual
        LocalDate endDate = now.withDayOfMonth(now.lengthOfMonth()); // Último día del mes actual

        log.info("📊 Obteniendo métricas por semana del mes actual: {} a {}, channel={}",
                startDate, endDate, channel);

        // 1. Obtener métricas diarias y agrupar por semana del mes
        List<Object[]> dailyMetrics = messageRepository.findDailyMetricsForWeeklyGrouping(
                startDate, endDate, channel);

        Map<String, WeeklyMetricsBuilder> weeklyMetricsMap = new HashMap<>();

        for (Object[] row : dailyMetrics) {
            LocalDate date = (LocalDate) row[0];
            Long sent = ((Number) row[2]).longValue();
            Long received = ((Number) row[3]).longValue();

            int year = date.getYear();
            int month = date.getMonthValue();
            int weekOfMonth = getWeekOfMonth(date);
            String key = year + "-" + month + "-" + weekOfMonth;

            WeeklyMetricsBuilder builder = weeklyMetricsMap.getOrDefault(key,
                    new WeeklyMetricsBuilder(year, month, weekOfMonth));
            builder.addMessages(sent, received);
            weeklyMetricsMap.put(key, builder);
        }

        // 2. Obtener conversaciones iniciadas y agrupar por semana del mes
        List<Object[]> conversationsStartedData = messageRepository.findConversationsStartedDates(
                startDate, endDate, channel);

        Map<String, Long> conversationsStartedMap = new HashMap<>();
        for (Object[] row : conversationsStartedData) {
            LocalDate date = (LocalDate) row[0];
            int year = date.getYear();
            int month = date.getMonthValue();
            int weekOfMonth = getWeekOfMonth(date);
            String key = year + "-" + month + "-" + weekOfMonth;

            conversationsStartedMap.put(key,
                    conversationsStartedMap.getOrDefault(key, 0L) + 1);
        }

        // 3. Obtener conversaciones con respuesta y agrupar por semana del mes
        List<Object[]> conversationsWithResponseData = messageRepository.findConversationsWithResponseDates(
                startDate, endDate, channel);

        Map<String, Long> conversationsWithResponseMap = new HashMap<>();
        for (Object[] row : conversationsWithResponseData) {
            LocalDate date = (LocalDate) row[0];
            int year = date.getYear();
            int month = date.getMonthValue();
            int weekOfMonth = getWeekOfMonth(date);
            String key = year + "-" + month + "-" + weekOfMonth;

            conversationsWithResponseMap.put(key,
                    conversationsWithResponseMap.getOrDefault(key, 0L) + 1);
        }

        // 4. Construir DTOs con tasa de respuesta
        return weeklyMetricsMap.entrySet().stream()
                .map(entry -> {
                    String key = entry.getKey();
                    WeeklyMetricsBuilder builder = entry.getValue();

                    // Obtener conversaciones para esta semana
                    Long conversationsStartedCount = conversationsStartedMap.getOrDefault(key, 0L);
                    Long conversationsWithResponseCount = conversationsWithResponseMap.getOrDefault(key, 0L);

                    // Calcular tasa de respuesta
                    Double responseRate = 0.0;
                    if (conversationsStartedCount > 0) {
                        responseRate = (conversationsWithResponseCount.doubleValue() /
                                conversationsStartedCount.doubleValue()) * 100.0;
                    }

                    // Calcular fechas de inicio y fin de semana
                    LocalDate firstDayOfMonth = LocalDate.of(builder.year, builder.month, 1);
                    LocalDate weekStart = firstDayOfMonth
                            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                            .plusWeeks(builder.weekOfMonth - 1);
                    LocalDate weekEnd = weekStart.plusDays(6);

                    // Ajustar si la semana se sale del mes
                    LocalDate lastDayOfMonth = firstDayOfMonth.with(TemporalAdjusters.lastDayOfMonth());
                    if (weekStart.isBefore(firstDayOfMonth)) {
                        weekStart = firstDayOfMonth;
                    }
                    if (weekEnd.isAfter(lastDayOfMonth)) {
                        weekEnd = lastDayOfMonth;
                    }

                    return WeeklyMetricsDTO.builder()
                            .year(builder.year)
                            .month(builder.month)
                            .weekOfMonth(builder.weekOfMonth)
                            .weekStart(weekStart)
                            .weekEnd(weekEnd)
                            .totalMessagesSent(builder.totalSent)
                            .totalMessagesReceived(builder.totalReceived)
                            .conversationsStarted(conversationsStartedCount)
                            .conversationsWithResponse(conversationsWithResponseCount)
                            .responseRate(Math.round(responseRate * 100.0) / 100.0)
                            .build();
                })
                .sorted((a, b) -> {
                    // Ordenar por año, mes y semana del mes descendente
                    int yearCompare = b.getYear().compareTo(a.getYear());
                    if (yearCompare != 0)
                        return yearCompare;
                    int monthCompare = b.getMonth().compareTo(a.getMonth());
                    if (monthCompare != 0)
                        return monthCompare;
                    return b.getWeekOfMonth().compareTo(a.getWeekOfMonth());
                })
                .collect(Collectors.toList());
    }

    /**
     * Calcula la semana del mes (1-5) para una fecha dada.
     * La semana 1 comienza el primer lunes del mes.
     */
    private int getWeekOfMonth(LocalDate date) {
        LocalDate firstDayOfMonth = date.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate firstMonday = firstDayOfMonth.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

        if (date.isBefore(firstMonday)) {
            return 1; // Días antes del primer lunes son semana 1
        }

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(firstMonday, date);
        return (int) (daysBetween / 7) + 1;
    }

    /**
     * Clase auxiliar para construir métricas semanales del mes
     */
    private static class WeeklyMetricsBuilder {
        Integer year;
        Integer month;
        Integer weekOfMonth;
        Long totalSent = 0L;
        Long totalReceived = 0L;

        WeeklyMetricsBuilder(Integer year, Integer month, Integer weekOfMonth) {
            this.year = year;
            this.month = month;
            this.weekOfMonth = weekOfMonth;
        }

        void addMessages(Long sent, Long received) {
            this.totalSent += sent;
            this.totalReceived += received;
        }
    }
}
