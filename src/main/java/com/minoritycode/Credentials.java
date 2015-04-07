package com.minoritycode;

import javax.swing.*;
import java.io.*;
import java.util.Properties;

/**
 * Created by Matt Hall on 21/03/2015.
 */
public class Credentials {

    public static String workingDir = System.getProperty("user.dir");

    public String getKey(){

        System.out.println("Getting Trello Key");
        Runtime rt = Runtime.getRuntime();
        String url = "https://trello.com/1/appKey/generate";
        try {
            rt.exec( "rundll32 url.dll,FileProtocolHandler " + url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String message = "Please enter your Trello Key";
        String key = getInput(message);
        saveProperty("trellokey",key);
        return key;
    }

    public String getToken(){

        System.out.println("Getting Trello Token");
        String key = Application.key;
        Runtime rt = Runtime.getRuntime();
        String url = "https://trello.com/1/authorize?key="+key+"&name=My+Trello+Backup&expiration=never&response_type=token";
        try {
            rt.exec( "rundll32 url.dll,FileProtocolHandler " + url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String message = "Please enter your Trello Token";
        String token = getInput(message);
        saveProperty("trellotoken", token);
        return token;
    }

    public static String getInput(String message)
    {
        JFrame frame = new JFrame("Trello Backup");
        // prompt the user to enter their name
        String response = JOptionPane.showInputDialog(frame, message);
        return response.trim();
    }


    public static String saveProperty(String name, String value)
    {
        System.out.println("Saving " +name);
        Properties prop = new Properties();
        Properties propToDelete = new Properties();
        OutputStream output = null;
        OutputStream outputDel = null;
        InputStream input = null;

        try {
            input = new FileInputStream(workingDir+"\\config.properties");
            propToDelete.load(input);
            input.close();

            outputDel = new FileOutputStream(workingDir+"\\config.properties");

            propToDelete.remove(name);
            propToDelete.store(outputDel, null);
            outputDel.flush();
            outputDel.close();

            output = new FileOutputStream(workingDir+"\\config.properties", true);

            prop.setProperty(name, value);
            prop.store(output, null);

            output.flush();

        } catch (IOException io) {
            io.printStackTrace();
            Application.logger.logLine(io.getMessage());
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Application.logger.logLine(e.getMessage());
                }
            }
        }
        return value;
    }
}
