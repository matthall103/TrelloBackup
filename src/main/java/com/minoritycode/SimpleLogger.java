package com.minoritycode;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Created by Matt Hall on 20/03/2015.
 */
public class SimpleLogger {

    String workingDir = System.getProperty("user.dir");
    PrintWriter outBoard = null;
    BufferedWriter boardOutputStream = null;

    public void logger(JSONObject report){

        DateFormat dfReport = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String backupDate = dfReport.format(Calendar.getInstance().getTime());

//        HashMap<String, String> errorLog,

        File file = new File(workingDir + "\\TrelloBackupLog.txt");

        if(!file.exists())
        {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileWriter fileWritterContact = null;
        try {
            fileWritterContact = new FileWriter(file, true);
        } catch (IOException e) {
            e.printStackTrace();
//            writeToErrorReport(id, e.getMessage());
            return;
        }
        BufferedWriter boardOutputStream = new BufferedWriter(fileWritterContact);

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
//                writeToErrorReport(id, e.getMessage());
                return;
            }
        }

        StringBuilder logLine = new StringBuilder();

        String backUpdate = null;
        String boardsNotDownloaded = null;
        Integer boardNum = null;
        Integer boardNumSuccessful = null;

        try {
            boardsNotDownloaded = (String) report.get("boardsNotDownloaded");
            backUpdate = (String) report.get("backupDate");
            boardNum = (Integer) report.get("boardNum");
            boardNumSuccessful = (Integer) report.get("boardNumSuccessful");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(boardsNotDownloaded.isEmpty())
        {
            boardsNotDownloaded = "None";
        }

        logLine.append(backUpdate +" ::: ");
        logLine.append("number of Boards = "+boardNum +" ::: ");
        logLine.append("number of Boards Successful = "+boardNumSuccessful +" ::: ");
        logLine.append("Error List = "+boardsNotDownloaded);

        PrintWriter outBoard = new PrintWriter(boardOutputStream);
        outBoard.println(logLine.toString());

        try {
            outBoard.flush();
            boardOutputStream.flush();
            outBoard.close();
            boardOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
//            writeToErrorReport(id, e.getMessage());
            return;
        }
    }

    public void startErrorLogger(){

        File file = new File(workingDir + "\\TrelloBackupErrorLog.txt");

        FileWriter fileWritterContact = null;
        try {
            fileWritterContact = new FileWriter(file, true);
        } catch (IOException e) {
            e.printStackTrace();
//            writeToErrorReport(id, e.getMessage());
            return;
        }
        boardOutputStream = new BufferedWriter(fileWritterContact);

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
//                writeToErrorReport(id, e.getMessage());
                return;
            }
        }
         outBoard = new PrintWriter(boardOutputStream);
    }

    public void stopErrorLogger()
    {
        try {
            outBoard.flush();
            boardOutputStream.flush();
            outBoard.close();
            boardOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();;
        }

        if(Boolean.parseBoolean(Application.config.getProperty("useMailer")))
        {
            Mailer mailer = new Mailer();
//            JSONObject report = new JSONObject();

            BufferedReader reader = null;
            try {
                reader = new BufferedReader( new FileReader(new File(workingDir + "\\TrelloBackupErrorLog.txt")));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            String         line = null;
            StringBuilder  stringBuilder = new StringBuilder();
            String         ls = System.getProperty("line.separator");

            try {
                while( ( line = reader.readLine() ) != null ) {

                    if(line.substring(0,10).equals(Application.backupDate.substring(0,10))) {
                        stringBuilder.append(line);
                        stringBuilder.append(ls);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Application.report.put("errorLog", stringBuilder.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mailer.SendMail();
        }

    }

    public void logLine(String line){

        boolean lockAcquired = false;
        //Lock and Block
        try {
            if (Application.lockErrorLog.tryLock(10, TimeUnit.MILLISECONDS)) {

                DateFormat dfReport = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                String backupDate = dfReport.format(Calendar.getInstance().getTime());

                StringBuilder logLine = new StringBuilder();
                logLine.append(backupDate +" ::: "+ line);
                outBoard.println(logLine.toString());
                lockAcquired = true;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //release lock
            if (lockAcquired) {
                Application.lockErrorLog.unlock();
            }
        }
    }
}
