package app.peermap;


import android.location.LocationManager;
import android.os.Bundle;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;
import app.peermap.R;

public class MainActivity extends Activity {
	
	private LocationManager gpsLocationManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		final ToggleButton recordBtn = (ToggleButton) findViewById(R.id.recordBtn);
		
		if(isLocationPollServiceRunning()){
			recordBtn.setChecked(true);
		}
		
		this.gpsLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		
		recordBtn.setOnClickListener( new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent locationPollIntent = new Intent(getApplicationContext(), LocationPollService.class);
				// start/stop the recording
				if(recordBtn.isChecked()){
					
					// Check if GPS is enabled
					boolean gpsEnabled = gpsLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
					
					// If not, prompt user to enable GPS
					if (!gpsEnabled) {
						// If GPS is not enabled
						// set record button back to off
						recordBtn.setChecked(false);
						
						Toast toast = Toast.makeText(getApplicationContext(), "GPS not available", Toast.LENGTH_SHORT);
						toast.setGravity(Gravity.CENTER, 0, 0);
						toast.show();

						// Create alert dialog here
						enableLocationSettings();
					}
					else{
						// start recording
						System.out.println("Start Recording");
						startService(locationPollIntent);
					}
				}
				else{
					System.out.println("Stop Recording");
					stopService(locationPollIntent);
				}
				
			}
		});
		
		Button choosePathBtn = (Button) findViewById(R.id.choosePathBtn_Home);
		choosePathBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent pathIntent = new Intent(getApplicationContext(), PathActivity.class);
				startActivity(pathIntent);
				
			}
		});
		
		Button saveFile = (Button) findViewById(R.id.saveBtn);
		saveFile.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//Intent pathIntent = new Intent(getApplicationContext(), PathActivity.class);
				//startActivity(pathIntent);
				Intent toCloud=new Intent(MainActivity.this,DropboxActivity.class);
    			startService(toCloud);
				
			}
		});
				
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void startClicked(View view) {
        startService(new Intent("LocationPollService"));
    }

    public void stopClicked(View view) {
        stopService(new Intent("LocationPollService"));
    }
    
    private void enableLocationSettings() {
		Intent settingsIntent = new Intent(
				android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(settingsIntent);
	}
    
    private boolean isLocationPollServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LocationPollService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}
