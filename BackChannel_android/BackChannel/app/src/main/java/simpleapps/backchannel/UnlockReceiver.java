package simpleapps.backchannel;

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.IOException;

import static android.app.WallpaperManager.FLAG_LOCK;

/**
 * Created by jupiterio on 13.09.18.
 */

public class UnlockReceiver extends BroadcastReceiver{
    UnlockReceiver screen;
    Context context=null;

    @Override
    public void onReceive(final Context context, Intent intent)
    {
        this.context = context;
        if (intent.getAction().equals(Intent.ACTION_SCREEN_ON) || intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {

            File f = context.getExternalFilesDir(null);
            File dir = new File(f.getAbsolutePath() + "/Pictures/");
            File imgFile = new File(dir, "/default_wallpaper.jpg");

            if (imgFile.exists()) {
                Bitmap bm = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

                WallpaperManager wpm = WallpaperManager.getInstance(context);
                try {
                    wpm.setBitmap(bm, null, true, FLAG_LOCK);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}