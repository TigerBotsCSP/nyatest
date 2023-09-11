// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import java.util.ArrayList;

import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Robot extends TimedRobot {
  public void ga() {
    RobotContainer.m_auto.start();
  }

  @Override
  public void robotInit() {
    RobotContainer.m_gyro.init();

    RobotContainer.init();
  }

  @Override
  public void robotPeriodic() {
    RobotContainer.updateData();

    // Reset Gyro
    if (RobotContainer.m_driveController.getBackButtonPressed()) {
      RobotContainer.m_gyro.reset();
    }
  }

  @Override
  public void teleopInit() {
    RobotContainer.m_intaker.setSpeed(-Constants.intakerSpeed);
  }

  @Override
  public void teleopPeriodic() {
    if (RobotContainer.m_nyads.m_executing ||
        RobotContainer.m_auto.m_autoExecuting)
      return;

    // Drive Speed Toggle
    if (RobotContainer.m_driveController.getLeftBumperPressed()) {
      if (Constants.driveSpeed == .6) {
        Constants.driveSpeed = .8;
      } else {
        Constants.driveSpeed = .6;
      }
    }

    // Arm Speed Toggle
    if (RobotContainer.m_armController.getYButtonPressed()) {
      if (Constants.armSpeed == .6) {
        Constants.armSpeed = .8;
      } else {
        Constants.armSpeed = .6;
      }
    }
    
    // Drive Mode Toggle
    if (RobotContainer.m_driveController.getXButtonPressed()) {
      RobotContainer.m_drive.toggleMode();
    }

    // Arm Intaker
    if (RobotContainer.m_armController.getAButton()) {
      RobotContainer.m_arm.close();
    } else if (RobotContainer.m_armController.getBButton()) {
      RobotContainer.m_arm.open();
    }

    // Main Intaker
    if (RobotContainer.m_driveController.getBButtonPressed()) {
      RobotContainer.m_intaker.togglePusher();
    }

    if (RobotContainer.m_intaker.getCurrent() > Constants.currentLimit && RobotContainer.m_intaker.m_in) {
      RobotContainer.m_intaker.setSpeed(-0.0);
    } else if (RobotContainer.m_driveController.getYButtonPressed()) {
      RobotContainer.m_intaker.toggleMotor();
    }

    // Drive functionality
    if (RobotContainer.m_driveController.getRightBumper()) {
      RobotContainer.m_drive.straightDrive(0);
    } else if (RobotContainer.m_ncp.liteMode.equals("Record")) {
      // ? Only straight drive when recording
      RobotContainer.m_drive
          .straightDrive(RobotContainer.m_driveController.getLeftY() * RobotController.getBatteryVoltage());
    } else {
      if (Constants.useBothJoysticks) {
        double speedX = RobotContainer.limit(RobotContainer.m_driveController.getRightX(), Constants.driveSpeed);
        double speedY = RobotContainer.limit(-RobotContainer.m_driveController.getLeftY(), Constants.driveSpeed);
        RobotContainer.m_drive.rotateDrive(speedX, speedY);
      } else {
        double speedX = RobotContainer.limit(-RobotContainer.m_driveController.getLeftX(), Constants.driveSpeed);
        double speedY = RobotContainer.limit(-RobotContainer.m_driveController.getLeftY(), Constants.driveSpeed);
        RobotContainer.m_drive.rotateDrive(speedX, speedY);
      }
    }

    // * Arm rotation with curving
    double armSpeed = RobotContainer.limit(-RobotContainer.m_armController.getLeftY(), Constants.armSpeed);
    RobotContainer.m_arm.setOrientation(-armSpeed);

    // * Arm length with curving
    double armLength = RobotContainer.limit(-RobotContainer.m_armController.getRightY(), Constants.armSpeed);
    RobotContainer.m_arm.setLength(armLength);

    SmartDashboard.putString("stopped", "no");
    // Stop intaker
    if (RobotContainer.m_intaker.getCurrent() > 15) {
      RobotContainer.m_intaker.tryLock();
    }

    // ? NCP Lite hook - NCP Lite hook - NCP Lite hook - NCP Lite hook
    if (RobotContainer.m_ncp.liteMode.equals("Record")) {
      ArrayList<Double> data = new ArrayList<Double>();

      // TODO: Check if just using 12V is fine. Multiplying by the current voltage
      // should be fine
      data.add(RobotContainer.m_driveController.getLeftY() * RobotController.getBatteryVoltage()); // Robot forward
      data.add(RobotContainer.m_driveController.getLeftX()); // Robot rotation
      data.add(RobotContainer.m_armController.getLeftY() * RobotController.getBatteryVoltage()); // Arm rotation
      data.add(RobotContainer.m_armController.getRightY() * RobotController.getBatteryVoltage()); // Arm length
      data.add(RobotContainer.m_armController.getBButtonPressed() ? 1.0
          : (RobotContainer.m_armController.getAButtonPressed() ? 2.0 : 0)); // Intaker
      data.add((Double) RobotController.getBatteryVoltage()); // Voltage

      // Push the data
      RobotContainer.m_ncp.apsActions.add(data);
    } else if (RobotContainer.m_ncp.liteMode.equals("Play") && RobotContainer.m_ncp.liteDoAuto) {
      try {
        // ? Play
        double vc = RobotController.getBatteryVoltage(); // Current/new/this voltage
        int i = RobotContainer.m_ncp.index; // Index of current loop
        var a = RobotContainer.m_ncp.apsActions; // ArrayList<ArrayList<Double>>
        var arr = a.get(i); // Current data array
        double vo = arr.get(5); // Original/recorded voltage

        // Arm Intaker
        if (arr.get(4) == 2.0) {
          RobotContainer.m_arm.close();
        } else if (arr.get(4) == 1.0) {
          RobotContainer.m_arm.open();
        }

        // Drive functionality with curving
        RobotContainer.m_drive.straightDrive(arr.get(0) * (vo / vc));

        // Arm rotation (with curving)
        RobotContainer.m_arm.setOrientation(-arr.get(2) * (vo / vc));

        // Arm length...yes, with curving
        RobotContainer.m_arm.setLength(arr.get(3) * (vo / vc));
      } catch (Exception e) {
        RobotContainer.m_ncp.liteDoAuto = false;
        RobotContainer.m_ncp.index = 0;
        RobotContainer.m_ncp.log("All done.", true);
      }
    }
  }

  @Override
  public void autonomousInit() {
    RobotContainer.m_auto.execute(RobotContainer.m_auto.load(Constants.Auto.autoPath));
  }

  @Override
  public void autonomousPeriodic() {
  }
}