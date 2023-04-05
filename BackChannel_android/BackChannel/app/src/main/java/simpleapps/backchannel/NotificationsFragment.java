package simpleapps.backchannel;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;


/**
 * Created by jupiterio on 18.09.18.
 */

public class NotificationsFragment extends Fragment {

    //GET USERNAMES
    String username;
    String real_username;

    ListView listview;

    public static NotificationsFragment newInstance() {
        return new NotificationsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_friend_requests, container, false);

        //GET USERNAMES
        SharedPreferences settings = getActivity().getSharedPreferences("USERNAME", 0);
        username = settings.getString("username", null);
        real_username = settings.getString("real_username", null);

        //String friend_request_list = getFriendRequests();

        //buildUI(view, friend_request_list);

        return view;
    }
/*
    public String  getFriendRequests(){
        String[] params;
        params = new String[]{"/" + username, "", "", ""};
        Network n = new Network();
        try {
            n.network("baseLogic", "getFriendRequests", params); //callback may not have finished problem
            sleep(1000); //add loading widget on wait -------------------------------------
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String resp = null;
        try {
            resp = n.resp.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            warnNoNetwork();
        }

        return resp;
    }

    public void buildUI(View view, final String frl){

        final LinearLayout linLayout = (LinearLayout) view.findViewById(R.id.generalLayout);
        linLayout.setOrientation(LinearLayout.VERTICAL);

        if(frl.equals("0")){
            TextView textView = new TextView(getContext());
            textView.setText("No new notifications");
            textView.setGravity(Gravity.CENTER);
            linLayout.addView(textView);
        }else{
            listview = new ListView(getContext());
            String trimmedList = frl.substring(1, frl.length() - 1);
            String[] fl = trimmedList.split(",");
            final List<String> nfl = new ArrayList<String>();
            for (String element : fl) {
                String ee = element.replaceAll("^\"|\"$", "");
                nfl.add(ee);
            }

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
                    final Object friend = listview.getAdapter().getItem(position);
                    AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(getContext());
                    myAlertDialog.setTitle("Accept " + friend + "?");
                    myAlertDialog.setPositiveButton(
                            "Yes",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    //check that user doesn't have +5 friends else toast
                                    //add friend to friendMap ???
                                    FriendFragment ff = new FriendFragment();
                                    HashMap<String, String> friendMap = ff.sync(username);
                                    int numberOfFriends = friendMap.size();
                                    if (numberOfFriends == 5) {
                                        Toast.makeText(getContext(), "You must remove a friend before you can accept "+friend, Toast.LENGTH_LONG).show();
                                    }else {
                                        friendMap.put((String) friend, "yes");
                                        updateProfile(friendMap);
                                        removeFromFriendRequestList(friend);
                                        nfl.remove(friend);
                                        listview.invalidateViews();
                                        dialog.cancel();
                                    }
                                }
                            });
                    myAlertDialog.setNegativeButton(
                            "No",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    //network remove friend from username friend_request_list
                                    //remove from list
                                    removeFromFriendRequestList(friend);
                                    nfl.remove(friend);
                                    listview.invalidateViews();
                                    dialog.cancel();
                                }
                            });
                    myAlertDialog.show();
                }
            });

            linLayout.addView(listview);
        }

    }

    public void removeFromFriendRequestList(Object friend){
        String[] params;
        params = new String[]{"/" + username, "/" + friend, "", ""};
        Network n = new Network();
        try {
            n.network("baseLogic", "removeInFRL", params); //callback may not have finished problem
            sleep(1000); //add loading widget on wait -------------------------------------
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String resp = null;
        try {
            resp = n.resp.body().string();
            Toast.makeText(getContext(), resp, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            warnNoNetwork();
        }
    }

    private void updateProfile(HashMap<String, String> friendMap){
        JSONObject jsonObject = new JSONObject(friendMap);
        String jsonString = jsonObject.toString();

        String[] params;
        params = new String[]{"/" + username, "/" + jsonString, "", ""};
        Network n = new Network();

        try {
            n.network("baseLogic", "updateProfile", params); //callback may not have finished problem
            sleep(1000);
            //NO RESPONSE HANDLED
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
*/
}