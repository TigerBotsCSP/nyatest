package frc.robot;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.PneumaticsModuleType;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.DoubleSolenoid.Value;

public class Intaker {
    public boolean m_in; // For motor
    public boolean m_down; // For position
    public boolean m_allowLock = false; // Allow motor lock 

    private CANSparkMax m_intakerMotor;
    private DoubleSolenoid m_intakerPusher;

    Intaker() {
        m_intakerMotor = new CANSparkMax(12, MotorType.kBrushless);
        m_intakerPusher = new DoubleSolenoid(PneumaticsModuleType.REVPH, 6, 7);
        m_in = true;
        m_down = false;
    }

    public void toggleMotor() {
        if (m_in) {
            m_intakerMotor.set(Constants.intakerSpeed);
            m_in = false;
        } else {
            m_intakerMotor.set(-Constants.intakerSpeed);
            m_in = true;
        }

        // Current spikes on direction change.
        // Add cooldown in a thread for current locking when direction changes
        ExecutorService executor = Executors.newFixedThreadPool(1); // Just 1?
        executor.submit(() -> {
            m_allowLock = false;
            Timer.delay(.75);
            m_allowLock = true;
        });
    }

    public void togglePusher() {
        if (m_down) {
            m_intakerPusher.set(Value.kReverse);
            m_down = false;
        } else {
            m_intakerPusher.set(Value.kForward);
            m_down = true;
        }
    }

    public double getCurrent() {
        return m_intakerMotor.getOutputCurrent();
    }

    public void setSpeed(double speed) {
        m_intakerMotor.set(speed);
    }

    public void tryLock() {
        if (m_allowLock) {
            setSpeed(0);
            System.out.println("! did lock");
        }
    }
}
