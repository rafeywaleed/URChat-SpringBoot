package com.exotech.urchat.dto.userDTOs;

import com.exotech.urchat.model.User;
import jakarta.persistence.Entity;
import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class UserDTOConvertor {

    public UserSearchDTO convertToSearchDTO(User user) {
        UserSearchDTO dto = new UserSearchDTO();
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        dto.setPfpIndex(user.getPfpIndex());
        dto.setPfpBg(user.getPfpBg());
        return dto;
    }

    public UserDTO convertToUserDTO(User user) {
        return new UserDTO(
                user.getUsername(),
                user.getFullName(),
                user.getBio(),
                user.getPfpIndex(),
                user.getPfpBg(),
                user.getJoinedAt()
        );
    }

    public UserPersonalDTO convertToUserPersonalDTO(User user) {
        return new UserPersonalDTO(
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getBio(),
                user.getPfpIndex(),
                user.getPfpBg(),
                user.getJoinedAt()
        );
    }


}
