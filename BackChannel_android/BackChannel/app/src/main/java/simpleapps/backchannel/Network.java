package simpleapps.backchannel;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by jupiterio on 22.08.18.
 */

public class Network {

    public Response resp;

    public Response network(String base, String endpoint, String params[]) {

        Map<String, String> routes = new HashMap<String, String>();
        routes.put("baseLogic", "http://165.227.172.171:7777");
        routes.put("baseAssets", "http://165.227.172.171:8000");
        routes.put("baseChannels", "http://165.227.172.171:9000");

        routes.put("user", "/user");
        routes.put("registration", "/registration");
        routes.put("searchFriend", "/searchFriend");
        routes.put("sendFriendRequest", "/sendFriendRequest");
        routes.put("getFriendRequests", "/friendRequests");
        routes.put("removeInFRL", "/removeInFRL");
        routes.put("deleteFriend", "/deleteFriend");
        routes.put("sync", "/sync");
        routes.put("updateProfile", "/updateProf");
        routes.put("backchannel", "/backchannel");

        routes.put("private", "/private");
        routes.put("public", "/public");

        routes.put("loadChannels", "/loadChannelsForBrowsing");

        //GET or POST

        for (String key : routes.keySet()) {
            if (key.matches(base)) {
                base = routes.get(key);
            } else if (key.matches(endpoint)) {
                endpoint = routes.get(key);
            }
        }
        String arg0 = params[0];
        String arg1 = params[1];
        String arg2 = params[2];
        String imgpath = params[3];

        String url = base + endpoint + arg0 + arg1 + arg2;

        Log.v("URL", url);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        if(endpoint.matches("/backchannel")){


            String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

            url = base + endpoint + arg0 + arg1;
            MediaType IMG = MediaType.parse("image/jpeg");
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "wallpaper.jpg", RequestBody.create(IMG, new File(imgpath)))
                    .addFormDataPart("Message", arg2)
                    .addFormDataPart("From", arg0)
                    .addFormDataPart("Time", String.valueOf(currentDateTimeString))
                    .build();

            request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

        } else {

            request = new Request.Builder()
                    .url(url)
                    .build();

        }


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {

            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                resp = response;
                //response.close();
            }

        });

        //String resp = client.newCall(request).execute().body().toString();
        //client.newCall(request).enqueue(this);

        return resp;
    }

}