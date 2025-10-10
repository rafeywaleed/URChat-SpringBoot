    package com.exotech.urchat.dto.webSocketDTOs;

    import lombok.Data;

    @Data
    public class MessageConfirmation {
        private Long messageId;
        private String status;

        public MessageConfirmation(Long messageId, String status) {
            this.messageId = messageId;
            this.status = status;
        }
    }