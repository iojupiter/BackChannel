package simpleapps.backchannel;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by jupiterio on 18.09.18.
 */

public class MasterFragment extends Fragment {

    public static MasterFragment newInstance() {
        return new MasterFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_master, container, false);


        File f = getActivity().getExternalFilesDir(null);
        File dir = new File(f.getAbsolutePath() + "/BackChannel_Feed/");
        File file = new File(dir, "/feed.html");

        String feed = LoadData(file.getAbsolutePath());
        WebView myWebView = (WebView) view.findViewById(R.id.webView);
        myWebView.reload();
        myWebView.getSettings().setJavaScriptEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //FOR DEBUGGING ONLY
            myWebView.setWebContentsDebuggingEnabled(true);
        }

        myWebView.loadData(feed, "text/html; charset=utf-8", "UTF-8");

        return view;
    }

    public String LoadData(String inFile) {

        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(inFile));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException e) {
            //You'll need to add proper error handling here
        }


        return text.toString();
    }


}