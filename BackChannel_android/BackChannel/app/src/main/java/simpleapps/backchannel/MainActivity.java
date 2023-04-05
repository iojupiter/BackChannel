package simpleapps.backchannel;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity {

    protected static final int REQUEST_IMAGE_CAPTURE = 0;
    protected static final int GALLERY_PICTURE = 1;
    protected static final int DEFAULT_IMG = 2;
    protected static final int OTHER_RESOURCE = 3;
    String mCurrentPhotoPath;
    private DrawerLayout mDrawerLayout;

    //GET USERNAMES
    String username;
    String real_username;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.content_frame, new MasterFragment());
        tx.commit();

        //setContentView(R.layout.activity_main);
        setContentView(R.layout.navigation_drawer_layout);

        //GET USERNAMES
        SharedPreferences settings = getSharedPreferences("USERNAME", 0);
        username = settings.getString("username", null);
        real_username = settings.getString("real_username", null);


        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);

        if (!isNetworkAvailable()) {
            warnNoNetwork();
        } else {
            try {
                networkRequest();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        loadFeed();


        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // set item as selected to persist highlight
                        menuItem.setChecked(false);
                        mDrawerLayout.closeDrawers();

                        Class fragmentClass = null;
                        switch(menuItem.getItemId()) {
                            case R.id.home:
                                fragmentClass = MasterFragment.class;
                                break;
                            /*case R.id.nav_myprofile:
                                comingSoon();
                                fragmentClass = MasterFragment.class;
                                break;*/
                            case R.id.nav_myfriends:
                                fragmentClass = FriendFragment.class;
                                break;
                            case R.id.nav_notifications:
                                fragmentClass = NotificationsFragment.class;
                                break;
                            /*case R.id.nav_myhistory:
                                comingSoon();
                                fragmentClass = MasterFragment.class;
                                break;*/
                            case R.id.nav_bcChannels:
                                fragmentClass = ChannelsFragment.class;
                                break;
                            case R.id.nav_manage:
                                fragmentClass = SettingsFragment.class;
                                break;
                            default:

                        }

                        Fragment fragment = null;
                        try {
                            fragment = (Fragment) fragmentClass.newInstance();
                        } catch (InstantiationException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }

                        // Insert the fragment by replacing any existing fragment
                        FragmentManager fragmentManager = getSupportFragmentManager();
                        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();


                        return true;
                    }
                });

        View headerView = navigationView.getHeaderView(0);
        TextView navUsername = (TextView) headerView.findViewById(R.id.nav_header_textView);
        navUsername.setText(real_username);


        DisplayMetrics dimension = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dimension);
        int width = dimension.widthPixels;
        int height = dimension.heightPixels;
        SharedPreferences screen = getApplicationContext().getSharedPreferences("SCREEN", 0);
        SharedPreferences.Editor editor = screen.edit();
        editor.putInt("width", width);
        editor.putInt("height", height);
        editor.commit();


        FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance();
        firebaseMessaging.subscribeToTopic("wimbledon");
    }

    public void networkRequest() throws IOException {
        SharedPreferences settings = getSharedPreferences("USERNAME", 0);
        String username = settings.getString("username", null);

        String fbt = FirebaseInstanceId.getInstance().getToken();
        new Networking().execute("user", username, fbt);
    }


    public void loadFeed() {

        Fragment fragment = null;
        Class fragmentClass = MasterFragment.class;
        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

    }

    @Override
    public void onResume() {
        super.onResume();
        loadFeed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_icons, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.backchat:
                SharedPreferences settings = getSharedPreferences("TARGETS", 0);
                Set<String> targetSet = settings.getStringSet("targets", null);
                if (targetSet == null) {
                    Toast.makeText(getApplicationContext(), "You need at least one friend to BackChannel", Toast.LENGTH_LONG).show();
                }else if(targetSet.size() == 0){
                    Toast.makeText(getApplicationContext(), "You need at least one friend to BackChannel", Toast.LENGTH_LONG).show();
                }else {
                    startDialog();
                }
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //TESTING SD-CARD AVAILABLE
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void warnNoNetwork() {
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
        loadFeed();
    }

   //CAMERA ICON DIALOG
    private void startDialog() {

        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(this);

        myAlertDialog.setTitle("Choose an image from");

        myAlertDialog.setPositiveButton("Gallery",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {

                        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, GALLERY_PICTURE);
                        } else {

                            Intent pictureActionIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(pictureActionIntent, GALLERY_PICTURE);
                        }
                    }
                });

        myAlertDialog.setNegativeButton("Camera",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                            File photoFile = null;
                            try {
                                photoFile = createImageFile();
                            } catch (IOException ex) {

                            }
                            if (photoFile != null) {
                                Uri photoURI = FileProvider.getUriForFile(MainActivity.this,
                                        "simpleapps.fileprovider",
                                        photoFile);
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                            }
                        }
                    }
                });
