package com.minoritycode;

import org.codehaus.plexus.util.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 *  Created by Matt Hall on 19/03/2015.
 *  minorityCode -- Trello backup application released under GNU General Public Licence v3.
 *  This software is open source and is free.  It may be copied and redistributed for free.
 *  Copyright 2015 Matthew Hall - minorityCode
 *
 *  This application is designed to run as a standalone java application wrapped in a windows executable.
 *  Please see config.properties to configure the application for mailer and proxy settings.
 *  Run the application and follow the instructions. A log file and error log and generated and can be
 mailed if using mailer and generated in the same working directory as the executable file.
 *
 *  The config file should be placed in the same directory as the executable file.
 *
 *  exe and jar built using maven and Launch4j
 It is recommended that the app is scheduled to run once a day using Windows scheduler
 To download all organisation boards please make sure you are an Admin for that organisation
 minimum system requirements :       at least Java jre 6 - 512mb PermSize
 internet connection
 recommended system requirements :   at least Java jre 6 - 1024mb PermSize
 *  This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>
 please report bugs to matthall103@gmail.com or visit the github repository @
 https://github.com/matthall103/TrelloBackup
 */

public class Application implements Runnable {

    private static String url = "https://api.trello.com/1/";
    private static String boardId;
    private static Lock lock;

    public static Lock lockErrorRep;
    public static Lock lockErrorLog;
    public static JSONObject report = new JSONObject();
    public static ArrayList<String> boards;
    public static Properties config = new Properties();
    public static Proxy proxy = null;
    public static  String charset = "UTF-8";
    public static DateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
    public static String date = df.format(Calendar.getInstance().getTime());
    public static DateFormat dfReport = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    public static String backupDate = dfReport.format(Calendar.getInstance().getTime());
    public static Integer boardCountSuc = 0;
    public static HashMap<String, String> errorBoards = new HashMap<String, String>();
    public static SimpleLogger logger = new SimpleLogger();
    public static Integer threadCounter = 0;
    public static String key = null;
    public static String token = null;

    public static File rootDir = null;
    public static File haysDir = null;
    public static File boardsDir = null;

    public static String workingDir = System.getProperty("user.dir");
    public static boolean manualOperation;

    public Application() {}

