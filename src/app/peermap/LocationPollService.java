package app.peermap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.view.Gravity;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVWriter;

public class LocationPollService extends Service {

	private LocationManager gpsLocationManager;

	private static final int TEN_SECONDS = 10000;
	private static final int TEN_METERS = 10;
	private static final int TWO_MINUTES = 1000 * 60 * 2;

	private CSVWriter writer;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		// Make the directory

		try {
			File csvFolder = new File(
					Environment.getExternalStorageDirectory(), "PeerMap");
			if (!csvFolder.exists()) {
				csvFolder.mkdir();
			}
			try {
				File csvFile = new File(csvFolder, "path" + ".csv");
				csvFile.createNewFile();
				writer = new CSVWriter(new FileWriter(csvFile));
			} catch (Exception ex) {
				System.out.println("ex: " + ex);
			}
		} catch (Exception e) {
			System.out.println("e: " + e);
		}

		this.gpsLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// set up csv file to store locations
		System.out.println("Creating cvs file");

		// start gps polling
		System.out.println("Polling GPS");
		Toast toast = Toast.makeText(getApplicationContext(),
				"Started Tracking", Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
		startUpdates();

		return Service.START_REDELIVER_INTENT;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// This is a non bounded service
		return null;
	}

	@Override
	public void onDestroy() {
		// stop gps polling
		System.out.println("Stopping GPS poll");
		stopUpdates();

		// close csv file
		// write the location to the cvs file
		try {
			writer.close();

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

		Toast toast = Toast.makeText(getApplicationContext(),
				"Finished Tracking", Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}

	private final LocationListener listener = new LocationListener() {

		@Override
		public void onLocationChanged(Location location) {
			System.out.println("updated location: " + location.getLatitude()
					+ ", " + location.getLongitude());
			Toast toast = Toast.makeText(getApplicationContext(),
					"updated location: " + location.getLatitude() + ", "
							+ location.getLongitude(), Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();

			String[] locationString = { Double.toString(location.getLatitude())
					+ ";" + Double.toString(location.getLongitude()) };
			writer.writeNext(locationString);
		}

		@Override
		public void onStatusChanged(String s, int i, Bundle bundle) {
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
		boolean gpsEnabled = this.gpsLocationManager
				.isProviderEnabled(LocationManager.GPS_PROVIDER);

		// check if the GPS was enabled
		if (gpsEnabled) {
			// Get gps location if GPS is enabled
			this.gpsLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, TEN_SECONDS, TEN_METERS,
					listener);
		}
	}

	// Stop receiving location updates whenever the Activity becomes invisible.
	protected void stopUpdates() {
		this.gpsLocationManager.removeUpdates(listener);

	}

	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix. Code taken from
	 * http://developer.android.com/guide/topics/location
	 * /obtaining-user-location.html
	 * 
	 * @param newLocation
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 * @return The better Location object based on recency and accuracy.
	 */
	protected Location getBetterLocation(Location newLocation,
			Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return newLocation;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = newLocation.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved.
		if (isSignificantlyNewer) {
			return newLocation;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return currentBestLocation;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (newLocation.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(newLocation.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return newLocation;
		} else if (isNewer && !isLessAccurate) {
			return newLocation;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
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
