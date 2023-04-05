package simpleapps.backchannel;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddMessage extends AppCompatActivity {

    Spinner dropdown;
    ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_message);

        //TOOLBAR
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        //TextView profileTitle = (TextView) findViewById(R.id.toolbar_title);
        //profileTitle.setText("Send to");

        //BACK BUTTON
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.lin);
        linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        //IMAGE THUMBNAIL
        ImageView mImageView = (ImageView) findViewById(R.id.mImageView);

        //DROPDOWN SELECT TARGET
        dropdown = (Spinner) findViewById(R.id.target_dropdown);
        SharedPreferences settings = getSharedPreferences("TARGETS", 0);
        Set<String> targetSet = settings.getStringSet("targets", null);
        List<String> targetList = new ArrayList<>(targetSet);
        targetList.add(0, "Select friend");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, targetList);
        dropdown.setAdapter(adapter);
        dropdown.setSelection(0);


        Intent intent = getIntent();
        final String img_path = intent.getStringExtra("IMG_PATH");
        String mediaType = intent.getStringExtra("TYPE");
        if(mediaType.equals("image")) {
            final int THUMBSIZE = 333;
            Bitmap ThumbImage = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(img_path), THUMBSIZE, THUMBSIZE);
            mImageView.setImageBitmap(ThumbImage);
        }

        final Button send = (Button) findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                if(String.valueOf(dropdown.getSelectedItem()).equals("Select friend")){
                    Toast.makeText(getApplicationContext(), "Haven't selected a friend!", Toast.LENGTH_LONG).show();
                    return;
                }

                progress = ProgressDialog.show(AddMessage.this, "", "Sending...", true, false);

                EditText message = (EditText) findViewById(R.id.message);
                SharedPreferences u = getSharedPreferences("USERNAME", 0);
                String username = u.getString("username", null);

                new Networking().execute("backchannel", username, String.valueOf(dropdown.getSelectedItem()), message.getText().toString(), img_path);

            }

        });


    }

    public void warnNoNetwork(){
        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(this);
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

    private class Networking extends AsyncTask<String, Void, String> {

        OkHttpClient client = new OkHttpClient();
        String r = null;


        @Override
        protected String doInBackground(String... params) {

            String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

            MediaType IMG = MediaType.parse("image/jpeg");
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "wallpaper.jpg", RequestBody.create(IMG, new File(params[4])))
                    .addFormDataPart("Message", params[3])
                    .addFormDataPart("From", params[2])
                    .addFormDataPart("Time", String.valueOf(currentDateTimeString))
                    .build();

            Request request = new Request.Builder()
                    .url("http://" +
                            "165.227.172.171:7777/"+ params[0] +"/"+ params[1] +"/"+ params[2])
                    .post(requestBody)
                    .build();

            try {
                Response response = client.newCall(request).execute();
                r = response.body().string();
                //if response this or that do this or that?

                response.close();
                return response.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(String result) {
            progress.cancel();
            Toast.makeText(getApplicationContext(), r, Toast.LENGTH_LONG).show();
            finish();
        }
    }

}
