package frc.robot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;

import com.google.gson.*;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

public class Ncp {
    // * Variables
    String ncpServerURL = "ws://10.90.72.221:9072/v1/client";
    WebSocketFactory ncpFactory;
    WebSocket ncpWebSocket;
    ArrayList<String> ncpLogs = new ArrayList<>();

    // * APS Stuff
    ArrayList<ArrayList<Double>> apsActions = new ArrayList<>(3);
    int index = 0;

    Ncp() {
        // ? NCP uses websockets for fast communication between the client, the server,
        // and the robot.
        // ? The client and server should be connected to the radio network for
        // successful routine.
        try {
            ncpFactory = new WebSocketFactory();
            ncpWebSocket = ncpFactory.createSocket(ncpServerURL, 1000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ! Nyahiito Control Panel: Core function, handles initial connection and
    // messages
    public void core() {
        try {
            ncpWebSocket.addListener(new WebSocketAdapter() {
                @Override
                public void onTextMessage(WebSocket websocket, String message) throws Exception {
                    JsonObject rootObject = JsonParser.parseString(message).getAsJsonObject();
                    if (rootObject.has("Action")) {
                        // ? Lite
                        lite(rootObject);
                    }
                }
            });

            ncpWebSocket.connect();

            log("Hi!", true);
        } catch (WebSocketException e) {
            e.printStackTrace();
        }
    }

    public void log(String message, boolean isABigDeal) {
        JsonObject log = new JsonObject();
        log.addProperty("Log", message);
        if (isABigDeal) {
            log.addProperty("Action", "BigLog");
        } else {
            log.addProperty("Action", "Log");
        }
        // ? Log Publish: Send a message to display in the client's terminal
        ncpWebSocket.sendText(log.toString());
    }

    // ! Don't pull a sudo rm -rf /
    public void exec(String cmd) {
        try {
            // * For the actual robot, it's { "/bin/sh", "-c", cmd }
            Process proc = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", "cd /home/lvuser && " + cmd });
            proc.waitFor();
        } catch (IOException | InterruptedException e) {
            // Alert that the command failed
            log("Invalid.", true);

            e.printStackTrace();
        }
    }

    // Loads a pathway array.
    public void apl(String path) {
        apsActions.clear();

        try {
            // Load file into a String
            String pathData = new String(Files.readAllBytes(Paths.get(path)));
            Gson gson = new Gson();

            // Put the actions into the Array
            apsActions = gson.fromJson(pathData, ArrayList.class);

            log("Path loaded! Click play to play.", true);

        } catch (IOException e) {
            log("Path doesn't exist!", true);
            e.printStackTrace();
        }
    }

    String liteMode = "Stop";
    boolean liteDoAuto = false;

    // ? NCP Lite
    public void lite(JsonObject root) {
        // Check action and set it
        String action = root.get("Action").getAsString();
        liteMode = action;

        String data = root.get("Data").getAsString();

        if (action.equals("Save")) {
              // Playing will also officially save the file
              String id = String.format("%04d", new Random().nextInt(10000));

              // Convert to JSON string then save
              Gson gson = new GsonBuilder().create();
              String json = gson.toJson(apsActions, ArrayList.class);
                    
              try {
                Files.write(Paths.get("/home/lvuser/" + id + ".json"), json.getBytes());
              } catch (IOException e) {
                e.printStackTrace();
              }
                    
              log("Saved as " + id + ".json", true);
        } else if (action.equals("Reset")) {
            liteDoAuto = false;
            apsActions.clear();
            log("Cleared.", true);
        } else if (action.equals("Play")) {
            apl("/home/lvuser/" + data + ".json");
            liteDoAuto = true;
            log("Playing...", true);
        } 
    }
}