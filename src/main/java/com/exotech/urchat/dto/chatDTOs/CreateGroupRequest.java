package com.exotech.urchat.dto.chatDTOs;

import lombok.Data;
import java.util.List;

@Data
public class CreateGroupRequest {
    private String name;
    private List<String> participants;
}