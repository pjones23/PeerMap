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
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVReader;

public class DirectionActivity extends Activity {

	private LocationManager mLocationManager;
	private SensorManager mSensorManager;
	private CSVReader reader;
	private List<Location> routePoints;
	private Location currentLocation;
	private Location closestPoint;
	private float minDistance;
	private String directionTo;
	private String compassDirection;
	// record the compass picture angle turned
    private float currentDegree = 0f;

	private static final int THREE_SECONDS = 3000;
	private static final int TEN_METERS = 10;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_path);

		routePoints = new ArrayList<Location>();

		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				THREE_SECONDS, TEN_METERS, mLocationListener);

		closestPoint = null;
		minDistance = 0;
		directionTo = "";
		compassDirection = "";

		currentLocation = mLocationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		// Create route array from csv file
		// Read file
		try {
			/*
			 * File csvFolder = new File("/storage/emulated/0/",
			 * "airdroid/upload"); Log.v("AIC", csvFolder.toString());
			 */
			// TODO change to different static file location for my testing
			File csvFile = new File("/system", "path" + ".csv");
			reader = new CSVReader(new FileReader(csvFile), ';');
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		Log.e("LOG", "file read");
		// Parse csv and store into array list
		String[] nextLine;
		try {
			while ((nextLine = reader.readNext()) != null) {
				// nextLine[] is an array of values from the line
				String[] parts = nextLine[0].split(";");
				/*
				 * Log.e("DOUBLE", parts[0]); Log.e("DOUBLE1", parts[1]);
				 */

				Location l = new Location(LocationManager.GPS_PROVIDER);
				l.setLatitude(Double.parseDouble(parts[0]));
				l.setLongitude(Double.parseDouble(parts[1]));
				routePoints.add(l);
			}
		} catch (IOException e) {
			e.printStackTrace();
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
		} else {
			// say we can't start tracking until we get an initial location or
			// closest point
		}

	}

	public void getTravelDirection() {
		// TODO implement getting current direction
		// String direction = "";
		Log.e("LOG", "log\n");
		/*
		 * Location l1 =
		 * mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		 * if(l1 != null)Log.e("LOG", "Get Location: \n" + l1.toString()); else
		 * Log.e("LOG", "l1 is null");
		 * 
		 * 
		 * LatLng loc = new LatLng(Double.parseDouble("33.77794599"),
		 * Double.parseDouble("-84.40939124")); Location l1 = new
		 * Location("reverseGeocoded"); l1.setLatitude(loc.latitude);
		 * l1.setLongitude(loc.longitude); displayRoutePoints(); for (Location
		 * lo : routePoints) { Log.e("Calculation", l1.bearingTo(lo) + " " +
		 * l1.distanceTo(lo) + "\n"); }
		 */
		float bearingToClosestPoint = currentLocation.bearingTo(closestPoint);
		System.out.println(bearingToClosestPoint);

	}

	public void getCompassDirection() {
		// TODO implement getting current direction
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
		// stop gps polling
		System.out.println("Stopping GPS poll");
		mLocationManager.removeUpdates(mLocationListener);

		// close csv file
		// write the location to the cvs file
		try {
			reader.close();

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

	}

	private final LocationListener mLocationListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location location) {
			// update current location
			currentLocation = location;

			// update closet point
			getClosetPoint();

			// update travel to direction
			getTravelDirection();

			// update compass direction
			getCompassDirection();

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

	private final SensorEventListener mSensorListener = new SensorEventListener() {

		@Override
		public void onSensorChanged(SensorEvent event) {

			ImageView compassImage = (ImageView) findViewById(R.id.imageViewCompass);

			// get the angle around the z-axis rotated
			float degree = Math.round(event.values[0]);

			// create a rotation animation (reverse turn degree degrees)
			RotateAnimation ra = new RotateAnimation(currentDegree, -degree,
					Animation.RELATIVE_TO_SELF, 0.5f,
					Animation.RELATIVE_TO_SELF, 0.5f);
			// how long the animation will take place
			ra.setDuration(210);
			// set the animation after the end of the reservation status
			ra.setFillAfter(true);
			// Start the animation
			compassImage.startAnimation(ra);
			currentDegree = -degree;

		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// do nothing
		}
	};

}
