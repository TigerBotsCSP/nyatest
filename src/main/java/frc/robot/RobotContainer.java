package frc.robot;

import java.io.File;
import java.util.ArrayList;

import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class RobotContainer {
    // Core
    public static Drive m_drive = new Drive();
    public static Arm m_arm = new Arm();
    public static Gyro m_gyro = new Gyro();
    public static Intaker m_intaker = new Intaker();
    public static NyaDS m_nyads = new NyaDS();
    public static Ncp m_ncp = new Ncp();
    public static TextAuto m_auto = new TextAuto();

    // Controllers
    public static XboxController m_driveController = new XboxController(0);
    public static XboxController m_armController = new XboxController(1);

    // Constants
    public static double driveSpeed = .7;
    public static double armSpeed = .6;
    public static int armLengthOutPOV = 0;
    public static int armLengthInPOV = 180;
    public static double joystickDriftSafety = 0.3;
    public static double pathwayLoadingDelay = 0.25;

    // Functions
    public static double limit(double value, double limit) {
        return Math.max(Math.min(value, limit), -limit);
    }

    public static void updateData() {
        // ? Try to keep repeating start so server is ensured to be connected
        // ! Bad code here, but just for testing
        try {
            m_auto.start();
        } catch(Exception e) {

        }

        // Set Shuffleboard data
        Constants.Auto.autoPath = SmartDashboard.getString("Auto Path/active", "");
        SmartDashboard.putNumber("Gyro Pitch", m_gyro.getGyro().getPitch());
        SmartDashboard.putNumber("Gyro Roll", m_gyro.getGyro().getRoll());
        SmartDashboard.putNumber("Gyro Yaw", m_gyro.getGyro().getYaw());
        SmartDashboard.putNumber("Gyro Angle", m_gyro.getGyro().getAngle());
        SmartDashboard.putBoolean("Drive Speed", (Constants.driveSpeed == .7 ? false : true)); // Green if turbo
        SmartDashboard.putBoolean("Arm Speed", (Constants.armSpeed == .6 ? false : true)); // Green if turbo
        SmartDashboard.putBoolean("Intaker In", m_intaker.m_in);
        SmartDashboard.putBoolean("Brake Mode", m_drive.m_brakeMode);
        SmartDashboard.putNumber("Intake Current", m_intaker.getCurrent());
    }

    public static void init() {
        try {
            // Autonomous chooser
            ArrayList<String> fileNames = new ArrayList<>();

            File directory = new File("/home/lvuser/");
            // New TextAuto files
            File[] files = directory.listFiles((dir, name) ->  name.endsWith(".ta"));

            for (File file : files) {
                fileNames.add(file.getName());
            }

            SendableChooser<String> chooser = new SendableChooser<>();
            for (String fileName : fileNames) {
                chooser.addOption(fileName, fileName);
            }
            chooser.setDefaultOption(Constants.Auto.autoPath, Constants.Auto.autoPath);

            SmartDashboard.putData("Auto Path", chooser);
            
            m_auto.start();
        } catch (Exception e) {
            // No files :(
        }
    }
}
