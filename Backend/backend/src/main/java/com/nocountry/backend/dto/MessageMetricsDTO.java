package com.nocountry.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageMetricsDTO {

    private Long totalMessagesSent;

    private Long totalMessagesReceived;

    private Long conversationsWithResponse;

    private Long totalConversationsStarted;

    private Double responseRate;

}
