package simpleapps.backchannel;

import android.app.WallpaperManager;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.app.WallpaperManager.FLAG_LOCK;

/**
 * Created by jupiterio on 25.07.18.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    Bitmap bitmap;

    @Override
    public void onMessageReceived(RemoteMessage message) {

        SharedPreferences settings = getSharedPreferences("USERNAME", 0);
        String username = settings.getString("username", null);

        String type = message.getData().get("TYPE").toString();
        String[] params;
        Response resp;
        switch (type){
            case "private_bc":
                params = new String[]{username, ""};
                resp = networkFetchAsset("private", params);
                updateFeed("priv", resp);
                updateLockScreenImage(resp);
                break;
            case "notification":
                //do nothing --> user gets notified
                break;
            case "new_content":
                String partner = message.getData().get("PARTNER").toString();
                params = new String[]{partner, username};
                resp = networkFetchAsset("public", params);
                updateFeed("pub", resp);
                updateLockScreenImage(resp);
                break;
            default:
                break;
        }

    }

    private Response networkFetchAsset(String path, String[] params) {
        //FETCH IMAGE RESOURCE WITH URL

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://165.227.172.171:8000/" + path + "/" + params[0] + "/" + params[1])
                .build();

        Response response = null;
        try {
            response = client.newCall(request).execute();

            InputStream inputStream = response.body().byteStream();
            bitmap = BitmapFactory.decodeStream(inputStream);

            try {
                inputStream.close();
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }



    //WRITE NEW RESPECTIVE CONTENT TO FEED.HTML
    private void updateFeed(String type, Response resp){
        File f = getApplicationContext().getExternalFilesDir(null);
        File feed_file = new File(f.getAbsolutePath() + "/BackChannel_Feed/feed.html");
        if(feed_file.exists()) {
            if(type.equals("priv")){
                try{

                    String snippet = resp.header("snippet");
                    FileOutputStream fOut = new FileOutputStream(feed_file);
                    OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                    myOutWriter.append("<!doctype html><html lang='en'><head><meta charset='utf-8'><title></title>" +
                                        "<style type='text/css'> body{margin: 0;} *{-webkit-user-select: none;} p{text-align:center;} img{position: relative;width: 100%;} " +
                                        "\t.message{margin:8px;font-size:15px;font-family: arial;font-weight:normal;padding: 0;}  \t.friend{font-size:11px;font-family: monospace;font-weight:bold;normal;padding: 0;margin: 2px;}  "+
                                        "\t.time{font-family: monospace;font-size:9px;font-weight:normal;normal;padding: 0;}</style></head><body>"+
                                        "<div>"+snippet+"</body></html>");
                    myOutWriter.close();
                    fOut.close();
                } catch(Exception e){}


            }else if(type.equals("pub")){
                try{
                    String url = resp.header("url");
                    FileOutputStream fOut = new FileOutputStream(feed_file);
                    OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                    myOutWriter.append("<!doctype html><html lang='en'><head><meta charset='utf-8'><title></title>" +
                            "<style> body{margin: 0;} iframe{width:100%;height:-webkit-fill-available;}</style></head><body>" +
                            "<iframe width=\"100%\" height=\"100%\" style=\"border: white;\" src="+url+"></iframe></body></html>");
                    myOutWriter.close();
                    fOut.close();
                } catch(Exception e){}
            }
        }
    }


    private void updateLockScreenImage(Response resp){
        SharedPreferences screenSettings = getSharedPreferences("SCREEN", 0);
        final int screenWidth = screenSettings.getInt("width", 0);
        final int screenHeight = screenSettings.getInt("height", 0);

        WallpaperManager wpm = WallpaperManager.getInstance(this);
        try {
            //wpm.setBitmap(bitmap);
            wpm.setBitmap(bitmap, null, true, FLAG_LOCK);
        } catch (IOException e) {
            System.out.println("BITMAP ERROR: " + e);
        }
    }



}