/*
        myAlertDialog.setNeutralButton("Other",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {

                        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, GALLERY_PICTURE);
                        } else {

                            Intent pictureActionIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(pictureActionIntent, OTHER_RESOURCE);
                        }
                    }
                });
*/
        myAlertDialog.show();
}


    //CREATE IMAGE FILE FOR NEWLY TAKE IMAGE
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir;
        if(isExternalStorageWritable()){
            storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        }else {
            storageDir = getFilesDir();
        }
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK && requestCode == REQUEST_IMAGE_CAPTURE) {

            Intent intent = new Intent(MainActivity.this, AddMessage.class);
            intent.putExtra("TYPE", "image");
            intent.putExtra("IMG_PATH", mCurrentPhotoPath);
            startActivity(intent);

        } else if (resultCode == RESULT_OK && requestCode == GALLERY_PICTURE) {

            if (data != null) {
                Uri selectedImage = data.getData();
                String[] filePath = {MediaStore.Images.Media.DATA};
                Cursor c = getContentResolver().query(selectedImage, filePath, null, null, null);
                c.moveToFirst();
                int columnIndex = c.getColumnIndex(filePath[0]);
                String selectedImagePath = c.getString(columnIndex);
                c.close();

                if (selectedImagePath != null) {
                    Intent intent = new Intent(MainActivity.this, AddMessage.class);
                    intent.putExtra("TYPE", "image");
                    intent.putExtra("IMG_PATH", selectedImagePath);
                    startActivity(intent);
                }
            }

        } else if (resultCode == RESULT_OK && requestCode == DEFAULT_IMG) {

            if (data != null) {
                Uri selectedImage = data.getData();
                String[] filePath = {MediaStore.Images.Media.DATA};
                Cursor c = getContentResolver().query(selectedImage, filePath, null, null, null);
                c.moveToFirst();
                int columnIndex = c.getColumnIndex(filePath[0]);
                String selectedImagePath = c.getString(columnIndex);
                c.close();

                InputStream fileStream = null;
                try {
                    fileStream = new FileInputStream(selectedImagePath);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                String default_wallpaper_file = null;
                try {
                    String imageFileName = "default_image";
                    File storageDir;
                    if (isExternalStorageWritable()) {
                        storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    } else {
                        storageDir = getFilesDir();
                    }
                    File image = new File(storageDir, "default_wallpaper.jpg");

                    default_wallpaper_file = image.getAbsolutePath();

                    OutputStream newImageFile = new FileOutputStream(default_wallpaper_file);
                    byte[] buffer = new byte[1024];
                    int length;

                    while ((length = fileStream.read(buffer)) > 0) {
                        newImageFile.write(buffer, 0, length);
                    }

                    newImageFile.flush();
                    fileStream.close();
                    newImageFile.close();


                } catch (IOException e) {
                    e.printStackTrace();
                }
                Toast.makeText(getApplicationContext(), "Default image set", Toast.LENGTH_SHORT).show();

            }
        }else {
            Toast.makeText(getApplicationContext(), "Cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        switch (requestCode) {
            case GALLERY_PICTURE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Intent pictureActionIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pictureActionIntent, GALLERY_PICTURE);

                } else {
                    //do something like displaying a message that he didn`t allow the app to access gallery and you wont be able to let him select from gallery
                }
                break;
            case DEFAULT_IMG:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Intent pictureActionIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pictureActionIntent, DEFAULT_IMG);

                } else {
                    //do something like displaying a message that he didn`t allow the app to access gallery and you wont be able to let him select from gallery
                }
                break;
        }
    }

    public void comingSoon() {
        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(this);
        myAlertDialog.setTitle("Coming soon");

        myAlertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
            dialog.cancel();
            }
        });
        myAlertDialog.show();
    }



    private class Networking extends AsyncTask<String, Void, String> {

        OkHttpClient client = new OkHttpClient();

        @Override
        protected String doInBackground(String... params) {

            Request request = new Request.Builder()
                    .url("http://165.227.172.171:7777/"+ params[0] +"/"+ params[1] +"/"+ params[2])
                    .build();

            try {
                Response response = client.newCall(request).execute();
                String r = response.body().string();
                //if response this or that do this or that?
                if (r.equals("0")) {
                    Intent intent = new Intent(MainActivity.this, Register.class);
                    startActivity(intent);
                } else if (r.equals("1")) {
                    loadFeed();
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


}