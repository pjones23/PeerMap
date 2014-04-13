package app.peermap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;


import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DropboxFileInfo;
import com.dropbox.client2.exception.DropboxException;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class DownloadFileFromDropbox extends AsyncTask<Void, Void, Boolean>{
	
	private DropboxAPI<?> dropbox;
    private String path;
    private Context context;
 
    public DownloadFileFromDropbox(Context context, DropboxAPI<?> dropbox,
            String path) {
        this.context = context.getApplicationContext();
        this.dropbox = dropbox;
        this.path = path;
    }
	
    protected Boolean doInBackground(Void... params) {
    	
    	File dir = Environment.getExternalStorageDirectory();
		File file = new File(dir,"work2.csv");
  
        OutputStream out= null;
        
        try {
            out = new BufferedOutputStream(new FileOutputStream(file));
          } catch (FileNotFoundException e1) {
              // TODO Auto-generated catch block
            e1.printStackTrace();
          }
        try {
            DropboxFileInfo info = dropbox.getFile(path+"peermap.csv", null, out, null);
            Log.i("DbExampleLog", "The file's rev is: " + info.getMetadata().rev);
            return true;
          } catch (DropboxException e) {
            Log.e("DbExampleLog", "Something went wrong while downloading.");
            //file.delete();
            return false;
          }
   
    }
 
    protected void onPostExecute(Boolean result) {
        if (result) {
            Toast.makeText(context, "File Downloaded Sucesfully!",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, "Failed to Download file", Toast.LENGTH_LONG)
                    .show();
        }
    }

}
