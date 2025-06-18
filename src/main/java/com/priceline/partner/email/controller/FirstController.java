package com.priceline.partner.email.controller;

import java.io.IOException;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.AndTerm;
import javax.mail.search.BodyTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SubjectTerm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FirstController {

    private final ChatClient chatClient;
    private static final String ANALYSIS_PROMPT = """
        Analyze the following email content and determine if a car was used or not.
        Respond with only one of these exact words:
        CAR_USED: if the text clearly indicates a car was used
        CAR_NOT_USED: if the text clearly indicates no car was used
        UNDECIDED: if you cannot determine with certainty
        
        Email content:
        """;

    public FirstController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/analyze-mail")
    public void analyzeMail() {
        // This method is a placeholder for the mail analysis logic.
        // You can implement the actual logic here to analyze emails.
        System.out.println("Analyzing mail...");
        String userInput = "Analyze my emails and provide insights";




        {
            try {

                Properties props = new Properties();
                props.setProperty("mail.store.protocol", "imaps");
                Session session = Session.getInstance(props, null);
                Store store = session.getStore();
                store.connect("imap.gmail.com", "mohd.aatif90.bkp@gmail.com", "essn wiek adgv nbfi");
                Folder inbox = store.getFolder("INBOX");
                inbox.open(Folder.READ_WRITE);

                System.out.println("Connected successfully!");

               // SearchTerm searchTerm = new AndTerm(new SubjectTerm("chargeback"), new BodyTerm("car"));
                SearchTerm unreadTerm = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
                SearchTerm subjectTerm = new SubjectTerm("chargeback");
                SearchTerm bodyTerm = new BodyTerm("car");
                SearchTerm searchTerm = new AndTerm(new SearchTerm[]{unreadTerm, subjectTerm, bodyTerm});
                Message[] messages = inbox.search(searchTerm);
                System.out.println("Message count: " + messages.length);
                for (Message message : messages) {
                    if (message.getContent() instanceof Multipart) {
                        Multipart mp = (Multipart) message.getContent();
                        for (int i = 0; i < mp.getCount(); i++) {
                            BodyPart bodyPart = mp.getBodyPart(i);
                            if (bodyPart.getContentType().toLowerCase().startsWith("text/plain")) {
                                System.out.println("Content: " + bodyPart.getContent());
                                String content = bodyPart.getContent().toString();

                                String analysis = analyzeContent(content);
                                System.out.println(analysis);

                            }
                        }
                    } else {
                        System.out.println("Content: " + message.getContent());
                    }
                }

                inbox.close(false);
                store.close();
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

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
}
