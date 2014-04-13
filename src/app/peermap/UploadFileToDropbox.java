package app.peermap;
 
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
 

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;
 
public class UploadFileToDropbox extends AsyncTask<Void, Void, Boolean> {
 
    private DropboxAPI<?> dropbox;
    private String path;
    private Context context;
 
    public UploadFileToDropbox(Context context, DropboxAPI<?> dropbox,
            String path) {
        this.context = context.getApplicationContext();
        this.dropbox = dropbox;
        this.path = path;
    }
 
    @Override
    protected Boolean doInBackground(Void... params) {

        try {
        File dir = Environment.getExternalStorageDirectory();
		File file = new File(dir,"peermap.csv");
		FileInputStream inputStream =new FileInputStream(file);
		dropbox.putFile(path + "peermap.csv",inputStream,
                file.length(), null, null);
        return true;	
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
	            e.printStackTrace();
	    } catch (DropboxException e) {
	            e.printStackTrace();
			
		}
		
        return false;
    }
 
    @Override
    protected void onPostExecute(Boolean result) {
        if (result) {
            Toast.makeText(context, "File Uploaded Sucesfully!",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, "Failed to upload file", Toast.LENGTH_LONG).show();
        }
    }
}