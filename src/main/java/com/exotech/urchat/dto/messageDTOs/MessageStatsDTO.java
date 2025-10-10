package com.exotech.urchat.dto.messageDTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageStatsDTO {
    private long totalMessages;
    private long recentMessages; // Last 30 days
    private long oldMessages; // Older than 30 days
}