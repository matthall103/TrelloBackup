package com.minoritycode;

import org.json.JSONException;
import org.json.JSONObject;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Properties;

/**
 * Created by Matt Hall on 27/02/2015.
 */
public class Mailer {

    private static final String SMTP_AUTH_USER = Application.config.getProperty("mailUser");
    private static final String SMTP_AUTH_PWD  = Application.config.getProperty("mailPassword");

    public void SendMail()
    {
        String from = Application.config.getProperty("mailFrom");
        String to = Application.config.getProperty("mailTo");

        // Recipient's email ID needs to be mentioned.
        String[] parts = to.split("@");
        //Get name parts from name
        String[] nameParts = parts[0].split("\\.");
        //get firstname and capitalise first letter
        String firstName = nameParts[0].substring(0, 1).toUpperCase() + nameParts[0].substring(1);

        String host = Application.config.getProperty("mailHost");

        if(host==null || host.isEmpty())
        {
            Application.logger.logLine("error mail host not set in config file");
            if(Application.manualOperation)
            {
                String message = "Please enter your mail Host address";
                host = Credentials.getInput(message);
                Credentials.saveProperty("mailHost", host);

                if(host.equals(null))
                {
                    System.exit(0);
                }
            }
            else { return; }
        }

        String port = Application.config.getProperty("mailPort");

        if(port==null || port.isEmpty())
        {
            Application.logger.logLine("error mail port not set in config file");
            if(Application.manualOperation)
            {
                String message = "Please enter your mail port";
                port = Credentials.getInput(message);
                Credentials.saveProperty("mailPort", port);

                if(port.equals(null))
                {
                    System.exit(0);
                }
            }
            else { return; }
        }

        String requireAuth = Application.config.getProperty("mailAuth");

        if(requireAuth==null || requireAuth.isEmpty())
        {
            Application.logger.logLine("error use mail Auth not set in config file");
            if(Application.manualOperation)
            {
                String message = "Please enter if mail auth is required (true \\ false)";
                requireAuth = Credentials.getInput(message);
                Credentials.saveProperty("requireAuth", requireAuth);

                if(requireAuth.equals(null))
                {
                    System.exit(0);
                }
            }
            else { return; }
        }

        // Get system properties
        Properties properties = System.getProperties();
        // Setup mail server
        properties.setProperty(host, host);

        Properties props = new Properties();
        props.put("mail.smtp.auth", requireAuth);
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        // Get the default Session object.


        Session session;
        if(Boolean.parseBoolean(requireAuth)) {
            Authenticator auth = new SMTPAuthenticator();
            session = Session.getDefaultInstance(props, auth);
        }
        else{
            session = Session.getDefaultInstance(props);
        }
        try{
            // Create a default MimeMessage object.
            MimeMessage message = new MimeMessage(session);
            // Set From: header field of the header.
            message.setFrom(new InternetAddress(from));
            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            // Set Subject: header field
            String mailSubject =  Application.config.getProperty("mailSubject");

            if(requireAuth==null || requireAuth.isEmpty())
            {
                Application.logger.logLine("error use mail Subject line not set in config file");
            }
            else {
                message.setSubject(mailSubject);
            }
            // Create the message part
            BodyPart messageBodyPart = new MimeBodyPart();

            String backUpdate = null;
            String errorLog = null;
            String boardsNotDownloaded = null;
            Integer boardNum = null;
            Integer boardNumSuccessful = null;

            try {
                boardsNotDownloaded = (String) Application.report.get("boardsNotDownloaded");
                backUpdate = (String) Application.report.get("backupDate");
                errorLog = (String) Application.report.get("errorLog");
                boardNum = (Integer) Application.report.get("boardNum");
                boardNumSuccessful = (Integer) Application.report.get("boardNumSuccessful");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if(boardsNotDownloaded.isEmpty() || boardsNotDownloaded==null)
            {
                boardsNotDownloaded = "None";
            }

            if(errorLog.isEmpty()  || errorLog.equals("") || errorLog==null)
            {
                errorLog = "No Errors";
            }

            if(boardNum==null) {
                boardNum =0;
            }

            if(boardNumSuccessful==null) {
                boardNumSuccessful =0;
            }

            String all= "none";
            if(boardNum==99999) {
                all = "ALL";
            }

            String bns;
            String bn;
            if(all.equals("ALL"))
            {
                bns=all;
                bn=all;
            }
            else
            {
                bns = boardNumSuccessful.toString();
                bn = boardNum.toString();
            }

            StringBuilder sb = new StringBuilder();

            sb.append("Hello " + firstName);
            sb.append("\n\nHalo Back up report\n\n");
            sb.append("Backup Date :" + backUpdate + "\n");
            sb.append("Number of Boards : " + bn + "\n");
            sb.append("Number of Boards successfully downloaded : " + bns + ",\n");
            sb.append("\nBoards that weren't downloaded : " + boardsNotDownloaded + ",\n");
            sb.append("\nError Log : " + errorLog + ",\n");
            sb.append("\nKind regards,\n\n");
            sb.append("TrelloBackUp\n");

            messageBodyPart.setText(sb.toString());
            System.out.println(sb.toString());

            // Create a multipart message
            Multipart multipart = new MimeMultipart();

            // Set text message part
            multipart.addBodyPart(messageBodyPart);

            message.setContent(multipart);

            // Send message
            Transport.send(message);
            System.out.println("Sent message successfully....");
        }catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }


    private class SMTPAuthenticator extends javax.mail.Authenticator {
        public PasswordAuthentication getPasswordAuthentication() {
            String username = SMTP_AUTH_USER;
            String password = SMTP_AUTH_PWD;
            return new PasswordAuthentication(username, password);
        }
    }
}