package com.axcdevelopment.azurlanesecretary;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    private ArrayList<Shipfu> shipfus;
    private Shipfu shipfu;
    private int version;
    private Context context;
    private Switch onOffSwitch;
    private Spinner selectorSpinner;
    private Spinner versionSelectorSpinner;
    private SeekBar sizeSeekBar;
    private EditText sizeEditText;
    private ImageView overlayPowerBtn;
    private WindowManager windowManager;
    private WindowManager.LayoutParams params;
    private MediaPlayer mediaPlayer;
    private static int size;
    private static String directory;
    private static File dir;
    private static final String ACCESS_TOKEN = "Lg5HLxplGRAAAAAAAAAADxjt40fswX-WsdoBJvO0BKYiR2uUu4SG0UlcnYKMlELt";
    private static DbxRequestConfig config;
    private static DbxClientV2 client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        directory = getFilesDir() + "/";
        dir = getFilesDir();
        config = DbxRequestConfig.newBuilder("ALS").build();
        client = new DbxClientV2(config, ACCESS_TOKEN);
        shipfus = new ArrayList<>();
        shipfu = new Shipfu();
        context = this;
        selectorSpinner = findViewById(R.id.select);
        versionSelectorSpinner = findViewById(R.id.selectVersion);
        mediaPlayer = new MediaPlayer();
        size = 200;

        // sync files with Dropbox
        syncDropbox();

        // set up spinners with synced files
        setUpSpinner();

        // set up size selection
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

        // set up on off switch
        setUpSwitch();
    }

    @Override
    protected void onStart() {
        super.onStart();
        File last = new File(directory + "last.txt");
        if (last.exists()) {
            try {
                Scanner in = new Scanner(last);
                if (selectorSpinner != null && in.hasNext()) {
                    shipfu = shipfus.get(shipfus.indexOf(in.next()));
                    version = in.nextInt();
                    selectorSpinner.setSelection(shipfus.indexOf(in.next()));
                    versionSelectorSpinner.setSelection(version);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        syncDropbox();
    }

    public void syncDropbox() {
        final Thread network = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ListFolderResult folders = client.files().listFolder("");
                    Log.v("USERINFO", "Root Folder: " + folders.toString());
                    for (Metadata folderMeta : folders.getEntries()) {
                        String folder = folderMeta.getName();
                        Log.v("USERINFO", folder);
                        File subDir = new File(directory + folder);
                        if (!subDir.exists()) {
                            Log.v("USERINFO", "Folder created: " + folder);
                            File fil = new File(directory, folder);
                            fil.mkdirs();
                        }
                        ListFolderResult subFolder = client.files().listFolder("/" + folder);
                        for (Metadata fileMeta : subFolder.getEntries()) {
                            String file = fileMeta.getName();
                            if (!(new File(directory + folder + "/" + file).exists())) {
                                OutputStream out = new FileOutputStream(directory + folder + "/" + file);
                                client.files().downloadBuilder("/" + folder + "/" + file)
                                        .download(out);
                                out.close();
                            }
                        }
                        dir = new File(directory + folder);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
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
                                }
                                Collections.sort(shipfus);
                                setUpSpinner();
                            }
                        });
                    }
                } catch (DbxException | FileNotFoundException e) {
                    Log.v("USERINFO", "Error: " + e.getMessage());
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.v("USERINFO", "Error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        network.start();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void startPowerOverlay() {
        // Starts the button overlay.
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        overlayPowerBtn = new ImageView(context);
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
                        version = position;
                        if (mediaPlayer != null) {
                            mediaPlayer.stop();
                        }
                        File last = new File(directory + "last.txt");
                        if (last.exists())
                            last.delete();
                        new File(directory, "last.txt");
                        try {
                            PrintWriter writer = new PrintWriter(last);
                            writer.print(shipfu.getName() + " " + version);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        onOffSwitch.setChecked(false);
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
