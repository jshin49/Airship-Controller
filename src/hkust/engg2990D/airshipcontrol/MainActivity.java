package hkust.engg2990D.airshipcontrol;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener, OnCheckedChangeListener,
					OnItemSelectedListener, SeekBar.OnSeekBarChangeListener {
	
	// This is the textview where we display all the status messages
	TextView mtxtStatus;
	
	// This is the reference to the Stop Button on the UI. If you include other similar image
	// buttons in your UI, you can use the same method as used for stop button. Just choose the
	// appropriate image, get a reference to the button in the onCreate() method. Then declare
	// the appropriate onClick() listener code in the onClick() method.
	ImageButton mbtnStop;
	
	// This is the Bluetooth On/Off Switch
	Switch mswConn;
	
	// These are two other switches that control Pin 7 and Pin 8. They could be used
	// to turn the electromagnet on and off, depending on which pin you attach the electromagnet.
	// Use the appropriate switch.
	Switch mswSwitch1;
	Switch mswSwitch2;
	
	// The spinner is used to select a specific motor and set its speed.
	Spinner mspMotor;
	ArrayAdapter<String> motorsAdapter;
	
	// This value is initialized to the currently selected motor. This variable
	// takes on values from 0 .. 5 corresponding to Motor 1 .. Motor 6. Hence if
	// you need to get the motor number, add 1 to this variable.
	int currentMotorIndex;
	
	SeekBar mseekBarMotorSpeed;
	
	// The maximum and minimum speeds of the motors. This is used to set the range of
	// the seek bar (from -MAX_DUTY_CYCLE to +MAX_DUTY_CYCLE). The whole range of the
	// speed is 2*MAX_DUTY_CYCLE. The seekbar position value ranges from 0 .. maximum value.
	// The maximum value of the seekbar is set to 2*MAX_DUTY_CYCLE. Thus the centre point will
	// be at MAX_DUTY_CYCLE corresponding to a motor speed of 0.
	final int MIN_DUTY_CYCLE = 0;
	final int MAX_DUTY_CYCLE = 255;
	
	// This array keeps track of the seekbar position for each motor
	// set this value in the onProgressChanged() method of the seekbar
	// DO seekBarPos[currentMotorIndex] = progress;
	int [] seekBarPos = {MAX_DUTY_CYCLE, MAX_DUTY_CYCLE, MAX_DUTY_CYCLE, MAX_DUTY_CYCLE, MAX_DUTY_CYCLE, MAX_DUTY_CYCLE};
	
	// This array keeps track of the motor speeds for each of the six motors.
	// set this value in the onProgressChanged() method of the seekbar
	// DO motorSpeed[currentMotorIndex] = (progress - MAX_DUTY_CYCLE);
	int [] motorSpeed = {MIN_DUTY_CYCLE, MIN_DUTY_CYCLE, MIN_DUTY_CYCLE, MIN_DUTY_CYCLE, MIN_DUTY_CYCLE, MIN_DUTY_CYCLE};
	
	TextView mspeedMin, mspeedMax, mspeedMid;
	
	/*
	 * Controller:
	 * 
	 *    The controller object gives us the right Application Programmer's Interface (API) to
	 *    manage the connection to the Arduino board through Bluetooth to issue commands to the
	 *    motors. This class exposes the following methods which you can call to control the motors
	 *    attached to the Arduino board
	 *    
	 *    Connect(): Turn on the Bluetooth Connection and establish connection to a device
	 *    
	 *    Disconnect(): Turn off the Bluetooth Connection
	 *    
	 *    IsConnected(): Is the Bluetooth connection on (returns true) or off (returns false)
	 *    
	 *    AutoStartBT(): Automatically enable the Bluetooth adapter so that we can establish connection. 
	 *         This is done at the start.
	 *    
	 *    AllStop(): Stop all the motors and set their speeds to zero
	 *    
	 *    MotorStart(int motorindex, int velocity): set the speed of the motor "motorindex" to "velocity"
	 *         In this case "motorindex" goes from 0 .. 5 corresponding to Motor 1 .. Motor 6
	 *         velocity ranges from -MAX_DUTY_CYCLE .. MAX_DUTY_CYCLE
	 *    
	 *    SwitchChange(int index, int state): Turn on/off the selected switch "index" to the "state"
	 *         Here "index" is either controller.SWITCH1 or controller.SWITCH2
	 *         "state" is either controller.ON or controller.OFF
	 *    
	 *    
	 */
	Controller controller;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// This sets the User Interface to the layout designed in activity_main.xml file
		setContentView(R.layout.activity_main);
		
		// create the controller object
		controller = new Controller(this);
		
		// check if bluetooth is available and turn it on automatically
		if(!controller.AutoStartBT()){			
			return;
		}
		
		// get a reference to the status textview
		mtxtStatus = (TextView) findViewById(R.id.txtStatus);
		
		// get a reference to the stop button and set it to be disabled
		// set the onclick listener for the button.
		// You can use the same procedure to include other image buttons on the UI
		mbtnStop = (ImageButton) findViewById(R.id.btnStop);
		mbtnStop.setOnClickListener(this);
		mbtnStop.setEnabled(false);
		
		// get a reference to the switches and st them to be disabled. set their on checked change listeners
		mswConn = (Switch) findViewById(R.id.swConn);
		mswConn.setOnCheckedChangeListener(this);
		mswSwitch1 = (Switch) findViewById(R.id.swSwitch1);
		mswSwitch1.setEnabled(false);
		mswSwitch2 = (Switch) findViewById(R.id.swSwitch2);
		mswSwitch2.setEnabled(false);
		mswSwitch1.setOnCheckedChangeListener(this);
		mswSwitch2.setOnCheckedChangeListener(this);
		
		//get a reference to the spinner
		mspMotor = (Spinner) findViewById(R.id.spMotor);
		motorsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
		motorsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		for (int i=1; i <= 6; i++) {
			motorsAdapter.add("Motor "+ i);
		}
		mspMotor.setAdapter(motorsAdapter);
		mspMotor.setEnabled(false);
		mspMotor.setOnItemSelectedListener(this);
		
		// Recall that the motor speed ranges from -MAX_DUTY_CYCLE to MAX_DUTY_CYCLE.
		// so the whole range is 2*MAX_DUTY_CYCLE. The seek bar position is always positive
		// Thus the seek bar position goes from 0 .. 2*MAX_DUTY_CYCLE
		mseekBarMotorSpeed = (SeekBar) findViewById(R.id.seekBarMotorSpeed);
		mseekBarMotorSpeed.setEnabled(false);
		mseekBarMotorSpeed.setMax(2*MAX_DUTY_CYCLE);
		mseekBarMotorSpeed.setProgress(MAX_DUTY_CYCLE);
		mseekBarMotorSpeed.setOnSeekBarChangeListener(this);
		
		// These three textviews are used to show the scale indicator below the seekbar
		mspeedMin = (TextView) findViewById(R.id.speedMin);
		mspeedMid = (TextView) findViewById(R.id.speedMid);
		mspeedMax = (TextView) findViewById(R.id.speedMax);
		
		Integer i = MAX_DUTY_CYCLE;
		mspeedMin.setText("-" + i.toString());
		mspeedMid.setText("0");
		mspeedMax.setText(i.toString());		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
		switch (v.getId()) {
		
		// if you add more image buttons to the UI, include similar case statements
		// for those buttons here and include the code before the break statement.
		// Don't forget to include the break statement at the end of each section
		// of the code. This is a major source of bugs in the code
		case R.id.btnStop:
				mtxtStatus.setText("Stop All Motors");
				
				// This statement is used to stop all the motors and set their speeds to 0
				controller.AllStop();
			break;
		
		default:
			break;
		}
		
	}

	@Override
	public void onCheckedChanged(CompoundButton btn, boolean set) {

		if (btn == mswConn && set) {
			// If the bluetooth button is turned on, then execute the code below
			
			// create asyncTask for the user interaction
			// DO NOT disturb this code.
			class EnableUI extends AsyncTask<Void, Void, Void>{

				protected void onPreExecute(){
					
					// show a paired devices list and wait for the user to select a device 
					// by pressing a selection for connection
					controller.Connect();	
				}
				
				protected void onPostExecute(Void result){
					
					// turn on UI after connected
					mbtnStop.setEnabled(true);
					mspMotor.setEnabled(true);
					mseekBarMotorSpeed.setEnabled(true);
					mswSwitch1.setEnabled(true);
					mswSwitch2.setEnabled(true);
				}
				@Override
				protected Void doInBackground(Void... params) {
					
					// check if it is connected
					while(!controller.IsConnected());
					return null;
				}				
			}
			
			EnableUI enableUI = new  EnableUI();
			enableUI.execute();
			mtxtStatus.setText("Bluetooth Turned On!");
		}
		else if (btn == mswConn && !set) {
			// If the bluetooth button is turned off, then execute the code below
			
			// create asyncTask for the user interaction
			// DO NOT disturb this code.
			class DisableUI extends AsyncTask<Void, Void, Void>{


				protected void onPreExecute(){
					
					// disconnecting
					controller.Disconnect();	
				}
				
				protected void onPostExecute(Void result){
					
					// turn off UI after disconnected
					mbtnStop.setEnabled(false);
					mspMotor.setEnabled(false);
					mseekBarMotorSpeed.setEnabled(false);
					mswSwitch1.setEnabled(false);
					mswSwitch2.setEnabled(false);
				}
				@Override
				protected Void doInBackground(Void... params) {

					// check if it is disconnected
					while(controller.IsConnected());
					return null;
				}				
			}
			
			DisableUI disableUI = new  DisableUI();
			disableUI.execute();
			mtxtStatus.setText("Bluetooth Turned Off!");
		}
		else if (btn == mswSwitch1 && set) {
			// if the user switches on Switch1
			// set pin 7 to high
			controller.SwitchChange(controller.SWITCH1, controller.ON);
			mtxtStatus.setText("Switch 1 Turned On!");
		}
		else if (btn == mswSwitch1 && !set) {
			// if the user switches off Switch1
			// set pin 7 to low
			controller.SwitchChange(controller.SWITCH1, controller.OFF);		
			mtxtStatus.setText("Switch 1 Turned Off!");
		}
		else if (btn == mswSwitch2 && set) {
			// if the user switches on Switch2
			// set pin 8 to high
			controller.SwitchChange(controller.SWITCH2, controller.ON);
			mtxtStatus.setText("Switch 2 Turned On!");
		}
		else if (btn == mswSwitch2 && !set) {
			// if the user switches off Switch2
			// set pin 8 to low
			controller.SwitchChange(controller.SWITCH2, controller.OFF);
			mtxtStatus.setText("Switch 2 Turned Off!");
		
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int position,
			long arg3) {
		
		// position indicated which position of the spinner. By default
		// position starts at 0. Remember, Motor 1 is in position 0 on
		// the spinner. So to get the motor number I have to add 1 to
		// position
		
		currentMotorIndex = position;
		
		mtxtStatus.setText("Motor "+(currentMotorIndex+1) + " selected");
		
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// TODO Auto-generated method stub
		
		mtxtStatus.setText("Motor "+(currentMotorIndex+1) + "'s Speed set to "+ (progress - MAX_DUTY_CYCLE));
		seekBarPos[currentMotorIndex] = progress;
		motorSpeed[currentMotorIndex] = (progress - MAX_DUTY_CYCLE);
		
		// start the motor selected by the spinner at the speed specified by the seekbar
		// Note that we identify Motor 1 as 0, Motor 2 as 1 and so on.
		controller.MotorStart(currentMotorIndex, progress - MAX_DUTY_CYCLE);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

}
