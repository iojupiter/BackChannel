package simpleapps.backchannel;

import android.app.WallpaperManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static android.app.WallpaperManager.FLAG_LOCK;


/**
 * Created by jupiterio on 18.09.18.
 */

public class SettingsFragment extends Fragment {

    protected static final int DEFAULT_IMG = 2;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        Button s_d_l_b = (Button) view.findViewById(R.id.default_lsi_button);

        s_d_l_b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setDefaultImg();
            }
        });


        return view;
    }

    public void setDefaultImg() {
        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(getContext());
        myAlertDialog.setTitle("Choose a default lock screen image");
        myAlertDialog.setMessage("Set a default image that will be set once you've unlocked your phone.");

        myAlertDialog.setPositiveButton("Choose from gallery", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, DEFAULT_IMG);
                } else {

                    Intent pictureActionIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    getActivity().startActivityForResult(pictureActionIntent, DEFAULT_IMG);

                }

            }
        });

        myAlertDialog.setNegativeButton("Use current image", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {

                WallpaperManager wpm = WallpaperManager.getInstance(getActivity());
                ParcelFileDescriptor current_wallpaper_id = wpm.getWallpaperFile(FLAG_LOCK);

                InputStream fileStream = new FileInputStream(current_wallpaper_id.getFileDescriptor());
                String default_wallpaper_file = null;
                try {
                    String imageFileName = "default_image";
                    File storageDir;
                    if (isExternalStorageWritable()) {
                        storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    } else {
                        storageDir = getActivity().getFilesDir();
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


                Toast.makeText(getContext(), "Default image set", Toast.LENGTH_SHORT).show();
            }
        });
        myAlertDialog.setNeutralButton("Don't set a default", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                File storageDir;
                if (isExternalStorageWritable()) {
                    storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                } else {
                    storageDir = getActivity().getFilesDir();
                }
                File image = new File(storageDir, "default_wallpaper.jpg");
                if(image.exists())
                    image.delete();
                Toast.makeText(getContext(), "Default image not set", Toast.LENGTH_SHORT).show();
            }
        });
        myAlertDialog.show();
    }


    //TESTING SD-CARD AVAILABLE
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
}