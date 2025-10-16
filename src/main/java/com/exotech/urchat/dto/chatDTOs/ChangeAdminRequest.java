package com.exotech.urchat.dto.chatDTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeAdminRequest {

    private String adminUsername;
    private String candidateUsername;
    private String chatId;

}
