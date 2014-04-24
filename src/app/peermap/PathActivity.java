package app.peermap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
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

	private LocationManager mGPSLocationManager;
	private LocationManager mNetworkLocationManager;
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
	
	private boolean validPath; 

	private static final int THREE_SECONDS = 3000;
	private static final int TWO_METERS = 2;
	private static final int TWO_MINUTES = 1000 * 60 * 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_path);

		routePoints = new ArrayList<Location>();

		mGPSLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		mNetworkLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
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

		validPath = true;

		// Get path
		Intent pathIntent = getIntent();
		String pathName = pathIntent.getStringExtra("pathName");
		TextView chosenPathTextView = (TextView) findViewById(R.id.chosenPathTxt);
		chosenPathTextView.setText(pathName);

		if (pathName == null || pathName.isEmpty()
				|| !(pathName.endsWith(".csv"))) {
			validPath = false;
			setContentView(R.layout.error);
			TextView errorTxt = (TextView) findViewById(R.id.errorTxt);
			errorTxt.setText(R.string.InvalidFileTxt);
			TextView errorFileTxt = (TextView) findViewById(R.id.errorFileTxt);
			errorFileTxt.setText(pathName);
			return;
		}

		// Create route array from csv file
		// Read file
		try {
			// static file location for my testing File
			File csvFile = new File("/storage/emulated/0/PeerMap/SavedPaths",
					pathName);
			reader = new CSVReader(new FileReader(csvFile), ';');
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		Log.e("LOG", "file read"); // Parse csv and store into array list
		String[] nextLine;
		try {
			// This check is necessary for only certain versions of android
			// It was found when testing on a separate device
			if (reader == null) {
				Log.e("LOG", "null reader object");
			}
			if (reader != null) {
				while ((nextLine = reader.readNext()) != null) { // nextLine[]
																	// is an
																	// array of
																	// values
																	// from
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
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (Location loc : routePoints) {
			System.out.println("Lat: " + loc.getLatitude() + ", Long: "
					+ loc.getLongitude());
		}

		if (routePoints == null || routePoints.isEmpty()) {
			validPath = false;
			setContentView(R.layout.error);
			TextView errorTxt = (TextView) findViewById(R.id.errorTxt);
			errorTxt.setText(R.string.EmptyPathTxt);
			TextView errorFileTxt = (TextView) findViewById(R.id.errorFileTxt);
			errorFileTxt.setText(pathName);
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		System.out.println("Resuming!!!");

		if (validPath) {
			// for the system's orientation sensor registered listeners
			mSensorManager.registerListener(mSensorListener, accelerometer,
					SensorManager.SENSOR_DELAY_UI);
			mSensorManager.registerListener(mSensorListener, magnetometer,
					SensorManager.SENSOR_DELAY_UI);

			startUpdates();

			getCurrentLocation();
		}
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

	@Override
	public void onDestroy() {
		super.onDestroy();
		// stop gps polling
		System.out.println("Stopping GPS poll");

		mSensorManager.unregisterListener(mSensorListener);
		mGPSLocationManager.removeUpdates(mLocationListener);
		mNetworkLocationManager.removeUpdates(mLocationListener);

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
			getCurrentLocation();

			// update closest point
			Log.i("GPS Location Change", "Updating closest point");
			getClosetPoint();

			// update travel to direction
			Log.i("GPS Location Change", "Updating travel direction");
			getTravelDirection();

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
		boolean gpsEnabled = this.mGPSLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

		// check if the GPS was enabled
		if (gpsEnabled) {
			// Get gps location if GPS is enabled
			this.mGPSLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, THREE_SECONDS, TWO_METERS,
					mLocationListener);
		}
		boolean networkEnabled = this.mGPSLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

		// check if the GPS was enabled
		if (networkEnabled) {
			// Get gps location if GPS is enabled
			this.mNetworkLocationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, THREE_SECONDS, TWO_METERS,
					mLocationListener);
		}
		 if(!gpsEnabled && !networkEnabled){
			 Toast toast = Toast.makeText(getApplicationContext(),
						"No GPS or network location assistance available. Try again.",
						Toast.LENGTH_LONG);
				toast.setGravity(Gravity.CENTER, 0, 0);
				toast.show();
		 }
	}

	// Stop receiving location updates whenever the Activity becomes invisible.
	protected void stopUpdates() {
		this.mGPSLocationManager.removeUpdates(mLocationListener);
		this.mNetworkLocationManager.removeUpdates(mLocationListener);

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
					float threshold = 3;
					if (Math.abs(currentDegree - (-degree)) > threshold) {

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
						currentDegree = -degree;
					}
					//currentDegree = -degree;
					
					String currentDegreeStr = degreeToDirection((degree+360)%360);
					
					// show distance in view
					final String DEGREE  = "\u00b0";
					TextView directionTo = (TextView) findViewById(R.id.compassTxtField);
					directionTo.setText(currentDegreeStr + " (" + Float.toString((degree+360)%360) + DEGREE + ")");

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
	
	public void getCurrentLocation() {

		Location newGPSLoc = mGPSLocationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		Location newNETLoc = mNetworkLocationManager
				.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

		Location bestLoc;
		if (newGPSLoc == null) {
			bestLoc = newNETLoc;
			System.out.println("newNETLoc: " + newNETLoc.getLatitude() + ", "
					+ newNETLoc.getLongitude());

		} else {
			bestLoc = getBetterLocation(newNETLoc, newGPSLoc);
			System.out.println("newGPSLoc: " + newGPSLoc.getLatitude() + ", "
					+ newGPSLoc.getLongitude());
			System.out.println("newNETLoc: " + newNETLoc.getLatitude() + ", "
					+ newNETLoc.getLongitude());

		}

		System.out.println("bestLoc: " + bestLoc.getLatitude() + ", "
				+ bestLoc.getLongitude());
		currentLocation = bestLoc;
	}

	/** Determines whether one Location reading is better than the current Location fix.
     * Code taken from
     * http://developer.android.com/guide/topics/location/obtaining-user-location.html
     *
     * @param newLocation  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new
     *        one
     * @return The better Location object based on recency and accuracy.
     */
   protected Location getBetterLocation(Location newLocation, Location currentBestLocation) {
       if (currentBestLocation == null) {
           // A new location is always better than no location
           return newLocation;
       }

       // Check whether the new location fix is newer or older
       long timeDelta = newLocation.getTime() - currentBestLocation.getTime();
       boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
       boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
       boolean isNewer = timeDelta > 0;

       // If it's been more than two minutes since the current location, use the new location
       // because the user has likely moved.
       if (isSignificantlyNewer) {
           return newLocation;
       // If the new location is more than two minutes older, it must be worse
       } else if (isSignificantlyOlder) {
           return currentBestLocation;
       }

       // Check whether the new location fix is more or less accurate
       int accuracyDelta = (int) (newLocation.getAccuracy() - currentBestLocation.getAccuracy());
       boolean isLessAccurate = accuracyDelta > 0;
       boolean isMoreAccurate = accuracyDelta < 0;
       boolean isSignificantlyLessAccurate = accuracyDelta > 200;

       // Check if the old and new location are from the same provider
       boolean isFromSameProvider = isSameProvider(newLocation.getProvider(),
               currentBestLocation.getProvider());

       // Determine location quality using a combination of timeliness and accuracy
       if (isMoreAccurate) {
           return newLocation;
       } else if (isNewer && !isLessAccurate) {
           return newLocation;
       } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
           return newLocation;
       }
       return currentBestLocation;
   }
   
   
   /** Checks whether two providers are the same */
   private boolean isSameProvider(String provider1, String provider2) {
       if (provider1 == null) {
         return provider2 == null;
       }
       return provider1.equals(provider2);
   }

}
