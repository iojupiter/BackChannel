package simpleapps.backchannel;


import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.iid.FirebaseInstanceId;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class Register extends AppCompatActivity {

    public String username;
    public String width;
    public  String height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);


        TextView instruction = (TextView) findViewById(R.id.instruction);
        TextView indicationBox = (TextView) findViewById(R.id.indicationBox);
        Button submit = (Button) findViewById(R.id.submit);

        DisplayMetrics dimension = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dimension);
        width = Integer.toString(dimension.widthPixels);
        height = Integer.toString(dimension.heightPixels);

        submit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                EditText usernameInputBox = (EditText) findViewById(R.id.usernameInputBox);
                username = usernameInputBox.getText().toString();
                if(username.matches("")){ //or any special characters too?
                    //do nothing
                }else if(username.length() < 7){
                    Register.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView errorBox = (TextView) findViewById(R.id.errorBox);
                            errorBox.setText("too short");
                        }
                    });
                }
                else{
                    String fbt = FirebaseInstanceId.getInstance().getToken();
                    new Networking().execute("registration", username, fbt, width + "x" + height);
                }
            }

        });
    }


    private class Networking extends AsyncTask<String, Void, String> {

        OkHttpClient client = new OkHttpClient();

        @Override
        protected String doInBackground(String... params) {

            Request request = new Request.Builder()
                    .url("http://165.227.172.171:7777/"+ params[0] +"/"+ params[1] +"/"+ params[2] +"/"+params[3])
                    .build();

            try {
                final Response response = client.newCall(request).execute();
                String r = response.body().string();
                //if response this or that do this or that?


                if (r.equalsIgnoreCase("taken")) {
                    Register.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView errorBox = (TextView) findViewById(R.id.errorBox);
                            errorBox.setText("unavailable");
                            response.close();
                        }
                    });
                }else {
                    SharedPreferences settings = getApplicationContext().getSharedPreferences("USERNAME", 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("username", r);
                    editor.putString("real_username", username);
                    editor.commit();

                    response.close();

                    //CREATE DIRECTORY AND FEED.html here
                    File f = getApplicationContext().getExternalFilesDir(null);
                    File dir = new File(f.getAbsolutePath() + "/BackChannel_Feed/");
                    dir.mkdir();
                    File file = new File(dir, "/feed.html");
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(file.exists()){
                        try{
                            FileOutputStream fOut = new FileOutputStream(file);
                            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                            myOutWriter.append("<!doctype html><html lang='en'><head><meta charset='utf-8'><title></title><style type='text/css'> *{-webkit-user-select: none;} div{display: flex;flex-direction: column;justify-content: center;align-items: center;} p{} img{position: relative;width: 90%;}  \t.message{margin:8px;font-size:15px;font-family: arial;font-weight:normal;padding: 0;width:70%;text-align:center;}  \t.friend{font-size:11px;font-family: monospace;font-weight:bold;normal;padding: 0;margin: 2px;}  \t.time{font-family: monospace;font-size:9px;font-weight:normal;normal;padding: 0;}  </style></head><body><!--NEW--><div><img style='width:100%;' class='image' src='data:image/jpg;base64,iVBORw0KGgoAAAANSUhEUgAAAgAAAAIACAYAAAD0eNT6AAAABGdBTUEAALGPC/xhBQAAACBjSFJNAAB6JgAAgIQAAPoAAACA6AAAdTAAAOpgAAA6mAAAF3CculE8AAAABmJLR0QAAAAAAAD5Q7t/AAAACXBIWXMAAArEAAAKxAFmbYLUAAAU+UlEQVR42u3de4yld13H8c9zzsxeKWvtZSmRipU/jIbE2DVIjMFG4y0EKgKlSlIuRoJEUbBIQY0xXkCsXBI1JCBXUVuVi1X/wJioKAolEijGGzatkXZp17bb3Znd2XPO4x+n08vu7O7szHPm95zze72ayU7Pzp79njOz83s/l/NMAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAGzGbbfd9ry2bV/Wtu0NN91003NKzwPsnKXSAzBzP5bkg0kGpQehf44dO/bo+3fffXfpcZhfh5M8N8ntpQdh8ywKi+1Hk3wgPs+cxcmTJx99fzQalR6H+XUwyW1JDpUehM2zMCyu9cV/WHoQoArrEXB16UHYHAGwmK7PdPF3iAfYSQeT/EVEwFwQAIvn+iQfisUfKGM9AhwO6DkBsFiuz/SEP7v9gZIOJvnz2BPQawJgcbwk08Xflj/QB0+JwwG9JgAWw0titz/QPweT/GVEQC8JgPln8Qf67PLYE9BLAmC+XRe7/YH+8+qAHhIA8+u6TLf8l0sPArAJIqBnBMB8ui7Jh2PxB+aLCOgRATB/XhzH/IH55YqBPSEA5suLY8sfmH9eItgDAmB+WPyBReIlgoUJgPnwolj8gcXjJYIFCYD+e1GSP4jFH1hMTgwsRAD02wtjyx9YfCKgAAHQXy/MdMt/V+lBAHbA+jkBzyo9SC0EQD/9SJKPxOIP1OXyTF8i6EcJ7wAB0D/ri7/d/kCNLo3rBOwIAdAvtvwBvERwRwiA/rD4Azxm/SWCDgfMiADohxfE4g9wuvXLBouAGRAA5Vn8Ac7Ozw6YEQFQ1g9nuvjvLj0IQI+5TsAMCIByfjjJH8biD7AZIqBjAqAMiz/AhRMBHRIAO+/a2O0PsFXrEeDEwG0SADvr2iR/lGRP6UEA5pgTAzsgAHbOtZku/rb8AbbP4YBtEgA74/lxzB+gayJgGwTA7D0/dvsDzIrLBm+RAJit58XiDzBrLhu8BQJgdp6X5I9j8QfYCU4MvEACYDZs+QPsPOcEXAAB0L31Lf+9pQcBqNB6BHx76UH6TgB071diyx+gpINJ3l96iL4TAN0blx4AgDyp9AB9JwC615YeAAAbY+cjAACgQgIAACokAACgQgIAACokAACgQgIAACokAACgQgIAACokAACgQgIAACokAACgQgIAACokAACgQgIAACokAACgQgIAACokAACgQgIAACokAACgQgIAACokAACgQgIAACokAACgQgIAACokAACgQgIAACokAACgQgIAACokAACgQgIAACokAACgQgIAACokAACgQgIAACokAACgQgIAACokAACgQgIAACokAACgQgIAACokAACgQgIAACokAACgQgIAACokAACgQkulB2BnvfTSS/Psiy7KuG1Lj8Ijhkn+enU1Hz18uPQoO27vxcv5zjc/Lc3uSdI2pceZS80wuftvj+bfbjlSehTmjACozPPvvz8vvP/+0mNwmsFll+WjpYcoYNeBQS55/T1ZzWos/1u3b3+SW0pPwbwRAJVZbZqk8a22VyaTnKj0c9JOkqwuZ2nviTSOSG5Zu9ommZQegzkjAOiXXbuSq66a/rp+mKJpNn7/bL93rtvWF9rT72Oj2872dz7e2f7OJBmNkjvvTFZXSz+rAGcQAPTHc56T3Hxz8k3flAyH09tOX7R3ckt5o7/79EX+XCaT5I47kle9Kvn853du7soNs5RhlkuP0am1iEi6JwDoh4svTt7znunW/yI5dCh597uTa65JVlZKT7Pwfig/le/JK7I7+5IsyomuTR7Okfxpfj2357bSw7BABAD9cOWVydOfXnqK2Th0KPmWb0k++9nSkyy0a3JDXpG3lx5jJp6Sb8xr86HcmEO5N18uPQ4Lwlk39MO5jrMvgv37S0+w8K7Jy0qPMFN7c1Gelm8uPQYLRADATljkuOmJUdZKjzBzJ3K89AgsEAFAP3h5Itv0N3lf6RFmzsmAdEkA0A9HjyanTpWeYnb27i09wcL7x9yaf83flR5jphqXS6JDAoB+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' /><p class='message' style='text-align:center;'>Welcome to BackChannel</p></div></body></html>");
                            //myOutWriter.append("<!doctype html><html lang='en'><head><meta charset='utf-8'><title></title></head><body><iframe src='165.227.172.171:8000/content/source.html'></iframe></body></html>");
                            myOutWriter.close();
                            fOut.close();
                        } catch(Exception e){

                        }
                    }else{
                        try{
                            file.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    finish();
                }
                response.close();
                return response.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(String result) {

        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    @Override
    public void onBackPressed() {
    }

}
