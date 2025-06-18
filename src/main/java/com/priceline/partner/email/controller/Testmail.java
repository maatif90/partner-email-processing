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

public class Testmail {

    public static void main(String[] args) {
        try {

            Properties props = new Properties();
            props.setProperty("mail.store.protocol", "imaps");
            Session session = Session.getInstance(props, null);
            Store store = session.getStore();
            store.connect("imap.gmail.com", "mohd.aatif90.bkp@gmail.com", "essn wiek adgv nbfi");
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            System.out.println("Connected successfully!");

            SearchTerm searchTerm = new AndTerm(new SubjectTerm("chargeback"), new BodyTerm("car"));
            Message[] messages = inbox.search(searchTerm);
            System.out.println("Message count: " + messages.length);
            for (Message message : messages) {
                if (message.getContent() instanceof Multipart) {
                    Multipart mp = (Multipart) message.getContent();
                    for (int i = 0; i < mp.getCount(); i++) {
                        BodyPart bodyPart = mp.getBodyPart(i);
                        if (bodyPart.getContentType().toLowerCase().startsWith("text/plain")) {
                            System.out.println("Content: " + bodyPart.getContent());
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
