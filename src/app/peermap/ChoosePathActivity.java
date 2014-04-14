package app.peermap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.TokenPair;
import com.dropbox.client2.session.Session.AccessType;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
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
	
	private ArrayList<String> storedPathNames;
	private ArrayList<String> cloudPathNames;
	private ArrayList<String> allPaths;
	
	private ArrayAdapter<String> pathListViewAdapter;
	private ListView pathListView;

	/** Called when the activity is first created. */
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_choose_path);
		
		storedPathNames = new ArrayList<String>();
		cloudPathNames = new ArrayList<String>();
		allPaths = new ArrayList<String>();
		
		pathListViewAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.simple_list_item, allPaths);
		pathListView = (ListView) findViewById(R.id.PathListView);
		pathListView.setAdapter(pathListViewAdapter);
		
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
					dropbox.getSession().startAuthentication(ChoosePathActivity.this);
				}
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();

		// get your stored path names
		getStoredFiles();		
		
		AndroidAuthSession session = dropbox.getSession();
		
		if(session.isLinked()){
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
		for(String sp : storedPathNames)
			System.out.println(sp);
		// populate listview with cloud paths
	}
	

	public void loggedIn(boolean isLogged) {
		isLoggedIn = isLogged;
		logIn.setText(isLogged ? R.string.CloudLogout : R.string.CloudLogin);
	}
	
	public void getStoredFiles(){
		if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
		{
			storedPathNames.clear();
			File dir= new File(android.os.Environment.getExternalStorageDirectory(),"PeerMap/SavedPaths");
			File listFile[] = dir.listFiles();
			for(File file:listFile){
				storedPathNames.add(file.getName());
			}
			updateAllPaths();
		}
		else{
			Toast.makeText(this, "SD card is not mounted",
					Toast.LENGTH_SHORT).show();
		}
	}
	
	private void updateAllPaths(){
		allPaths.clear();
		allPaths.addAll(storedPathNames);
		allPaths.addAll(cloudPathNames);
		pathListViewAdapter.notifyDataSetChanged();
	}
	
	
	private class CloudFiles extends AsyncTask<String, Integer, Boolean>{

        private Activity activity;
        public CloudFiles(Activity activity) {
        	System.out.println("here");
            this.activity = activity;
        }
        
		protected void onPreExecute() {
            Toast.makeText(activity.getApplicationContext(), "Reading cloud files", Toast.LENGTH_SHORT).show();            
        }
		
		@Override
		protected void onPostExecute(Boolean success) {
			System.out.println("post execute");
			updateAllPaths();

			if (success) {
				Toast.makeText(activity.getApplicationContext(), "Done reading cloud files", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(activity.getApplicationContext(), "Error reading cloud files", Toast.LENGTH_SHORT).show();
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
				Entry dropboxDir1 = dropbox.metadata(FILE_DIR, 0, null, true, null);
				if (dropboxDir1.isDir) {
					System.out.println("___isdir");

					List<Entry> contents1 = dropboxDir1.contents;

					if (contents1 != null) {
						cloudPathNames.clear();

						for (int i = 0; i < contents1.size(); i++) {
							Entry e = contents1.get(i);

							String a = e.fileName();
							System.out.println("cloud: " + a);
							cloudPathNames.add(a + "|[cloud]");
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

}
