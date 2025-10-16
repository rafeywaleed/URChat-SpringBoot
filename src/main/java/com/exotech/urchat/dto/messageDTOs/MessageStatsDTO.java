package com.exotech.urchat.dto.messageDTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageStatsDTO {
    private long totalMessages;
    private long recentMessages; // Last 7 days
    private long oldMessages; // Older than 7 days
}