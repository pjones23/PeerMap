package app.peermap;
 
import java.util.ArrayList;

import app.peermap.R;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.dropbox.client2.session.TokenPair;
 
public class DropboxActivity extends Service implements OnClickListener {
 
    private DropboxAPI<AndroidAuthSession> dropbox;
 
    private final static String FILE_DIR = "/";
    private final static String DROPBOX_NAME = "dropbox_prefs";
    private final static String ACCESS_KEY = "anm0hstr5qr8kb9";
    private final static String ACCESS_SECRET = "xz7yev2wz07ycmx";
    private boolean isLoggedIn;
    private Button logIn;
    private Button uploadFile;
    private Button downloadFile;
    private LinearLayout container;
 
    @Override
	public int onStartCommand(Intent intent, int flags, int startId) {
 
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
 
        session = dropbox.getSession();
        if (session.authenticationSuccessful()) {
            try {
                session.finishAuthentication();
 
                TokenPair tokens = session.getAccessTokenPair();
                Editor editor = prefs.edit();
                editor.putString(ACCESS_KEY, tokens.key);
                editor.putString(ACCESS_SECRET, tokens.secret);
                editor.commit();
 
                loggedIn(true);
            } catch (IllegalStateException e) {
                Toast.makeText(this, "Error during Dropbox authentication",
                        Toast.LENGTH_SHORT).show();
            }
            if (!isLoggedIn) {
                dropbox.getSession().startAuthentication(DropboxActivity.this);
            }
            //if upload
            UploadFileToDropbox upload = new UploadFileToDropbox(this, dropbox, FILE_DIR);
            upload.execute();
        }
        
        return Service.START_REDELIVER_INTENT;
    }
 
    public void loggedIn(boolean isLogged) {
        isLoggedIn = isLogged;
        uploadFile.setEnabled(isLogged);
        downloadFile.setEnabled(isLogged);
        logIn.setText(isLogged ? "Log out" : "Log in");
    }
 
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.dropbox_login:
 
            if (isLoggedIn) {
                dropbox.getSession().unlink();
                loggedIn(false);
            } else {
                dropbox.getSession().startAuthentication(DropboxActivity.this);
            }
 
            break;
        case R.id.download_file:
 
        	DownloadFileFromDropbox download = new DownloadFileFromDropbox(this, dropbox,
                    FILE_DIR);
            download.execute();
            break;
        case R.id.upload_file:
            UploadFileToDropbox upload = new UploadFileToDropbox(this, dropbox,
                    FILE_DIR);
            upload.execute();
            break;
 
        default:
            break;
        }
    }

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
}