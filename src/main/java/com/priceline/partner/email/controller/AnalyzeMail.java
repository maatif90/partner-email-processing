package com.priceline.partner.email.controller;




import java.io.IOException;
import java.util.Properties;

import javax.mail.*;
import javax.mail.search.AndTerm;
import javax.mail.search.BodyTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SubjectTerm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.priceline.partner.email.DTO.ResearchOutcomeRequest;
import com.priceline.partner.email.DTO.ResearchOutcomeRequest.*;

@RestController
    public class AnalyzeMail {

    @Autowired
    private RestTemplate restTemplate;

    private static final String PROCESS_OUTCOME_URL = "https://guse4-carespcimidtiergw-qaa.dqs.pcln.com/c3cancelsvcs/process-partner-research-outcome";

    private final ChatClient chatClient;
        private static final String IMAP_HOST = "imap.gmail.com";
        private static final String EMAIL = "mohd.aatif90.bkp@gmail.com";
        private static final String APP_CRED = "essn wiek adgv ****";
        private static final String ANALYSIS_PROMPT = """
        Analyze the following email content and determine if a car was used or not.
        Respond with only one of these exact words:
        CAR_USED: if the text clearly indicates a car was used
        CAR_NOT_USED: if the text clearly indicates no car was used
        UNDECIDED: if you cannot determine with certainty

        Email content:
        """;

        public AnalyzeMail(ChatClient.Builder chatClientBuilder) {
            this.chatClient = chatClientBuilder.build();
        }

        @GetMapping("/analyze-mail-v2")
        public String analyzeMail() {
            try (Store store = connectToMailStore()) {
                Folder inbox = openInbox(store);
                Message[] messages = searchMessages(inbox);
                 String result = processMessages(messages);
                inbox.close(false);
                return result;
            } catch (MessagingException | IOException e) {
                throw new RuntimeException("Error processing emails", e);
            }
        }

        private Store connectToMailStore() throws MessagingException {
            Properties props = new Properties();
            props.setProperty("mail.store.protocol", "imaps");
            Session session = Session.getInstance(props, null);
            Store store = session.getStore();
            store.connect(IMAP_HOST, EMAIL, APP_CRED);
            return store;
        }

        private Folder openInbox(Store store) throws MessagingException {
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);
            return inbox;
        }

        private Message[] searchMessages(Folder inbox) throws MessagingException {
            SearchTerm searchTerm = createSearchCriteria();
            return inbox.search(searchTerm);
        }

        private SearchTerm createSearchCriteria() {
            SearchTerm unreadTerm = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            SearchTerm subjectTerm = new SubjectTerm("Priceline.com Customer Service - Travel Itinerary for Trip");
        //    SearchTerm bodyTerm = new BodyTerm("car");
            return new AndTerm(new SearchTerm[]{unreadTerm, subjectTerm});
        }

        private String processMessages(Message[] messages) throws MessagingException, IOException {
            String analysis = null;
            for (Message message : messages) {
                String subject = extractSubject(message);
                String tripNumber = extractTripNumber(subject);
                String content = extractMessageContent(message);
                if (content != null) {
                     analysis = analyzeContent(content);
                     if(analysis.equals("CAR_USED") || analysis.equals("CAR_NOT_USED")){
                         processCarUse(tripNumber, analysis);
                     }else {
                         markAsUnread(message); // Mark the message as unread
                     }
                    System.out.println("Analysis result: " + analysis);
                }
            }
            return analysis;
        }

        private String extractMessageContent(Message message) throws MessagingException, IOException {
            if (message.getContent() instanceof Multipart) {
                return extractMultipartContent((Multipart) message.getContent());
            }
            return message.getContent().toString();
        }

        private String extractMultipartContent(Multipart multipart) throws MessagingException, IOException {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (bodyPart.getContentType().toLowerCase().startsWith("text/plain")) {
                    return bodyPart.getContent().toString();
                }
            }
            return null;
        }


        private String analyzeContent(String content) {
            String response = chatClient.prompt()
                .user(ANALYSIS_PROMPT + content)
                .call()
                .content()
                .trim();

            return switch (response.toUpperCase()) {
                case "CAR_USED" -> "CAR_USED";
                case "CAR_NOT_USED" -> "CAR_NOT_USED";
                default -> "UNDECIDED";
            };
        }

    private String extractTripNumber(String subject) {
        if (subject == null) return null;
        int hashIndex = subject.indexOf('#');
        if (hashIndex != -1) {
            return subject.substring(hashIndex + 1).trim();
        }
        return null;
    }

    private void markAsUnread(Message message) throws MessagingException {
        message.setFlags(new Flags(Flags.Flag.SEEN), false);
    }
    private String extractSubject(Message message) throws MessagingException {
        return message.getSubject();
    }

    void processCarUse(String offerNumber,String researchOutcomeType){
        ResearchOutcomeRequest request = new ResearchOutcomeRequest(
            new Metadata(
                new Agent("6666eb755cd0b80978a00507", "mohammed.aatif@priceline.com"),
                new Contact("undefined")
            ),
            new PartnerResearchInstruction(
                offerNumber,
                7,
                131,
                researchOutcomeType,
                true
            )
        );

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                PROCESS_OUTCOME_URL,
                request,
                String.class
            );
            System.out.println("Response status: " + response.getStatusCode());
        } catch (Exception e) {
            throw new RuntimeException("Failed to process car not used outcome", e);
        }

    }
    }

