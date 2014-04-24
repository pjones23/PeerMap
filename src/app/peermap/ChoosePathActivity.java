package app.peermap;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DropboxFileInfo;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.TokenPair;
import com.dropbox.client2.session.Session.AccessType;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class ChoosePathActivity extends Activity {

	private DropboxAPI<AndroidAuthSession> dropbox;

	private final static String FILE_DIR = "/";
	private final static String DROPBOX_NAME = "dropbox_prefs";
	private final static String ACCESS_KEY = "anm0hstr5qr8kb9";
	private final static String ACCESS_SECRET = "xz7yev2wz07ycmx";
	private boolean isLoggedIn;
	private Button logIn;
	private Button peerBtn;

	private ArrayList<String> storedPathNames;
	private ArrayList<String> cloudPathNames;
	private ArrayList<String> allPaths;

	private ArrayAdapter<String> pathListViewAdapter;
	private ListView pathListView;
	
	private LocationManager gpsLocationManager;

	/** Called when the activity is first created. */
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_choose_path);

		storedPathNames = new ArrayList<String>();
		cloudPathNames = new ArrayList<String>();
		allPaths = new ArrayList<String>();

		pathListViewAdapter = new ArrayAdapter<String>(getApplicationContext(),
				R.layout.simple_list_item, allPaths);
		pathListView = (ListView) findViewById(R.id.PathListView);
		pathListView.setAdapter(pathListViewAdapter);
		setPathListViewClickListener();

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

		logIn = (Button) findViewById(R.id.CloudLogBtn_Choose_Path);
		logIn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (isLoggedIn) {
					dropbox.getSession().unlink();
					loggedIn(false);
				} else {
					dropbox.getSession().startAuthentication(
							ChoosePathActivity.this);
				}
			}
		});
		
		peerBtn = (Button) findViewById(R.id.SearchPeerBtn);
		peerBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// start the Wifi P2P
				startWifiDirect();
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();

		// get your stored path names
		getStoredFiles();

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

				// get cloud path names
				new CloudFiles(this).execute("");

			} catch (IllegalStateException e) {
				Toast.makeText(this, "Error during Dropbox authentication",
						Toast.LENGTH_SHORT).show();
			}
		}

		// populate list view

		// populate listview with your paths
		for (String sp : storedPathNames)
			System.out.println(sp);
		// populate listview with cloud paths
	}

	private void setPathListViewClickListener() {
		pathListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {

				long pathListIndex = arg3;
				String chosenPath = allPaths.get((int) pathListIndex);
				System.out.println(chosenPath);

				// check if chosen is in cloud
				boolean inCloud = chosenPath.contains("[cloud]");
				System.out.println(inCloud);
				// if inCloud, download to sdcard and refresh list, else start
				// the Path activity
				if (inCloud) {
					String parsedChosenPath = chosenPath.substring(0,
							chosenPath.length() - 8);
					System.out.println("Download: " + parsedChosenPath);
					DownloadCloudFile download = new DownloadCloudFile(
							ChoosePathActivity.this, dropbox, FILE_DIR,
							parsedChosenPath);
					download.execute();

				} else {
					// Check if GPS is enabled
					gpsLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
					boolean gpsEnabled = gpsLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

					// If not, prompt user to enable GPS
					if (!gpsEnabled) {
						// If GPS is not enabled

						Toast toast = Toast.makeText(getApplicationContext(),
								"GPS not available", Toast.LENGTH_SHORT);
						toast.setGravity(Gravity.CENTER, 0, 0);
						toast.show();

						// Create alert dialog here
						enableLocationSettings();
					} else {
						// start Path activity
						Intent pathIntent = new Intent(getApplicationContext(), PathActivity.class);
						pathIntent.putExtra("pathName", chosenPath);
						startActivity(pathIntent);
					}									
				}
			}

		});
	}

	public void loggedIn(boolean isLogged) {
		isLoggedIn = isLogged;
		logIn.setText(isLogged ? R.string.CloudLogout : R.string.CloudLogin);
	}

	public void getStoredFiles() {
		if (android.os.Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED)) {
			storedPathNames.clear();
			File dir = new File(
					android.os.Environment.getExternalStorageDirectory(),
					"PeerMap/SavedPaths");
			File listFile[] = dir.listFiles();
			for (File file : listFile) {
				storedPathNames.add(file.getName());
			}
			updateAllPaths();
		} else {
			Toast.makeText(this, "SD card is not mounted", Toast.LENGTH_SHORT)
					.show();
		}
	}

	private void updateAllPaths() {
		allPaths.clear();
		allPaths.addAll(storedPathNames);
		allPaths.addAll(cloudPathNames);
		pathListViewAdapter.notifyDataSetChanged();
	}
	
	private void enableLocationSettings() {
		Intent settingsIntent = new Intent(
				android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(settingsIntent);
	}
	
	private void startWifiDirect(){
		Intent startP2P = new Intent(getApplicationContext(), WiFiDirectActivity.class);
		startActivity(startP2P);
	}

	private class CloudFiles extends AsyncTask<String, Integer, Boolean> {

		private Activity activity;

		public CloudFiles(Activity activity) {
			System.out.println("here");
			this.activity = activity;
		}

		protected void onPreExecute() {
			Toast.makeText(activity.getApplicationContext(),
					"Reading cloud files", Toast.LENGTH_SHORT).show();
		}

		@Override
		protected void onPostExecute(Boolean success) {
			if (success) {
				updateAllPaths();
				Toast.makeText(activity.getApplicationContext(),
						"Done reading cloud files", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(activity.getApplicationContext(),
						"Error reading cloud files", Toast.LENGTH_SHORT).show();
			}
		}

		@Override
		protected Boolean doInBackground(String... params) {
			return getCloudFiles();
		}

		public boolean getCloudFiles() {
			System.out.println("inside getCloudFiles");
			try {
				System.out.println("1");
				Entry dropboxDir1 = dropbox.metadata(FILE_DIR, 0, null, true,
						null);
				if (dropboxDir1.isDir) {
					System.out.println("___isdir");

					List<Entry> contents1 = dropboxDir1.contents;

					if (contents1 != null) {
						cloudPathNames.clear();

						for (int i = 0; i < contents1.size(); i++) {
							Entry e = contents1.get(i);

							String a = e.fileName();
							System.out.println("cloud: " + a);
							cloudPathNames.add(a + " [cloud]");
						}
					}
				}
				return true;
			} catch (Exception ex) {
				ex.printStackTrace();
				return false;
			}
		}

	}

	public class DownloadCloudFile extends AsyncTask<Void, Void, Boolean> {

		private DropboxAPI<?> dropbox;
		private String path;
		private String fileName;
		private Context context;

		public DownloadCloudFile(Context context, DropboxAPI<?> dropbox,
				String path, String fileName) {
			this.context = context.getApplicationContext();
			this.dropbox = dropbox;
			this.path = path;
			this.fileName = fileName;
		}

		protected void onPreExecute() {
			Toast.makeText(context,
					"Downloading " + fileName, Toast.LENGTH_SHORT).show();
		}
		
		protected Boolean doInBackground(Void... params) {

			File dir = Environment.getExternalStorageDirectory();
			File file = new File(dir, "PeerMap/SavedPaths/" + fileName);
			

			OutputStream out = null;

			try {
				out = new BufferedOutputStream(new FileOutputStream(file));
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
			try {
				DropboxFileInfo info = dropbox.getFile(path + fileName, null,
						out, null);
				Log.i("DbExampleLog",
						"The file's rev is: " + info.getMetadata().rev);
				out.close();
				return true;
			} catch (DropboxException e) {
				Log.e("DbExampleLog", "Something went wrong while downloading.");
				// file.delete();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}

		}

		protected void onPostExecute(Boolean result) {
			if (result) {
				getStoredFiles();
				updateAllPaths();
				Toast.makeText(context, "File Download Successful!",
						Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(context, "Failed to Download file",
						Toast.LENGTH_LONG).show();
			}
		}
	}
}
