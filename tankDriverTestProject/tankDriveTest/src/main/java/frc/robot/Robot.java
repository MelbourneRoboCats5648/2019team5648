/*----------------------------------------------------------------------------*/
/* Copyright (c) 2017-2018 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.GenericHID.Hand;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.Spark;
import edu.wpi.first.wpilibj.Talon;
import edu.wpi.first.wpilibj.PWMTalonSRX;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.DoubleSolenoid;


/**
 * This is a demo program showing the use of the RobotDrive class, specifically
 * it contains the code necessary to operate a robot with tank drive.
 */
public class Robot extends TimedRobot {
  private DifferentialDrive m_myRobot;
  private DifferentialDrive m_myRobotTalon;
  private XboxController m_primaryController;
  private NetworkTableInstance ntInstance;
  private Compressor compressor;
  NetworkTable motorTable;
  DoubleSolenoid hatchSolenoid;
  boolean hatchOpen;
  private boolean intake = false;
  private boolean outtake = false;
  private Talon m_intakeMotor;
  DoubleSolenoid climbingfront;
  DoubleSolenoid climbingback;
  boolean climbingfrontopen;
  boolean climbingbackopen;
  private Talon m_winchmotor;   


  @Override
  public void robotInit() {
    // Setup XBox controller
    m_primaryController = new XboxController(0);

    // Setup drive motors
    m_myRobot = new DifferentialDrive(new Spark(0), new Spark(1));
    m_myRobotTalon = new DifferentialDrive(new PWMTalonSRX(8), new PWMTalonSRX(9));
    
    // Setup intake motors
    m_intakeMotor = new Talon(2);
    m_winchmotor = new Talon(4);

    // Create debug tables
    ntInstance = NetworkTableInstance.getDefault();
    NetworkTable debugTable = ntInstance.getTable("Debug");
    motorTable = debugTable.getSubTable("Motor Input");

    // Setup compressor
    try {
      compressor = new Compressor(0);
    }
    catch (Exception e)
    {
      System.err.println("Issue with compressor");
      throw e;
    }

    // Setup hatch solenoid
    hatchSolenoid = new DoubleSolenoid(0,1);
    hatchSolenoid.set(DoubleSolenoid.Value.kReverse);
    hatchOpen = false;

    // Setup climbing solenoids
    climbingfront = new DoubleSolenoid(2,3);
    climbingback = new DoubleSolenoid(4,5);   
    climbingfront.set(DoubleSolenoid.Value.kReverse);
    climbingfrontopen = false;
    climbingback.set(DoubleSolenoid.Value.kReverse); 
    climbingbackopen = false; 

    // Start compressor
    compressor.setClosedLoopControl(true);

    // Start camera server
    try{
      UsbCamera cameraFront = CameraServer.getInstance().startAutomaticCapture(0);
      UsbCamera cameraBack  = CameraServer.getInstance().startAutomaticCapture(1);
      // Set the camera video mode
      VideoMode[] modeFront = cameraFront.enumerateVideoModes();
      VideoMode[] modeBack = cameraFront.enumerateVideoModes();

      cameraFront.setVideoMode(modeFront[100]);
      cameraBack.setVideoMode(modeBack[100]);
    }
    catch (Exception e)
    {
      System.err.println("No USB Camera Found");
    }
    
  }

  @Override
  public void autonomousPeriodic () {
    arcadeDrive();
    pneumaticsControl();
    climbingControl();
    winchcontrol();
    motorIntake();
  }

  @Override
  public void teleopPeriodic() {
    arcadeDrive();
    pneumaticsControl();
    climbingControl();
    winchcontrol();
    motorIntake();
    }

  public void winchcontrol(){
    // This raises the winch for the motor intake 
    if(m_primaryController.getPOV() == 0){
      // D-Pad Up
      m_winchmotor.set(-1.0);
      }
    else if(m_primaryController.getPOV() == 180){
      m_winchmotor.set(1.0);
      // D-Pad Down
    }
    else{;
      m_winchmotor.set(0.0); 
    }
  }

  public void climbingControl() {
    // This statement controls the front climbing solenoid
    if(m_primaryController.getStartButtonPressed()){
        if (climbingfrontopen == true) {
          climbingfront.set(DoubleSolenoid.Value.kReverse);
          climbingfrontopen = false;
        }
        else {
          climbingfront.set(DoubleSolenoid.Value.kForward);
          climbingfrontopen = true;
        } 
    }

    // This statement controls the back climbing solenoid
    if(m_primaryController.getBackButtonPressed()){
      if(climbingbackopen == true){
        climbingback.set(DoubleSolenoid.Value.kReverse);
        climbingbackopen = false;
      }
      else {
        climbingback.set(DoubleSolenoid.Value.kForward);
        climbingbackopen = true;
      }
    }
  } 

  public void pneumaticsControl() {
    // Controlling the hatch mechanism
    if(m_primaryController.getBumperPressed(Hand.kRight)){
      if (hatchOpen == true) {
        hatchSolenoid.set(DoubleSolenoid.Value.kReverse);
        hatchOpen = false;
      }
      else
      {
        hatchSolenoid.set(DoubleSolenoid.Value.kForward);
        hatchOpen = true;
      }
    }
  }

  public void arcadeDrive() {
    // Arcade drive motor control
    var forward = m_primaryController.getY(Hand.kLeft)*0.8;
    var spin = m_primaryController.getX(Hand.kRight)*0.45;
    m_myRobot.arcadeDrive(forward, spin, false);
    m_myRobotTalon.arcadeDrive(forward, spin, false);
  }

  public void motorIntake(){
    //Ball intake motor control
    //A - Intake
    //Y - Outtake
    //B - Off
    if (m_primaryController.getAButtonPressed()) {
      intake = true;
      outtake = false;
    }

    if (m_primaryController.getYButtonPressed()) {
      outtake = true; 
      intake = false;
    }

    if (m_primaryController.getBButtonPressed()) {
      intake = false;
      outtake = false;
    }

    if (intake == true) {m_intakeMotor.set(-0.3);}
    if (outtake == true) {m_intakeMotor.set(1.0);}
    if (intake == false && outtake == false) {m_intakeMotor.set(0);}
  }
}