    @Override
    public void run() {

        boolean lockAcquired = false;
        do {
            //Lock and Block + remove file from file list.
            try {
                if (lock.tryLock(10, TimeUnit.MILLISECONDS)) {
                    boardId = boards.get(0);
                    boards.remove(0);
                    lockAcquired = true;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                logger.logLine(e.getMessage());
            } finally {
                //release lock
                if (lockAcquired) {
                    lock.unlock();
                    lockAcquired = false;
                }
            }

            BoardDownloader downloader = new BoardDownloader();
            downloader.downloadBoard(url,boardId);

            long threadInterval = Long.parseLong(config.getProperty("threadInterval"));
            try {
                Thread.sleep(threadInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
                logger.logLine(e.getMessage());
            }
        }
        while(boards.size() > 0);
        threadCounter++;

//        System.out.println(threadCounter+ " Threads completed");
        if(threadCounter == Integer.parseInt(config.getProperty("numberOfThreads"))){

            System.out.println(threadCounter+ " Threads completed");
            StringBuilder boardsMissed = new StringBuilder();

            Iterator it = errorBoards.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                System.out.println(pair.getKey() + " = " + pair.getValue());
                boardsMissed.append(pair.getKey() + " = " + pair.getValue() + "\n");
                it.remove(); // avoids a ConcurrentModificationException
            }

            try {
                report.put("boardNumSuccessful", boardCountSuc);
                report.put("boardsNotDownloaded", boardsMissed.toString());
            } catch (JSONException e) {
                e.printStackTrace();
                logger.logLine(e.getMessage());
            }

            logger.logger(report);
            logger.stopErrorLogger();

            manageBackups(rootDir);

            if(manualOperation)
            {
                System.out.println("Press enter to exit..........");
                try {
                    System.in.read();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.exit(0);

        }
    }

    public static void main(String[] args)
    {
        System.out.println("Trello Backup Application");

        File configFile = new File(workingDir+"\\config.properties");
        InputStream input = null;
        try {
            input = new FileInputStream(configFile);
            config.load(input);
            input.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        String workingDir = System.getProperty("user.dir");
        manualOperation = Boolean.parseBoolean(config.getProperty("manualOperation"));

        logger.startErrorLogger();
        setBackupDir();

        try {
            report.put("backupDate", backupDate);
        } catch (JSONException e) {
            e.printStackTrace();
            logger.logLine(e.getMessage());
        }

        lock = new ReentrantLock();
        lockErrorRep = new ReentrantLock();
        lockErrorLog = new ReentrantLock();

        Application.key = config.getProperty("trellokey");
        Application.token = config.getProperty("trellotoken");

        boolean useProxy = Boolean.parseBoolean(config.getProperty("useProxy"));

        boolean proxySet = true;

        if(useProxy) {
            proxySet = setProxy();
        }

//        GUI  swingContainerDemo = new GUI();
//        swingContainerDemo.showJPanelDemo();
        if(proxySet) {
            Credentials credentials = new Credentials();
            if (Application.key.isEmpty()) {
                Application.key = credentials.getKey();
            } else {
                Application.key = config.getProperty("trellokey");
            }
            if (token.isEmpty()) {
                Application.token = credentials.getToken();
            } else {
                Application.token = config.getProperty("trellotoken");
            }

            BoardDownloader downloader = new BoardDownloader();

            downloader.downloadMyBoard(url);
            boards = downloader.downloadOrgBoard(url);

            if (boards != null) {
                try {
                    report.put("boardNum", boards.size());
                } catch (JSONException e) {
                    e.printStackTrace();
                    logger.logLine(e.getMessage());
                }

                Integer numberOfThreads = Integer.parseInt(config.getProperty("numberOfThreads"));

                if(numberOfThreads==null)
                {
                    logger.logLine("error number of threads not set in config file");
                    if(manualOperation)
                    {
                        String message = "How many threads do you want to use (10) is average";
                        numberOfThreads = Integer.parseInt(Credentials.getInput(message));
                        Credentials.saveProperty("numberOfThreads", numberOfThreads.toString());
                    }
                    else{
                        if (Boolean.parseBoolean(config.getProperty("useMailer"))) {
                            Mailer mailer = new Mailer();
                            mailer.SendMail();
                        }
                        System.exit(-1);
                    }
                }

                ArrayList<Thread> threadList = new ArrayList<Thread>();
                for (int i = 0; i < numberOfThreads; i++) {
                    Thread thread = new Thread(new Application(), "BoardDownloadThread");
                    threadList.add(thread);
                    thread.start();
                }
            } else {
                //create empty report
                try {
                    report.put("boardsNotDownloaded", "99999");
                    report.put("boardNum", 0);
                    report.put("boardNumSuccessful", 0);
                } catch (JSONException e) {
                    e.printStackTrace();
                    logger.logLine(e.getMessage());
                }

                if (Boolean.parseBoolean(config.getProperty("useMailer"))) {
                    Mailer mailer = new Mailer();
                    mailer.SendMail();
                }

                logger.logger(report);
            }
        }
        else {
            //create empty report
            try {
                report.put("boardsNotDownloaded", "99999");
                report.put("boardNum", 0);
                report.put("boardNumSuccessful", 0);
            } catch (JSONException e) {
                e.printStackTrace();
                logger.logLine(e.getMessage());
            }

            if (Boolean.parseBoolean(config.getProperty("useMailer"))) {

                Mailer mailer = new Mailer();
                mailer.SendMail();
            }
        }
    }

    public static URLConnection makeConnection(String url, boolean useProxy){

        URLConnection connection = null;
        try {
            if(useProxy) {
                connection = new URL(url).openConnection(Application.proxy);
            }
            else
            {
                connection = new URL(url).openConnection();
            }
            connection.setRequestProperty("Accept-Charset", charset);
            connection.setConnectTimeout(30000);
        } catch (IOException e) {
            e.printStackTrace();
            Application.errorBoards.put(url,e.getMessage());
            logger.logLine(e.getMessage());
            return null;
        }

        return connection;
    }

    private static void manageBackups(File backupDir){

        System.out.println("Manage Backups");
        File[] list = backupDir.listFiles();
        ArrayList<File> backupFiles = new ArrayList<File>();
        for (File file: list)
        {
            if(file.isDirectory() && !file.getName().equals(".") && !file.getName().equals(".."))
            {
                backupFiles.add(file);
            }
        }

        Collections.sort(backupFiles);

        int numOfBackups = Integer.parseInt(config.getProperty("numOfBackups"));
        for(int i = 0; i < (backupFiles.size() - numOfBackups); i++)
        {
            File file = backupFiles.get(i);
//            backupFiles.remove(0);

            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(!file.isDirectory())
            {
                System.out.println(file.getName() + " has been deleted!");
            }else{
                System.out.println("Delete operation has failed for "+ file.getName());
            }
        }
    }

    public static void setBackupDir(){

        boolean useWorkingDir = Boolean.parseBoolean(Application.config.getProperty("useWorkingDir"));
        String backupFolderName = config.getProperty("backupFolderName");

        if(backupFolderName==null)
        {
            backupFolderName = "";
        }

        if (useWorkingDir) {
            rootDir = new File(workingDir + "\\JSON");
            haysDir = new File(workingDir + "\\JSON\\"+ backupFolderName +"_" + Application.date);
            boardsDir = new File(workingDir + "\\JSON\\"+backupFolderName + "_" + Application.date + "\\boards");
        }
        else {
            String backupPath = Application.config.getProperty("backupPath");
            rootDir = new File(backupPath);
            haysDir = new File(backupPath+"\\"+backupFolderName + "_"  + Application.date);
            boardsDir = new File(backupPath+"\\"+backupFolderName + "_"  + Application.date+"\\boards");
        }

        if (!rootDir.exists()) {
            rootDir.mkdir();
        }
        if (!haysDir.exists()) {
            haysDir.mkdir();
        }
        if (!boardsDir.exists()) {
            boardsDir.mkdir();
        }
    }

    private static boolean setProxy() {

        System.out.println("Setting proxy...");

        String host = null;

        host = config.getProperty("proxyHost").trim();

        if(host==null || host.isEmpty() || host.equals(""))
        {
            logger.logLine("error proxy host not set in config file");
            if(manualOperation)
            {
                String message = "Please enter your proxy Host address";
                host = Credentials.getInput(message).trim();
                Credentials.saveProperty("proxyHost", host);

                if(host.equals(null))
                {
                    System.exit(0);
                }
            }
            else { return false;}
        }

        String port = config.getProperty("proxyPort").trim();

        if(port==null || port.isEmpty())
        {
            logger.logLine("error proxy port not set in config file");
            if(manualOperation)
            {
                String message = "Please enter your proxy port";
                port = Credentials.getInput(message).trim();
                Credentials.saveProperty("proxyPort", port);

                if(port.equals(null))
                {
                    System.exit(0);
                }
            }
            else { return false;}
        }

        String user =  config.getProperty("proxyUser").trim();

        if(user==null || user.isEmpty())
        {
            logger.logLine("error proxy username not set in config file");
            if(manualOperation)
            {
                String message = "Please enter your proxy username";
                user = Credentials.getInput(message).trim();

                if(user.equals(null))
                {
                    System.exit(0);
                }

                Credentials.saveProperty("proxyUser", user);
            }
            else { return false;}
        }

        String password = config.getProperty("proxyPassword").trim();

        if(password==null || password.isEmpty())
        {
            logger.logLine("error proxy password not set in config file");
            if(manualOperation)
            {
                String message = "Please enter your proxy password";
                password = Credentials.getInput(message).trim();
                Credentials.saveProperty("proxyPassword", password);
                if(password.equals(null))
                {
                    System.exit(0);
                }
            }
            else { return false;}
        }

        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {

                if(getRequestorType() == RequestorType.PROXY) {
                    String prot = getRequestingProtocol().toLowerCase();
                    String host = System.getProperty(prot + ".proxyHost", config.getProperty("proxyHost"));
                    String port = System.getProperty(prot + ".proxyPort", config.getProperty("proxyPort"));
                    String user = System.getProperty(prot + ".proxyUser",config.getProperty("proxyUser"));
                    String password = System.getProperty(prot + ".proxyPassword", config.getProperty("proxyPassword"));

                    if (getRequestingHost().equalsIgnoreCase(host)) {
                        if (Integer.parseInt(port) == getRequestingPort()) {
                            // Seems to be OK.
                            return new PasswordAuthentication(user, password.toCharArray());
                        }
                    }
                }
                return null;
            }
        });

        System.setProperty("http.proxyPort", port);
        System.setProperty("http.proxyHost", host);
        System.setProperty("http.proxyUser", user);
        System.setProperty("http.proxyPassword",password);

        proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, Integer.parseInt(port)));

//        System.out.println(host+":"+port +" "+ user+":"+password);
        System.out.println("Proxy set");
        return true;
    }
}