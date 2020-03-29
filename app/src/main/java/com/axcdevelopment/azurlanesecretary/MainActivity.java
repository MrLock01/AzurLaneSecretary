package com.axcdevelopment.azurlanesecretary;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity {

    private static ArrayList<Shipfu> shipfus;
    private static Shipfu shipfu;
    private static int version;
    private static Context context;
    private static Switch onOffSwitch;
    private static Spinner selectorSpinner;
    private static Spinner versionSelectorSpinner;
    private static TextView textView;
    private static SeekBar sizeSeekBar;
    private static EditText sizeEditText;
    private static ImageView overlayPowerBtn;
    private static WindowManager windowManager;
    private static WindowManager.LayoutParams params;
    private static MediaPlayer mediaPlayer;
    private static int size;
    private static final String URL = "https://github.com/alandaboi/Ships/archive/master.zip";

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        shipfus = new ArrayList<>();
        shipfu = new Shipfu();
        context = this;
        mediaPlayer = new MediaPlayer();
        size = 200;

        setUpList();

        setUpSpinner();

        setUpSeekBar();

        setUpEditText();

        // Check for overlay permission. If not enabled, request for it. If enabled, show the overlay
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(context)) {
            CharSequence text = "Please grant the access to the application.";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.fromParts("package", getPackageName(), null)));
        }

        setUpSwitch();

        writeToList();

    }

    public void setUpList() {
        File dir = new File(getFilesDir() + "/Ships-master/");
        Log.v("USERINFO", "directory: " + dir);
        Log.v("USERINFO", "zip directory exists: " + dir.exists());
        if (dir.exists()) {
            Log.v("USERINFO", dir.listFiles().toString());
            for (File file : dir.listFiles()) {
                String filename = file.getName();
                String fileExtension = filename.substring(filename.length() - 4);
                if (fileExtension.equalsIgnoreCase(".png")) {
                    String[] split = filename.split("-");
                    Log.v("USERINFO", "registering shipfu: " + split[0]);
                    boolean newShip = true;
                    for (Shipfu s : shipfus) {
                        if (s.getName().equalsIgnoreCase(split[0])) {
                            newShip = false;
                            break;
                        }
                    }
                    if (newShip) {
                        shipfu = new Shipfu();
                        shipfu.setName(split[0]);
                        shipfus.add(shipfu);
                    }
                    shipfus.get(shipfus.indexOf(shipfu)).getSkins().add(split[1]);
                    shipfus.get(shipfus.indexOf(shipfu)).getChibi().add(Uri.parse(file.getPath()));
                } else if (fileExtension.equalsIgnoreCase(".ogg")) {
                    String name = filename.substring(0, filename.length() - 5);
                    boolean newShip = true;
                    for (Shipfu s : shipfus) {
                        if (s.getName().equalsIgnoreCase(name)) {
                            newShip = false;
                            break;
                        }
                    }
                    if (newShip) {
                        shipfu = new Shipfu();
                        shipfu.setName(name);
                        shipfus.add(shipfu);
                    }
                    shipfus.get(shipfus.indexOf(shipfu)).getVoiceLines().add(Uri.parse(file.getPath()));
                }
            }
            setUpSpinner();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void writeToList() {
        downloadZip();
        /*
        Thread network = new Thread(new Runnable() {
            @Override
            public void run() {
                downloadZip();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        unpackZip(getFilesDir().getPath() + "/", "master.zip");
                        setUpList();
                    }
                });
            }
        });
        network.start();
         */

    }

    long downloadID;
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void downloadZip() {
        File file = new File(getFilesDir().getAbsolutePath() + "/");
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(URL))
                .setTitle("Azur lane Secretary")
                .setDescription("Checking for new shipfus")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(file))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true);
        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        downloadID = downloadManager.enqueue(request);
        BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Fetching the download id received with the broadcast
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                //Checking if the received broadcast is for our enqueued download by matching download id
                if (downloadID == id) {
                    Log.v("USERINFO", "Data refreshed");
                    unpackZip(getFilesDir().getPath() + "/", "master.zip");
                    setUpList();
                }
            }
        };
        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        /*
        try (BufferedInputStream inputStream = new BufferedInputStream(new URL(URL).openStream());
             FileOutputStream fileOS = new FileOutputStream(new File(getFilesDir().getAbsolutePath() + "/", "master.zip"))) {
            Log.v("USERINFO", "attempting fetch");
            byte data[] = new byte[5120];
            int byteContent;
            while ((byteContent = inputStream.read(data, 0, 5120)) != -1) {
                fileOS.write(data, 0, byteContent);
                Log.v("USERINFO", "fetching, please wait");
            }
            Log.v("USERINFO", "zip downloaded");
        } catch (IOException e) {
            Log.v("USERINFO", "failed to fetch " + e.toString());
        }
         */

    }

    private boolean unpackZip(String path, String zipname) {
        InputStream is;
        ZipInputStream zis;
        try {
            String filename;
            is = new FileInputStream(path + zipname);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;
            while ((ze = zis.getNextEntry()) != null) {
                filename = ze.getName();
                Log.v("USERINFO", "extracting: " + path + filename);
                // Need to create directories if not exists, or
                // it will generate an Exception...
                if (ze.isDirectory()) {
                    File fmd = new File(path + filename);
                    fmd.mkdirs();
                    continue;
                }
                if (new File(path + filename).exists()) {
                    Log.v("USERINFO", "file " + new File(path + filename).toString() + " already exists");
                    continue;
                }
                FileOutputStream fout = new FileOutputStream(path + filename);
                while ((count = zis.read(buffer)) != -1) {
                    fout.write(buffer, 0, count);
                }
                Log.v("USERINFO", "File extracted to: " + path);
                fout.close();
                zis.closeEntry();
            }
            zis.close();
            Log.v("USERINFO", "zip extration successful");
        }
        catch(IOException e) {
            e.printStackTrace();
            Log.v("USERINFO", "extraction failed:" + e.toString());
            return false;
        }
        return true;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void startPowerOverlay() {
        // Starts the button overlay.
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        overlayPowerBtn = new ImageView(context);
        //overlayPowerBtn.setImageResource(R.drawable.prinz_eugen_chibi);
        overlayPowerBtn.setImageURI(shipfu.getChibi().get(version));
        Log.v("USERINPUT", "Image Set");
        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // APPLICATION_OVERLAY FOR ANDROID 26+ AS THE PREVIOUS VERSION RAISES ERRORS
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            // FOR PREVIOUS VERSIONS USE TYPE_PHONE AS THE NEW VERSION IS NOT SUPPORTED
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.NO_GRAVITY;
        params.x = 0;
        params.y = 0;
        params.height = size;
        params.width = size;
        overlayPowerBtn.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Save current x/y
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        if (!mediaPlayer.isPlaying() && shipfu.getVoiceLines().size() > 0) {
                            mediaPlayer = MediaPlayer.create(context,
                                    shipfu.getVoiceLines().get(new Random().nextInt(shipfu.getVoiceLines().size())));
                            mediaPlayer.start();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(overlayPowerBtn, params);
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(overlayPowerBtn, params);
    }

    private void setUpSpinner() {
        selectorSpinner = findViewById(R.id.select);
        versionSelectorSpinner = findViewById(R.id.selectVersion);
        textView = findViewById(R.id.versionText);
        selectorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // On selecting a spinner item
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                }
                shipfu = shipfus.get(position);
                versionSelectorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        String skin = shipfu.getSkins().get(position);
                        version = position;
                        if (overlayPowerBtn != null) {
                            windowManager.removeView(overlayPowerBtn);
                            startPowerOverlay();
                        }
                        // Showing selected spinner item
                        Toast.makeText(parent.getContext(), skin + " selected", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
                ArrayAdapter<String> dataAdapter =
                        new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, shipfu.getSkins());
                dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                versionSelectorSpinner.setAdapter(dataAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        ArrayAdapter<Shipfu> dataAdapter =
                new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, shipfus);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selectorSpinner.setAdapter(dataAdapter);

    }

    private void setUpSeekBar() {
        sizeSeekBar = findViewById(R.id.size);
        sizeSeekBar.setMax(500);
        sizeSeekBar.setPadding(20, 0, 20, 0);
        sizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                this.progress = progress;
                size = progress;
                if (overlayPowerBtn != null) {
                    params.height = progress;
                    params.width = progress;
                    windowManager.updateViewLayout(overlayPowerBtn, params);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (sizeEditText != null) {
                    sizeEditText.setText("" + progress);
                }
            }
        });
        sizeSeekBar.setProgress(200);
    }

    private void setUpEditText() {
        sizeEditText = findViewById(R.id.editSize);
        sizeEditText.setText("" + sizeSeekBar.getProgress());
        sizeEditText.setFilters(new InputFilter[]{new InputFilterMinMax(0, 500)});
        sizeEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    sizeSeekBar.setProgress(Integer.parseInt(sizeEditText.getText().toString()));
                    if (overlayPowerBtn != null) {
                        params.height = sizeSeekBar.getProgress();
                        params.width = sizeSeekBar.getProgress();
                        windowManager.updateViewLayout(overlayPowerBtn, params);
                    }
                    InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    sizeEditText.clearFocus();
                    return true;
                }
                return false;
            }
        });

    }

    private void setUpSwitch() {
        onOffSwitch = findViewById(R.id.start);
        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!shipfu.getChibi().isEmpty())
                        startPowerOverlay();
                    else {
                        onOffSwitch.setChecked(false);
                        //new Toast(context, "Please select a ship!")
                    }
                }
                else {
                    if (overlayPowerBtn != null)
                        windowManager.removeView(overlayPowerBtn);
                    if (mediaPlayer != null)
                        mediaPlayer.stop();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (overlayPowerBtn != null)
            windowManager.removeView(overlayPowerBtn);
    }

    private class InputFilterMinMax implements InputFilter {
        private int min, max;

        public InputFilterMinMax(int min, int max) {
            this.min = min;
            this.max = max;
        }

        public InputFilterMinMax(String min, String max) {
            this.min = Integer.parseInt(min);
            this.max = Integer.parseInt(max);
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            try {
                int input = Integer.parseInt(dest.toString() + source.toString());
                if (isInRange(min, max, input))
                    return null;
            } catch (NumberFormatException nfe) { }
            return "";
        }

        private boolean isInRange(int a, int b, int c) {
            return b > a ? c >= a && c <= b : c >= b && c <= a;
        }
    }

}
