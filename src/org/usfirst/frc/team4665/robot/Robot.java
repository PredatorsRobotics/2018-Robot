/*----------------------------------------------------------------------------*/
/* Copyright (c) 2017-2018 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package org.usfirst.frc.team4665.robot;

import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.PWMTalonSRX;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.Spark;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.Victor;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the IterativeRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the manifest file in the resource
 * directory.
 */
public class Robot extends IterativeRobot {
	private PWMTalonSRX driveMotors[] = { new PWMTalonSRX(5), new PWMTalonSRX(6), new PWMTalonSRX(7), new PWMTalonSRX(8) };
	private RobotDrive m_robotDrive = new RobotDrive(driveMotors[0], driveMotors[2], driveMotors[1], driveMotors[3]);
	private Spark armExtender = new Spark(2);
	private Spark armDart = new Spark(3);
	private Joystick r_stick = new Joystick(0);
	private Joystick l_stick = new Joystick(1);
	private Timer m_timer = new Timer();
	private DigitalInput limitSwitches[] = { new DigitalInput(0), new DigitalInput(1), new DigitalInput(2),
			new DigitalInput(3) };
	private boolean isHoldingBox = false;
	private Spark gripperMotors[] = { new Spark(0), new Spark(1) };
	private boolean isLeftSideOurs;
	private SendableChooser autoChooser;
	private String autoSide;

	/**
	 * This function is run when the robot is first started up and should be used
	 * for any initialization code.
	 */
	@Override
	public void robotInit() {
		// All of our drive motors are inverted
		for (PWMTalonSRX driveMotor : driveMotors) {
			driveMotor.setInverted(true);
		}
		autoChooser = new SendableChooser();
		autoChooser.addDefault("Advance", "Default");
		autoChooser.addObject("Left", "Left");
		autoChooser.addObject("Right", "Right");
		SmartDashboard.putData("Auto Chooser", autoChooser);

	}

	private Object Default() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * This function is run once each time the robot enters autonomous mode.
	 */
	@Override
	public void autonomousInit() {
		m_timer.reset();
		m_timer.start();
		String gameData;
		isLeftSideOurs = DriverStation.getInstance().getGameSpecificMessage().charAt(1) == 'L';
		autoSide = (String) autoChooser.getSelected();
	}

	/**
	 * This function is called periodically during autonomous.
	 */
	@Override
	public void autonomousPeriodic() {

		if (autoSide.equals("Left") && isLeftSideOurs)
			autoSameSide("Left");
		else if (autoSide.equals("Right") && !isLeftSideOurs)
			autoSameSide("Right");
		else if (autoSide.equals("Left") && !isLeftSideOurs)
			autoOppositeSide("Left");
		else if (autoSide.equals("Right") && isLeftSideOurs)
			autoOppositeSide("Right");
		else
			autoDefault();
	}

	public void autoSameSide(String side) {
		Integer rotationCoefficient = side.equals("Left") ? 1 : -1;
			
		// Left
		if (m_timer.get() < 0.5) {
			m_robotDrive.arcadeDrive(0, (isLeftSideOurs ? -0.5 : 0.5));
		} else if (m_timer.get() < 3.0) {
			// TODO: figure out why we have to go negative here
			m_robotDrive.arcadeDrive(-0.5, 0.0); // drive forwards half speed
		} else {
			m_robotDrive.stopMotor(); // stop robot
		}
	}
	
	public function autoOppositeSide(String side) {
		Integer rotationCoefficient = side.equals("Left") ? 1 : -1;
		// TODO
	}
	
	public function autoDefault() {
		// Default (Advance)
		if (m_timer.get() < 6) {
			m_robotDrive.arcadeDrive(-.5,0);
		} else {
			m_robotDrive.stopMotor(); // stop robot
		}
	}

	/**
	 * This function is called once each time the robot enters teleoperated mode.
	 */
	@Override
	public void teleopInit() {
	}

	/**
	 * This function is called periodically during teleoperated mode.
	 */
	@Override
	public void teleopPeriodic() {
		m_robotDrive.arcadeDrive(r_stick.getY(), r_stick.getX());

		// Extend/retract arm:
		if (r_stick.getRawButton(2) && !limitSwitches[1].get()) { // Button Pressed and switch not pressed
			armExtender.set(-1); // Retract arm
		} else if (r_stick.getRawButton(3) && limitSwitches[3].get() && !limitSwitches[2].get() && // the arm is all the
																									// way up
				!limitSwitches[2].get()) { // the arm isn't fully extended
			armExtender.set(1); // Extend arm
		} else {
			armExtender.set(0); // Disable arm
		}

		// Raise and lower arm:
		if (r_stick.getRawButton(6) && !limitSwitches[3].get()) { // Button Pressed and switch not pressed
			armDart.set(.85); // Raise arm
		} else if (r_stick.getRawButton(7) && limitSwitches[1].get()) {
			armDart.set(-.75); // Lower arm
		} else {
			armDart.set(0); // Disable arm
		}

		// Deal with gripper
		if (r_stick.getRawButton(11)) {
			isHoldingBox = true;
		} else if (r_stick.getTrigger()) {
			isHoldingBox = false;
			setGrip(1);
		} else if (l_stick.getTrigger()) {
			isHoldingBox = false;
			setGrip(0.3);
		} else if (isHoldingBox && !limitSwitches[0].get()) { // if the box isn't fully sucked in
			setGrip(-0.65);
		} else {
			setGrip(0);
		}
	}

	public void setGrip(double value) {
		if (r_stick.getRawButton(4))
			gripperMotors[0].set(-value/2); // invert
		else
			gripperMotors[0].set(value);
		if (r_stick.getRawButton(5))
			gripperMotors[1].set(value/2);
		else
			gripperMotors[1].set(-value);
	}

	/**
	 * This function is called periodically during test mode.
	 */
	@Override
	public void testPeriodic() {
	}
}
