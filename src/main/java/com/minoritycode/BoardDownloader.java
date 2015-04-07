package com.minoritycode;

import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


/**
 * Created by Matt Hall on 19/03/2015.
 */
public class BoardDownloader {

    String workingDir = System.getProperty("user.dir");
    String charset = "UTF-8";  // Or in Java 7 and later, use the constant: java.nio.charset.StandardCharsets.UTF_8.name()
    String param1 = Application.key;
    String param2 = Application.token;
    String ext = null;
    String query = null;

    boolean backupClosedBoards = Boolean.parseBoolean(Application.config.getProperty("backupClosedBoards"));

    public void downloadMyBoard(String url) {

        try {
            ext = "members/me/boards";
            query = String.format("&key=%s&token=%s",
                    URLEncoder.encode(param1, charset),
                    URLEncoder.encode(param2, charset));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            writeToErrorReport("topBoard", e.getMessage());
            Application.logger.logLine("topBoard ::: "+ e.getMessage());
            return;
        }

//        System.out.println("url ::: " +(url+"?"+query));
        InputStream response = null;
        JSONParser jsonParser = new JSONParser();
        URLConnection connection = null;
        try {
            connection = Application.makeConnection(url + ext + "?" + query, Boolean.parseBoolean(Application.config.getProperty("useProxy")));
            response = connection.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            writeToErrorReport("topBoard", e.getMessage());
            Application.logger.logLine("topBoard ::: "+ e.getMessage());
            return;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(response));
        StringBuilder sb = new StringBuilder();

//        JSONObject orgBard = new JSONObject();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
//                orgBard.writeJSONString(writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
            writeToErrorReport("topBoard", e.getMessage());
            Application.logger.logLine("topBoard ::: "+ e.getMessage());
            return;
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
                writeToErrorReport("topBoard", e.getMessage());
                Application.logger.logLine("topBoard ::: "+ e.getMessage());
            }
        }

        JSONArray orgBoardArray = new JSONArray();
        JSONObject orgBoard = new JSONObject();
        try {
            orgBoardArray = (JSONArray) jsonParser.parse(sb.toString());

            if(orgBoardArray.size()>0) {
                orgBoard = (JSONObject) orgBoardArray.get(0);
            }
        } catch (ParseException e) {
            e.printStackTrace();
            writeToErrorReport("topBoard", e.getMessage());
            Application.logger.logLine("topBoard ::: "+ e.getMessage());
            return;
        }

        printJSON(orgBoard.toString());

    }

    public static org.json.JSONObject printJSON(String jsonString) {

        org.json.JSONObject printer = null;
        try {
            printer = new org.json.JSONObject(jsonString);
//            System.out.println("url = " +url+ext+query);
//            System.out.println(printer.toString(4));
        } catch (JSONException e) {
            e.printStackTrace();
            writeToErrorReport("JSONPRINT", e.getMessage());
            Application.logger.logLine("JSONPRINT ::: "+ e.getMessage());
        }

        return printer;
    }

    public ArrayList<String> downloadOrgBoard(String url) {

        try {
            ext = "members/me/organizations";
            query = String.format("&key=%s&token=%s",
                    URLEncoder.encode(param1, charset),
                    URLEncoder.encode(param2, charset));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            writeToErrorReport("orgBoard", e.getMessage());
            Application.logger.logLine("orgBoard ::: "+ e.getMessage());
            return null;
        }

//        System.out.println("url ::: " +(url+"?"+query));
        InputStream response = null;
        JSONParser jsonParser = new JSONParser();
        URLConnection connection = null;
        try {
            connection = Application.makeConnection(url + ext + "?" + query, Boolean.parseBoolean(Application.config.getProperty("useProxy")));
            response = connection.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            writeToErrorReport("orgBoard", e.getMessage());
            Application.logger.logLine("orgBoard ::: "+ e.getMessage());
            return null;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(response));
        StringBuilder sb = new StringBuilder();

//        JSONObject orgBard = new JSONObject();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            writeToErrorReport("orgBoard", e.getMessage());
            Application.logger.logLine("orgBoard ::: "+ e.getMessage());
            return null;
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
                writeToErrorReport("orgBoard", e.getMessage());
                Application.logger.logLine("orgBoard ::: "+ e.getMessage());
            }
        }

        JSONArray orgBoardArray = new JSONArray();
        JSONObject orgBoard = new JSONObject();
        try {
            orgBoardArray = (JSONArray) jsonParser.parse(sb.toString());

            if(orgBoardArray.size() > 0) {
                orgBoard = (JSONObject) orgBoardArray.get(0);
            }
        } catch (ParseException e) {
            e.printStackTrace();
            writeToErrorReport("orgBoard", e.getMessage());
            Application.logger.logLine("orgBoard ::: "+ e.getMessage());
            return null;
        }

        org.json.JSONObject printer = printJSON(orgBoard.toString());

        String orgId = null;
        try {
            orgId = (String) printer.get("name");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ArrayList<String> boards = new ArrayList<String>();

        boards = (ArrayList<String>) orgBoard.get("idBoards");

        org.json.JSONArray members = downloadMemberList(url, orgId);

        try {
            printer.put("members", members);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String orgName = Application.config.getProperty("orgName");

        if(orgName==null || orgName.isEmpty())
        {
            orgName = "Organisation Board";
        }

        System.out.println("saving org board " +orgName);
        saveBoard(orgName, printer, false, 1);

        return boards;
    }

    public org.json.JSONArray downloadMemberList(String url, String id) {

        try {
            ext = "organizations/"+id;
            query = String.format("members=all&member_fields=username,fullName&key=%s&token=%s",
                    URLEncoder.encode(param1, charset),
                    URLEncoder.encode(param2, charset));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            writeToErrorReport("orgBoard", e.getMessage());
            Application.logger.logLine("orgBoard ::: "+ e.getMessage());
            return null;
        }

//        System.out.println("url ::: " +(url+"?"+query));
        InputStream response = null;
        JSONParser jsonParser = new JSONParser();
        URLConnection connection = null;
        try {
            connection = Application.makeConnection(url + ext + "?" + query, Boolean.parseBoolean(Application.config.getProperty("useProxy")));
            response = connection.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            writeToErrorReport("members", e.getMessage());
            Application.logger.logLine("members ::: "+ e.getMessage());
            return null;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(response));
        StringBuilder sb = new StringBuilder();

//        JSONObject orgBard = new JSONObject();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            writeToErrorReport("members", e.getMessage());
            Application.logger.logLine("members ::: "+ e.getMessage());
            return null;
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
                writeToErrorReport("members", e.getMessage());
                Application.logger.logLine("members ::: "+ e.getMessage());
            }
        }

        JSONArray membersBoardArray = new JSONArray();
        JSONObject membersBoard = new JSONObject();
        try {
            membersBoard = (JSONObject) jsonParser.parse(sb.toString());

            if(membersBoardArray.size() > 0) {
                membersBoard = (JSONObject) membersBoardArray.get(0);
            }
        } catch (ParseException e) {
            e.printStackTrace();
            writeToErrorReport("members", e.getMessage());
            Application.logger.logLine("members ::: "+ e.getMessage());
            return null;
        }

        org.json.JSONObject printer = printJSON(membersBoard.toString());

        org.json.JSONArray members = null;
        try {
            members = printer.getJSONArray("members");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return members;
    }


    public void downloadBoard(String url, String id) {

        ext = "boards/" + id;
        String param3 = "all";
        String param4 = "1000";

        try {
            query = String.format("&key=%s&token=%s",
                    URLEncoder.encode(param1, charset),
                    URLEncoder.encode(param2, charset));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            writeToErrorReport(id, e.getMessage());
            Application.logger.logLine(id+" ::: "+ e.getMessage());
            return;
        }
//        try {
                query = "actions=all&actions_limit=1000&cards=all&labels=all&checklists=all&lists=all&members=all&card_attachments=true&member_fields=all&fields=all&key="+param1+"&token="+param2;

//        &checklists=all&card_attachments=all

//              query = String.format("actions=%s&actions_limit=%s&cards=%s&labels=$s&checklists=%s&lists=%s&members=%s&member_fields=%s&checklists=%s&fields=%s&key=%s&token=%s",
//                URLEncoder.encode(param3, charset),
//                URLEncoder.encode(param4, charset),
//                URLEncoder.encode(param3, charset),
//                URLEncoder.encode(param3, charset),
//                URLEncoder.encode(param3, charset),
//                URLEncoder.encode(param3, charset),
//                URLEncoder.encode(param3, charset),
//                URLEncoder.encode(param3, charset),
//                URLEncoder.encode(param3, charset),
//                URLEncoder.encode(param3, charset),
//                URLEncoder.encode(param3, charset),
//                URLEncoder.encode(param1, charset),
//                URLEncoder.encode(param2, charset));
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//            writeToErrorReport(id, e.getMessage());
//            Application.logger.logLine(id+" ::: "+ e.getMessage());
//            return;
//        }

//        System.out.println("url ::: " +(url+"?"+query));
        InputStream response = null;
        JSONParser jsonParser = new JSONParser();
        URLConnection connection = null;
        try {
//            System.out.println("about to download board "+id);
            connection = Application.makeConnection(url + ext + "?" + query, Boolean.parseBoolean(Application.config.getProperty("useProxy")));
            response = connection.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            writeToErrorReport(id, e.getMessage());
            Application.logger.logLine(id+" ::: "+ e.getMessage());
            return;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(response));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            writeToErrorReport(id, e.getMessage());
            Application.logger.logLine(id+" ::: "+ e.getMessage());
            return;
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
                writeToErrorReport(id, e.getMessage());
                Application.logger.logLine(id+" ::: "+ e.getMessage());
            }
        }

        JSONObject board = new JSONObject();
        try {
            board = (JSONObject) jsonParser.parse(sb.toString());
        } catch (ParseException e) {
            e.printStackTrace();
            writeToErrorReport(id, e.getMessage());
            Application.logger.logLine(id+" ::: "+ e.getMessage());
            return;
        }

        org.json.JSONObject printer = printJSON(board.toString());

        String boardname = null;
        boolean closed = false;
        try {
            boardname = printer.getJSONArray("actions").getJSONObject(0).getJSONObject("data").getJSONObject("board").getString("name");
            if (!backupClosedBoards) {
                closed = printer.getJSONArray("actions").getJSONObject(0).getJSONObject("data").getJSONObject("board").getBoolean("name");
            } else {
                closed = false;
            }

        } catch (JSONException e) {
            e.printStackTrace();
            writeToErrorReport(id, e.getMessage());
            Application.logger.logLine(id+" ::: "+ e.getMessage());
            return;
        }

        saveBoard(boardname, printer, closed, 2);
        if(!closed) {
            System.out.println("boardname " + boardname + " downloaded successfully!");
            Application.boardCountSuc++;
        }
    }

    public void saveBoard(String boardname, org.json.JSONObject printer, boolean closed, Integer type){

        boardname = boardname.replaceAll(" ", "_");
        boardname = boardname.replaceAll("/", "_");
        boardname = boardname.replaceAll("\\\\", "_");
        boardname = boardname.replaceAll("<", "_");
        boardname = boardname.replaceAll(">", "_");
        boardname = boardname.replaceAll(":", "_");
        boardname = boardname.replaceAll("#", "");
        boardname = boardname.replaceAll("\\)", "");
        boardname = boardname.replaceAll("\\(", "");
        boardname = boardname.replaceAll("\\?", "");
        boardname = boardname.replaceAll("\\[", "");
        boardname = boardname.replaceAll("]", "");

        String filename = boardname + ".json";

        if (!closed) {

            File file = null;
            if(type==1) {
                file = new File(Application.haysDir + "\\" + filename);
            }
            else{
                file = new File(Application.boardsDir + "\\" + filename);
            }

            //save json
            FileWriter fileWritterContact = null;
            try {
                fileWritterContact = new FileWriter(file);
            } catch (IOException e) {
                e.printStackTrace();
                writeToErrorReport(boardname, e.getMessage());
                Application.logger.logLine(boardname+" ::: "+ e.getMessage());
                return;
            }
            BufferedWriter boardOutputStream = new BufferedWriter(fileWritterContact);

            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    writeToErrorReport(boardname, e.getMessage());
                    Application.logger.logLine(boardname+" ::: "+ e.getMessage());
                    return;
                }
            }

            PrintWriter outBoard = new PrintWriter(boardOutputStream);
            outBoard.println(printer.toString());

            try {
                outBoard.flush();
                boardOutputStream.flush();
                outBoard.close();
                boardOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                writeToErrorReport(boardname, e.getMessage());
                Application.logger.logLine(boardname+" ::: "+ e.getMessage());
                return;
            }
            return;
        }
        return;
    }

    public static void writeToErrorReport(String board, String error ){

        boolean lockAcquired = false;
        //Lock and Block + remove file from file list.
        try {
            if (Application.lockErrorRep.tryLock(10, TimeUnit.MILLISECONDS)) {

                Application.errorBoards.put(board, error);
                lockAcquired = true;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //release lock
            if (lockAcquired) {
                Application.lockErrorRep.unlock();
            }
        }
    }
}
