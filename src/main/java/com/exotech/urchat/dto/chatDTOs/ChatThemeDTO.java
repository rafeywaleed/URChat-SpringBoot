package com.exotech.urchat.dto.chatDTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatThemeDTO {

    private int themeIndex;
    private Boolean isDark;

}
