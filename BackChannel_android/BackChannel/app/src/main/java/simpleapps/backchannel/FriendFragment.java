package simpleapps.backchannel;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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

public class FriendFragment extends Fragment {

    String netType;

    //GET USERNAMES
    String username;
    String real_username;

    public HashMap<String, String> friendMap;
    List<String> nfl = new ArrayList<String>();

    Button inviteButton;
    EditText search;
    Button searchButton;
    ListView listview;

    private TextView[] generatedViews;


    private View.OnClickListener searchFriendListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            EditText usernameInputBox = (EditText) getView().findViewById(R.id.search);
            String friendUsername = usernameInputBox.getText().toString();
            if (friendUsername.matches("") || friendUsername.length() < 7) { //or any special characters too?
                Toast.makeText(getContext(), "Search cannot be empty or less than 7 characters", Toast.LENGTH_LONG).show();
            } else {
                new Networking().execute("searchFriend", username, friendUsername);
            }
        }
    };

    private View.OnClickListener inviteListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            String shareBody = "Would you care to join me on BackChannel? \n https://backchatapp.com";
            sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(sharingIntent, "Share using"));

        }
    };

    public static FriendFragment newInstance() {
        return new FriendFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_friend, container, false);


        //GET USERNAMES
        SharedPreferences settings = getActivity().getSharedPreferences("USERNAME", 0);
        username = settings.getString("username", null);
        real_username = settings.getString("real_username", null);

        //FIRST TIME INFORMATION
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        if(!prefs.contains("FirstTime")){
            AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(getContext());
            myAlertDialog.setTitle("Getting started");
            myAlertDialog.setMessage("Invite a friend or add them by their username. \n\n" +
                    "You can add yourself to get an idea of how BackChannel works. \n\n" +
                    "A friend in green means you've paired. \n\n" +
                    "You can only BackChannel with a friend in green. \n\n" +
                    "A friend in red hasn't added you yet. \n\n" +
                    "Tap on a friend to delete.");
            myAlertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("FirstTime", true);
                    editor.commit();
                }
            });
            myAlertDialog.show();
        }


        //FRIEND SEARCH TOOLS
        search = (EditText) view.findViewById(R.id.search);
        searchButton = (Button) view.findViewById(R.id.searchButton);
        searchButton.setOnClickListener(searchFriendListener);
        listview = (ListView) view.findViewById(R.id.searchResultList);

        //SYNC WITH BACKEND
        new Networking().execute("sync", username, "");

        return view;
    }

    private void buildUI(View view) {

        int numberOfFriends = friendMap.size();

        LinearLayout linLayout = (LinearLayout) view.findViewById(R.id.dynamicLayout);
        linLayout.setOrientation(LinearLayout.VERTICAL);

        linLayout.removeAllViews();

        TextView friendCount = (TextView) view.findViewById(R.id.friendCounter);

        if (numberOfFriends == 0) {
            TextView textView = new TextView(getContext());
            textView.setText("You have no friends");
            textView.setGravity(Gravity.CENTER);
            linLayout.addView(textView);

            //INVITE BUTTON
            Button inviteButton = new Button(getContext());
            inviteButton.setText("Invite a friend");
            inviteButton.setGravity(Gravity.CENTER);
            inviteButton.setTextColor(Color.WHITE);
            inviteButton.setBackgroundResource(R.color.colorPrimary);
            ConstraintLayout.LayoutParams p = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
            p.setMargins(0, 50, 0, 0);
            inviteButton.setLayoutParams(p);
            inviteButton.setOnClickListener(inviteListener);
            linLayout.addView(inviteButton);


            friendCount.setVisibility(View.INVISIBLE);
        } else {
            friendCount.setVisibility(View.VISIBLE);
            friendCount.setGravity(Gravity.CENTER);
            friendCount.setText(numberOfFriends + "/5 friends");

            generatedViews = new TextView[numberOfFriends];
            int count = 0;
            for (String k : friendMap.keySet()) {


                TextView textView = new TextView(getContext());
                textView.setVisibility(View.VISIBLE);
                textView.setText(k);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25f);
                textView.setGravity(Gravity.CENTER);


                String mutual = friendMap.get(k);

                if (mutual.equals("no")) {
                    textView.setBackgroundResource(R.color.bg_screen2);
                } else {
                    textView.setBackgroundResource(R.color.colorAccent);
                }
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(50, 20, 50, 20);
                linLayout.addView(textView, layoutParams);
                generatedViews[count] = textView;
                count++;
            }

        }
        if (numberOfFriends == 5) {
            search.setVisibility(View.INVISIBLE);
            searchButton.setVisibility(View.INVISIBLE);
        }else {
            search.setVisibility(View.VISIBLE);
            searchButton.setVisibility(View.VISIBLE);
        }

        assignListeners(view);

    }

    private void assignListeners(final View view) {
        if(generatedViews == null){
            //do nothing
        }else {
            for (int i = 0; i < generatedViews.length; i++) {
                for (TextView tv : generatedViews) {
                    final String friend = (String) tv.getText();
                    tv.setOnClickListener(new View.OnClickListener() {


                        public void onClick(final View v) {
                            AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(v.getContext());
                            myAlertDialog.setTitle("Delete " + friend + "?");
                            myAlertDialog.setPositiveButton(
                                    "Yes",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            //remove from friendMap where username like friend
                                            new Networking().execute("deleteFriend", username, friend);
                                            friendMap.remove(friend);
                                        }
                                    });
                            myAlertDialog.setNegativeButton(
                                    "No",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });
                            myAlertDialog.show();
                        }
                    });
                }
            }
        }
    }

    private void populateSearchList(){
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ArrayAdapter<String> listViewAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, nfl);
                listview.setAdapter(listViewAdapter);
            }
        });

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                Object friend = listview.getAdapter().getItem(position);
                String f = friend.toString();

                new Networking().execute("sendFriendRequest", username, f);

            }
        });
    }

    public void updateProfile(){
        JSONObject jsonObject = new JSONObject(friendMap);
        String jsonString = jsonObject.toString();
        new Networking().execute("updateProfile", username, jsonString);

    }

    public void warnNoNetwork(){
        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(getContext());
        myAlertDialog.setTitle("No internet connection");
        myAlertDialog.setMessage("BackChannel needs an internet connection to work.");
        myAlertDialog.setNegativeButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        myAlertDialog.show();
    }

    public void setBackChatables(){
        //SET SHAREDPREF FOR ADDMESSAGE

        SharedPreferences targets = getContext().getSharedPreferences("TARGETS", 0);
        SharedPreferences.Editor editor = targets.edit();
        editor.clear();
        Set<String> targetSet = new HashSet<String>();
        for (String k : friendMap.keySet()) {
            String mutual = friendMap.get(k);
            if(friendMap.keySet().isEmpty()){
                editor.clear();
            } else if (mutual.equals("yes")) {
                targetSet.add(k);
            }else if (mutual.equals("no")){
                targetSet.remove(k);
            }
        }
        editor.putStringSet("targets", targetSet);
        editor.commit();
    }


    private class Networking extends AsyncTask<String, Void, String> {

        OkHttpClient client = new OkHttpClient();

        @Override
        protected String doInBackground(String... params) {

            Request request = new Request.Builder()
                    .url("http://165.227.172.171:7777/" + params[0] + "/" + params[1] + "/" + params[2])
                    .build();

            String r = null;
            try {
                Response response = client.newCall(request).execute();
                r = response.body().string();
                //if response this or that do this or that?
                switch (params[0]) {
                    case "sync":
                        netType = "sync";
                        //friendMap.clear();
                        friendMap = new HashMap(); //redefine hashmap on every sync? where could it corrupt?
                        JSONObject jsonObject = new JSONObject(r);
                        Iterator<String> keysItr = jsonObject.keys();
                        while (keysItr.hasNext()) {
                            String key = keysItr.next();
                            String value = (String) jsonObject.getString(key);
                            friendMap.put(key, value);
                        }
                        break;
                    case "searchFriend":
                        netType = "searchFriend";
                        if (r.equals("null") || r.equals("[]")) {
                            break;
                        } else {
                            String trimmedList = r.substring(1, r.length() - 1);
                            String[] fl = trimmedList.split(",");
                            nfl = new ArrayList<String>();
                            for (String element : fl) {
                                String ee = element.replaceAll("^\"|\"$", "");
                                nfl.add(ee);
                            }
                        }
                        break;
                    case "sendFriendRequest":
                        netType = "sendFriendRequest";
                        if (r.equals(real_username)) {
                            friendMap.put((String) r, "yes"); //very risky.. network may fail...TEST
                        } else {
                            friendMap.put((String) r, "no"); //very risky.. network may fail...TEST
                        }
                        break;
                    case "updateProfile":
                        netType = "updateProfile";
                        break;
                    case "deleteFriend":
                        netType = "deleteFriend";
                        break;
                }
                response.close();
                //return response.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return r;
        }

        protected void onPostExecute(String r) {
            switch (netType){
                case "sync":
                    buildUI(getView());
                    setBackChatables();
                    break;
                case "searchFriend":
                    if (r.equals("null") || r.equals("[]")) {
                        Toast.makeText(getActivity(), "No user found", Toast.LENGTH_LONG).show();
                    }
                    populateSearchList();
                    break;
                case "sendFriendRequest":
                    search.setText("");
                    listview.setAdapter(null);
                    Toast.makeText(getContext(), "Friend request sent", Toast.LENGTH_LONG).show();
                    updateProfile();
                    buildUI(getView());
                    break;
                case "updateProfile":
                    setBackChatables();
                    break;
                case "deleteFriend":
                    search.setText("");
                    listview.setAdapter(null);
                    Toast.makeText(getContext(), "Friend deleted", Toast.LENGTH_LONG).show();
                    updateProfile();
                    setBackChatables();
                    buildUI(getView());
                    break;
            }
        }
    }


}