package app.peermap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVReader;

public class PathActivity extends Activity {

	private LocationManager mLocationManager;
	private SensorManager mSensorManager;
	private Sensor accelerometer;
	private Sensor magnetometer;
	private CSVReader reader;
	private List<Location> routePoints;
	private Location currentLocation;
	private Location closestPoint;
	private float minDistance;
	private String directionToStr;
	// record the compass picture angle turned
	private float currentDegree = 0f;

	private static final int THREE_SECONDS = 3000;
	private static final int TWO_METERS = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_path);

		routePoints = new ArrayList<Location>();

		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		// mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
		// THREE_SECONDS, TEN_METERS, mLocationListener);

		closestPoint = null;
		minDistance = 0;
		directionToStr = "";

		// Create route array from csv file
		// Read file
		try {
			// static file location for my testing File
			File csvFile = new File("/storage/emulated/0/PeerMap/Sample",
					"path" + ".csv");
			reader = new CSVReader(new FileReader(csvFile), ';');
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		Log.e("LOG", "file read"); // Parse csv and store into array list
		String[] nextLine;
		try {
			while ((nextLine = reader.readNext()) != null) { // nextLine[] is an
																// array of
																// values from
																// the line
																// String[]
				String[] parts = nextLine[0].split(";");

				Log.i("Latitude", parts[0]);
				Log.i("Longitude", parts[1]);

				Location l = new Location(LocationManager.GPS_PROVIDER);
				l.setLatitude(Double.parseDouble(parts[0]));
				l.setLongitude(Double.parseDouble(parts[1]));
				routePoints.add(l);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (Location loc : routePoints) {
			System.out.println("Lat: " + loc.getLatitude() + ", Long: "
					+ loc.getLongitude());
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		System.out.println("Resuming!!!");
		// for the system's orientation sensor registered listeners
		mSensorManager.registerListener(mSensorListener, accelerometer,
				SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(mSensorListener, magnetometer,
				SensorManager.SENSOR_DELAY_UI);

		startUpdates();

		currentLocation = mLocationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);

	}

	public void getClosetPoint() {
		closestPoint = routePoints.get(0);

		minDistance = currentLocation.distanceTo(closestPoint);

		if (currentLocation != null) {
			// find the closet point in meters to travel to in order to get back
			// on course
			for (Location routeLocation : routePoints) {
				float distance = currentLocation.distanceTo(routeLocation);
				if (distance < minDistance) {
					closestPoint = routeLocation;
					minDistance = distance;
				}
			}
			// show distance in view
			TextView distanceTo = (TextView) findViewById(R.id.distanceToTxtField);
			distanceTo.setText(Float.toString(minDistance));
		}
		else{
			// say we can't start tracking until we get an initial location or closest point
		}
	}

	public void getTravelDirection() {
		// gets current direction

		float bearingToClosestPoint = currentLocation.bearingTo(closestPoint);
		bearingToClosestPoint = (bearingToClosestPoint+360)%360;
		System.out.println(bearingToClosestPoint);
		
		directionToStr = degreeToDirection(bearingToClosestPoint);
		
		// show distance in view
		final String DEGREE  = "\u00b0";
		TextView directionTo = (TextView) findViewById(R.id.directionToTxtField);
		directionTo.setText(directionToStr + " (" + Float.toString(bearingToClosestPoint) + DEGREE + ")");

	}

	/*
	 * private String displayRoutePoints() {
	 * 
	 * String res = "";
	 * 
	 * for (Location loc : routePoints) { res += loc.toString() + "  " +
	 * loc.getBearing() + "  " + "\n"; } return res; }
	 */

	@Override
	public void onDestroy() {
		super.onDestroy();
		// stop gps polling
		System.out.println("Stopping GPS poll");

		mSensorManager.unregisterListener(mSensorListener);

		// close csv file
		// write the location to the cvs file
		try {
			if (reader != null) {
				reader.close();
			}
			/*
			 * // get the files in the internal storage
			 * 
			 * File fileList = getFilesDir(); if (fileList != null) { File[]
			 * filenames = fileList.listFiles(); for (File tmpf : filenames) {
			 * // Do something with the files System.out.println("file: " +
			 * tmpf.getName()); } }
			 */
		} catch (IOException e) {
			e.printStackTrace();
		}

		finish();

	}

	private final LocationListener mLocationListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location location) {
			// update current location
			currentLocation = location;

			// update closest point
			Log.i("GPS Location Change", "Updating closest point");
			getClosetPoint();

			// update travel to direction
			Log.i("GPS Location Change", "Updating travel direction");
			getTravelDirection();

			// display them on interface
			/*
			 * TextView directionToTravelField = (TextView)
			 * findViewById(R.id.DirectionTxtField);
			 * directionToTravelField.setText(directionTo);
			 * 
			 * TextView compassDirectionField = (TextView)
			 * findViewById(R.id.CompassTxtField);
			 * compassDirectionField.setText(compassDirection);
			 * 
			 * TextView distanceToTravelField = (TextView)
			 * findViewById(R.id.DistanceTxtField);
			 * distanceToTravelField.setText(Float.toString(minDistance));
			 */
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// Do nothing

		}

		@Override
		public void onProviderEnabled(String s) {
			Toast toast = Toast.makeText(getApplicationContext(),
					"GPS turned on. GPS tracking has started.",
					Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
		}

		@Override
		public void onProviderDisabled(String s) {
			System.out.println("GPS disabled");
			Toast toast = Toast.makeText(getApplicationContext(),
					"GPS turned off. GPS tracking has stopped.",
					Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
		}
	};

	public void startUpdates() {
		boolean gpsEnabled = this.mLocationManager
				.isProviderEnabled(LocationManager.GPS_PROVIDER);

		// check if the GPS was enabled
		if (gpsEnabled) {
			// Get gps location if GPS is enabled
			this.mLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, THREE_SECONDS, TWO_METERS,
					mLocationListener);
		}
	}

	// Stop receiving location updates whenever the Activity becomes invisible.
	protected void stopUpdates() {
		this.mLocationManager.removeUpdates(mLocationListener);

	}

	float[] mGravity;
	float[] mGeomagnetic;
	float azimut;
	private final SensorEventListener mSensorListener = new SensorEventListener() {

		@Override
		public void onSensorChanged(SensorEvent event) {

			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
				mGravity = event.values;
			if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
				mGeomagnetic = event.values;
			if (mGravity != null && mGeomagnetic != null) {
				float Rotation[] = new float[9];
				float Inclination[] = new float[9];
				boolean success = SensorManager.getRotationMatrix(Rotation,
						Inclination, mGravity, mGeomagnetic);
				if (success) {
					/*
					 * System.out.println("Rotation"); for(float val:Rotation){
					 * System.out.println(val); }
					 * System.out.println("Inclination"); for(float
					 * val:Inclination){ System.out.println(val); }
					 */
					float orientation[] = new float[3];
					SensorManager.getOrientation(Rotation, orientation);

					azimut = orientation[0]; // orientation contains: azimut,
												// pitch and roll

					// update compass view
					ImageView compassImage = (ImageView) findViewById(R.id.imageViewCompass);

					// get the angle around the z-axis rotated
					float degree = (float) Math.round(Math.toDegrees(azimut)); // Math.round(event.values[0]);
					// To avoid a shaking compass, use the threshold in
					// difference for new and current degree
					float threshold = 5;
					if (Math.abs(currentDegree - degree) > threshold) {

						// create a rotation animation (reverse turn degree
						// degrees)
						RotateAnimation ra = new RotateAnimation(currentDegree,
								-degree, Animation.RELATIVE_TO_SELF, 0.5f,
								Animation.RELATIVE_TO_SELF, 0.5f);
						// how long the animation will take place
						ra.setDuration(210);
						// set the animation after the end of the reservation
						// status
						ra.setFillAfter(true);
						// Start the animation
						compassImage.startAnimation(ra);
					}
					currentDegree = -degree;
				}
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// do nothing
		}
	};
	
	public String degreeToDirection(float degree){
		String direction = "";
		
		if(degree > 330 || degree < 30)
		{
			direction = "N"; // North
		}
		else if(degree > 30 && degree < 60)
		{
			direction = "NE"; // North
		}
		else if(degree > 60 && degree < 120)
		{
			direction = "E"; // North
		}
		else if(degree > 120 && degree < 150)
		{
			direction = "SE"; // North
		}
		else if(degree > 150 && degree < 210)
		{
			direction = "S"; // North
		}
		else if(degree > 210 && degree < 240)
		{
			direction = "SW"; // North
		}
		else if(degree > 240 && degree < 300)
		{
			direction = "W"; // North
		}
		else if(degree > 300 && degree < 330)
		{
			direction = "NW"; // North
		}
		
		return direction;
	}

}
