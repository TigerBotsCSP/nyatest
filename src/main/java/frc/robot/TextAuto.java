package frc.robot;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.wpi.first.wpilibj.Timer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;

public class TextAuto extends WebSocketServer {
    Timer m_balanceTimer = new Timer();
    boolean m_autoExecuting = false;
    ArrayList<WebSocket> m_sockets = new ArrayList<WebSocket>();

    // Gyro variables
    private final double Kp = 0.05;
    private final double Ki = 0.0;
    private final double Kd = 0.0;
    private double integral = 0.0;
    private double previousError = 0.0;
    private double setPoint = 0.0;

    public TextAuto() {
        super(new InetSocketAddress(4444));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        m_sockets.add(conn);
        System.out.println("[TextAuto] Client connected");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        m_sockets.remove(conn);
        System.out.println("[TextAuto] Client disconnected");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Parse actions
        JsonObject json = JsonParser.parseString(message).getAsJsonObject();
        String action = json.get("action").getAsString();
        String data = json.get("data").getAsString();

        if (action.equals("execute")) {
            log("Info", "Executing...");
            execute(data);
        } else if (action.equals("save")) {
            save(data);
        }
    }

    @Override
    public void onStart() {
        System.out.println("[TextAuto] Started server at default port 4444");
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
    };

    public void execute(String input) {
        m_autoExecuting = true;
        m_balanceTimer.start();

        String[] lines = input.toLowerCase().split("\n");

        for (String line : lines) {

            String[] params = line.split(" ");
            String type = params[0];
            float speed = 0;
            float time = 0;

            if (params.length >= 2)
                speed = Float.parseFloat(params[1]);
            if (params.length >= 3)
                time = Float.parseFloat(params[2]);

            if (type.equals("drive")) {
                RobotContainer.m_drive.straightDrive(speed);
                Timer.delay(time);
                RobotContainer.m_drive.straightDrive(0);
            } else if (type.equals("rotate")) {
                RobotContainer.m_arm.setVoltageOrientation(speed);
                Timer.delay(time);
                RobotContainer.m_arm.setVoltageOrientation(0);
            } else if (type.equals("length")) {
                RobotContainer.m_arm.setVoltageLength(-speed);
                Timer.delay(time);
                RobotContainer.m_arm.setVoltageLength(0);
            } else if (type.equals("delay")) {
                Timer.delay(speed);
            } else if (type.equals("balance")) {
                while (m_balanceTimer.get() < 14.9) {
                    // Uses roll for angle for Nyahiito
                    double angle = RobotContainer.m_gyro.getGyro().getRoll();

                    // Calculate the error and update the integral
                    double error = setPoint - angle;
                    integral += error * 0.02;
                    double derivative = (error - previousError) / 0.02;
                    previousError = error;

                    // Calculate the output using the PID formula
                    double output = Kp * error + Ki * integral + Kd * derivative;

                    output = RobotContainer.limit(output, 1);

                    RobotContainer.m_drive.straightDriveNormal(output);

                    Timer.delay(0.02);
                }

                m_balanceTimer.reset();
            }
        }

        m_autoExecuting = false;
    }

    public void save(String code) {
        // Playing will also officially save the file
        String id = String.format("%04d", new Random().nextInt(10000));

        try {
            Files.write(Paths.get("/home/lvuser/" + id + ".ta"), code.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        log("Success", "Saved as " + id + ".ta!");
    }

    public String load(String path) {
        try {
            String code = new String(Files.readAllBytes(Paths.get("/home/lvuser/" + path)));
            return code;
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public void log(String type, String message) {
        // Print for us
        System.out.println("[TextAuto::" + type + "] " + message);

        JsonObject log = new JsonObject();
        log.addProperty("type", type);
        log.addProperty("data", message);

        for (WebSocket conn : m_sockets) {
            conn.send(log.toString());
        }
    }
}