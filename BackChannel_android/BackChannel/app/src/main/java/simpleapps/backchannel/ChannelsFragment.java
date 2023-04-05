package simpleapps.backchannel;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


/**
 * Created by jupiterio on 18.09.18.
 */

public class ChannelsFragment extends Fragment {

    JSONObject json_logo_ds = new JSONObject();
    HashMap<String, ImageButton> generatedImageButtons = new HashMap<>();
    Set<String> user_channels_set;
    Set<String> temp_set = new HashSet<>();

    public static ChannelsFragment newInstance() {
        return new ChannelsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_channels, container, false);

        //on arrival: network request /channels <-- partner logos()
        new Networking().execute();

        return view;
    }

    private void buildUI(){

        TableLayout tableLayout = (TableLayout) getView().findViewById(R.id.tablelayout);

        Iterator<String> iter = json_logo_ds.keys();
        List<String> entity_name_list = new ArrayList<String>();

        while (iter.hasNext()) {
            String entity_name = iter.next();
            entity_name_list.add(entity_name);
        }

        int total_rows = (int) Math.ceil(entity_name_list.size()/2.0);
        int channel_counter = 0;

        for (int row_count = 0; row_count < total_rows; row_count++){
            TableRow tableRow = new TableRow(getContext());

            for(int column_count = 0; column_count < 3; column_count++){

                if(entity_name_list.size() == channel_counter){
                    break;
                }else {
                    String entity_name = entity_name_list.get(channel_counter);

                    String entity_base64 = null;
                    try {
                        entity_base64 = (String) json_logo_ds.get(entity_name);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    byte[] decodedString = Base64.decode(entity_base64.getBytes(), Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                    ImageButton imageButton = new ImageButton(getContext());
                    imageButton.setVisibility(View.VISIBLE);
                    imageButton.setImageBitmap(bitmap);
                    imageButton.setBackgroundColor(Color.TRANSPARENT);

                    TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);
                    params.setMargins(50, 50, 50, 50);

                    generatedImageButtons.put(entity_name, imageButton);

                    tableRow.addView(imageButton, params);

                    channel_counter++;
                }
            }

            tableLayout.addView(tableRow);
        }

        //Restore channels state if any
        SharedPreferences user_channels = getActivity().getSharedPreferences("USER_CHANNELS", 0);
        user_channels_set = user_channels.getStringSet("user_channels", null);
        if(user_channels_set != null) {
            temp_set = user_channels_set;
            for(String ch : user_channels_set){
                ImageButton imageButton = generatedImageButtons.get(ch);
                imageButton.setBackgroundResource(R.color.channel_background_subscribe);
            }
        }else{
            user_channels_set = temp_set;
        }


        assignListeners();

    }

    private void assignListeners() {

        for (final String channel_name : generatedImageButtons.keySet()) {

            final ImageButton imageButton = generatedImageButtons.get(channel_name);

            imageButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(final View v) {

                        if(temp_set.contains(channel_name)){
                            setChannels("remove", channel_name, imageButton);
                        }else{
                            setChannels("add", channel_name, imageButton);
                        }


                    }
                });
        }
    }

    private void setChannels(String operation, String channel_name, ImageButton imageButton){
        SharedPreferences user_channels = getActivity().getSharedPreferences("USER_CHANNELS", 0);
        SharedPreferences.Editor editor = user_channels.edit();
        editor.clear();
        //Set<String> user_channels_set = user_channels.getStringSet("user_channels", null);

        int number_of_channels = (temp_set.isEmpty()) ? 0 : temp_set.size();

        if(number_of_channels >= 3 && operation.equals("add")){
            Toast.makeText(getContext(), "You reached limit of 3 channels", Toast.LENGTH_SHORT).show();
        }else {
            if (operation.equals("add")) {
                FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance();
                firebaseMessaging.subscribeToTopic(channel_name);
                imageButton.setBackgroundResource(R.color.channel_background_subscribe);

                temp_set.add(channel_name);

                editor.putStringSet("user_channels", temp_set);
                editor.commit();
                Toast.makeText(getContext(), "Subscribed to " + channel_name, Toast.LENGTH_SHORT).show();
            } else {
                FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance();
                firebaseMessaging.unsubscribeFromTopic(channel_name);
                imageButton.setBackgroundResource(R.color.channel_background_unsubscribe);
                temp_set.remove(channel_name);
                editor.putStringSet("user_channels", temp_set);
                editor.commit();
                Toast.makeText(getContext(), "Unsubscribed from " + channel_name, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class Networking extends AsyncTask<String, Void, String> {

        OkHttpClient client = new OkHttpClient();

        @Override
        protected String doInBackground(String... params) {

            Request request = new Request.Builder()
                    .url("http://165.227.172.171:9000/loadChannelsForBrowsing")
                    .build();

            try {
                Response response = client.newCall(request).execute();


                String r = response.body().string();
                json_logo_ds = new JSONObject(r);


                response.close();
                return response.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(String r) {

            buildUI();

        }
    }

}

