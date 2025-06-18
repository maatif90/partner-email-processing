package com.priceline.partner.email.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResearchOutcomeRequest {
    private Metadata metadata;
    private PartnerResearchInstruction partnerResearchInstruction;

    @Data
    @AllArgsConstructor
    public static
    class Metadata {
        private Agent agent;
        private Contact contact;
    }

    @Data
    @AllArgsConstructor
    public static
    class Agent {
        private String kustomerUserId;
        private String emailAddress;
    }

    @Data
    @AllArgsConstructor
    public static
    class Contact {
        private String ivrContactId;
    }

    @Data
    @AllArgsConstructor
    public static
    class PartnerResearchInstruction {
        private String reservationId;
        private int categoryId;
        private int issueId;
        private String researchOutcomeType;
        private boolean isChargeBack;
    }
}

