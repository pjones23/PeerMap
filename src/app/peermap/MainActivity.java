package app.peermap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import app.peermap.R;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.dropbox.client2.session.TokenPair;

public class MainActivity extends Activity {

	private LocationManager gpsLocationManager;

	private DropboxAPI<AndroidAuthSession> dropbox;

	private final static String FILE_DIR = "/";
	private final static String DROPBOX_NAME = "dropbox_prefs";
	private final static String ACCESS_KEY = "anm0hstr5qr8kb9";
	private final static String ACCESS_SECRET = "xz7yev2wz07ycmx";
	private boolean isLoggedIn;
	private Button logIn;
	private Button saveFile;
	private Button uploadFile;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final ToggleButton recordBtn = (ToggleButton) findViewById(R.id.recordBtn);

		if (isLocationPollServiceRunning()) {
			recordBtn.setChecked(true);
		}

		this.gpsLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		recordBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent locationPollIntent = new Intent(getApplicationContext(),
						LocationPollService.class);
				// start/stop the recording
				if (recordBtn.isChecked()) {

					// Check if GPS is enabled
					boolean gpsEnabled = gpsLocationManager
							.isProviderEnabled(LocationManager.GPS_PROVIDER);

					// If not, prompt user to enable GPS
					if (!gpsEnabled) {
						// If GPS is not enabled
						// set record button back to off
						recordBtn.setChecked(false);

						Toast toast = Toast.makeText(getApplicationContext(),
								"GPS not available", Toast.LENGTH_SHORT);
						toast.setGravity(Gravity.CENTER, 0, 0);
						toast.show();

						// Create alert dialog here
						enableLocationSettings();
					} else {
						// start recording
						System.out.println("Start Recording");
						startService(locationPollIntent);
					}
				} else {
					System.out.println("Stop Recording");
					stopService(locationPollIntent);
				}

			}
		});

		Button choosePathBtn = (Button) findViewById(R.id.choosePathBtn_Home);
		choosePathBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				Intent choosePathIntent = new Intent(getApplicationContext(),
						ChoosePathActivity.class);
				startActivity(choosePathIntent);

			}
		});

		saveFile = (Button) findViewById(R.id.saveBtn);
		saveFile.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				TextView pathNameField = (TextView) findViewById(R.id.SaveFileTxtField);
				String pathName = pathNameField.getText().toString();
				if (pathName != null && !pathName.isEmpty()) {
					// Transfer saved file to sd card
					File csvFolder = new File(Environment
							.getExternalStorageDirectory(), "PeerMap");
					File csvFile = new File(csvFolder, "path" + ".csv");
					if (csvFile.exists()) {
						new SavePathToSD(MainActivity.this).execute("");
					} else {
						Toast.makeText(getApplicationContext(),
								"No recorded path found.", Toast.LENGTH_SHORT)
								.show();
					}

				} else {
					notifyToEnterPathName();
				}
			}
		});

		uploadFile = (Button) findViewById(R.id.uploadBtn);
		uploadFile.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				TextView pathNameField = (TextView) findViewById(R.id.SaveFileTxtField);
				String pathName = pathNameField.getText().toString();
				if (pathName != null && !pathName.isEmpty()) {
					File csvFolder = new File(Environment
							.getExternalStorageDirectory(), "PeerMap");
					File csvFile = new File(csvFolder, "path" + ".csv");
					if (csvFile.exists()) {
						UploadFileToDropbox upload = new UploadFileToDropbox(
								getApplicationContext(), dropbox, FILE_DIR,
								pathName);
						upload.execute();
					} else {
						Toast.makeText(getApplicationContext(),
								"No recorded path found.", Toast.LENGTH_SHORT)
								.show();
					}
				} else {
					notifyToEnterPathName();
				}
			}
		});

		logIn = (Button) findViewById(R.id.cloudLogBtn);
		logIn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (isLoggedIn) {
					dropbox.getSession().unlink();
					loggedIn(false);
				} else {
					dropbox.getSession().startAuthentication(MainActivity.this);
				}
			}
		});

		loggedIn(false);

		AndroidAuthSession session;
		AppKeyPair pair = new AppKeyPair(ACCESS_KEY, ACCESS_SECRET);

		SharedPreferences prefs = getSharedPreferences(DROPBOX_NAME, 0);
		String key = prefs.getString(ACCESS_KEY, null);
		String secret = prefs.getString(ACCESS_SECRET, null);

		if (key != null && secret != null) {
			AccessTokenPair token = new AccessTokenPair(key, secret);
			session = new AndroidAuthSession(pair, AccessType.APP_FOLDER, token);
		} else {
			session = new AndroidAuthSession(pair, AccessType.APP_FOLDER);
		}

		dropbox = new DropboxAPI<AndroidAuthSession>(session);
	}

	@Override
	protected void onResume() {
		super.onResume();

		AndroidAuthSession session = dropbox.getSession();

		if (session.isLinked()) {
			loggedIn(true);
		}

		if (session.authenticationSuccessful()) {
			try {
				session.finishAuthentication();

				TokenPair tokens = session.getAccessTokenPair();
				SharedPreferences prefs = getSharedPreferences(DROPBOX_NAME, 0);
				Editor editor = prefs.edit();
				editor.putString(ACCESS_KEY, tokens.key);
				editor.putString(ACCESS_SECRET, tokens.secret);
				editor.commit();

				loggedIn(true);
			} catch (IllegalStateException e) {
				Toast.makeText(this, "Error during Dropbox authentication",
						Toast.LENGTH_SHORT).show();
			}
		}
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
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (LocationPollService.class.getName().equals(
					service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	public void loggedIn(boolean isLogged) {
		isLoggedIn = isLogged;
		uploadFile.setEnabled(isLogged);
		logIn.setText(isLogged ? R.string.CloudLogout : R.string.CloudLogin);
	}

	public void notifyToEnterPathName() {
		Toast.makeText(this, "Enter a path name.", Toast.LENGTH_SHORT).show();
	}

	private class SavePathToSD extends AsyncTask<String, Integer, Boolean> {

		private Activity activity;

		public SavePathToSD(Activity activity) {
			System.out.println("here");
			this.activity = activity;
		}

		protected void onPreExecute() {
			Toast.makeText(activity.getApplicationContext(), "Saving path",
					Toast.LENGTH_SHORT).show();
		}

		@Override
		protected void onPostExecute(Boolean success) {
			if (success) {
				Toast.makeText(activity.getApplicationContext(),
						"Done saving path", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(activity.getApplicationContext(),
						"Error saving path", Toast.LENGTH_SHORT).show();
			}
		}

		@Override
		protected Boolean doInBackground(String... params) {
			return saveToSD();
		}

		public boolean saveToSD() {

			try {
				// get recorded Path
				File csvFolder = new File(
						Environment.getExternalStorageDirectory(), "PeerMap");
				File csvFile = new File(csvFolder, "path" + ".csv");

				File csvSaveFolder = new File(
						Environment.getExternalStorageDirectory(),
						"PeerMap/SavedPaths");

				if (!csvSaveFolder.exists()) {
					csvSaveFolder.mkdirs();
				}

				TextView pathNameField = (TextView) findViewById(R.id.SaveFileTxtField);
				String pathName = pathNameField.getText().toString();
				File csvSaveFile = new File(csvSaveFolder, pathName + ".csv");
				if (!csvSaveFile.exists()) {
					csvSaveFile.createNewFile();
				}

				FileInputStream inStream = new FileInputStream(csvFile);
				FileOutputStream outStream = new FileOutputStream(csvSaveFile);
				FileChannel inChannel = inStream.getChannel();
				FileChannel outChannel = outStream.getChannel();
				inChannel.transferTo(0, inChannel.size(), outChannel);
				inStream.close();
				outStream.close();
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}

	}

}
