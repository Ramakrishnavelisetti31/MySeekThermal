
package com.example.seekdeliverain;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Size;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.thermal.seekware.CaptureResult;
import com.thermal.seekware.ColorPalettePicker;
import com.thermal.seekware.SeekCamera;
import com.thermal.seekware.SeekCameraManager;
import com.thermal.seekware.SeekImage;
import com.thermal.seekware.SeekMediaCapture;
import com.thermal.seekware.SeekPreview;
import com.thermal.seekware.SeekUtility;
import com.thermal.seekware.VideoCaptureRequest;

import java.util.Arrays;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final @ColorInt
    int PHOTO_CAPTURE_BUTTON_COLOR = 0xFFFFFFFF;
    private static final @ColorInt
    int VIDEO_CAPTURE_BUTTON_COLOR = 0xFFFF0000;
    private static final int MEDIA_CAPTURE_SCALE_FACTOR = 4;
    private static final float PREVIEWS_PER_SCREEN = 5.5f;
    private static final int SHUTTER_SPEED_IN_MILLIS = 500;
    private boolean photo = true;

    private static final SeekCamera.ColorPalette[] luts = {
            SeekCamera.ColorPalette.SPECTRA,
            SeekCamera.ColorPalette.IRON2,
            SeekCamera.ColorPalette.RECON,
            SeekCamera.ColorPalette.BLACK_RECON,
            SeekCamera.ColorPalette.WHITEHOT,
            SeekCamera.ColorPalette.BLACKHOT,
            SeekCamera.ColorPalette.AMBER,
            SeekCamera.ColorPalette.GREEN,
            SeekCamera.ColorPalette.TYRIAN,
            SeekCamera.ColorPalette.PRISM,
            SeekCamera.ColorPalette.IRON,
    };

    private boolean initialized = false;

    private SeekCamera thermalCamera;
    private SeekCameraManager seekCameraManager;
    private SeekPreview seekPreview;
    private SeekMediaCapture seekMediaCapture;
    private SeekUtility.PermissionHandler permissionHandler;
    private SimpleOverlay simpleOverlay;
    private ImageButton switchButton;
    private ImageButton captureButton;
    private ImageButton stopButton;
    private TextView videoInfo;
    private ColorPalettePicker colorPalettePicker;
    private ImageView thermalOverlay;

    private SeekCamera.StateCallback stateCallback = new SeekCamera.StateCallbackAdapter() {
        @Override
        public synchronized void onOpened(SeekCamera seekCamera) {
            thermalCamera = seekCamera;
            SeekCamera.Characteristics characteristics = seekCamera.getCharacteristics();

//            seekMediaCapture.setSize(characteristics.getHeight() * MEDIA_CAPTURE_SCALE_FACTOR,
//                    characteristics.getWidth() * MEDIA_CAPTURE_SCALE_FACTOR);
//            System.out.println("seekMediaCapture height is" + characteristics.getHeight());
//            System.out.println("seekMediaCapture width is" + characteristics.getWidth());
            seekMediaCapture.setSize(1280,
                    960);
            seekCamera.setImageSmoothing(true);
            seekCamera.getImageSmoothing();
            seekCamera.createSeekCameraCaptureSession(seekPreview, seekMediaCapture);
        }

        @Override
        public synchronized void onStarted(SeekCamera seekCamera) {
            seekPreview.bringToFront();
            thermalCamera = seekCamera;
            SeekCamera.Characteristics characteristics = seekCamera.getCharacteristics();

//            seekMediaCapture.setSize(characteristics.getHeight() * MEDIA_CAPTURE_SCALE_FACTOR,
//                    characteristics.getWidth() * MEDIA_CAPTURE_SCALE_FACTOR);
//            System.out.println("seekMediaCapture height is" + characteristics.getHeight());
//            System.out.println("seekMediaCapture width is" + characteristics.getWidth());
            seekMediaCapture.setSize(960,
                    1280);
            seekCamera.createSeekCameraCaptureSession(seekPreview, seekMediaCapture);
        }

        @Override
        public synchronized void onStopped(final SeekCamera seekCamera) {
            thermalOverlay.bringToFront();
        }

        @Override
        public synchronized void onClosed(SeekCamera seekCamera) {
            thermalCamera = null;
            initialized = false;
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //hide the title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Objects.requireNonNull(getSupportActionBar()).hide();
        super.onCreate(savedInstanceState);

        //show the activity in full screen
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        simpleOverlay = new SimpleOverlay(this);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        captureButton = findViewById(R.id.capture);
        switchButton = findViewById(R.id.switch_camera_video);
        seekPreview = findViewById(R.id.seek_preview);
        stopButton = findViewById(R.id.stop_record);
        colorPalettePicker = findViewById(R.id.color_lut_picker);
        videoInfo = findViewById(R.id.video_info);
        thermalOverlay = findViewById(R.id.thermal_overlay);
        colorPalettePicker.setVisibility(View.INVISIBLE);
        stopButton.setVisibility(View.INVISIBLE);
        captureButton.setOnLongClickListener(v -> {
            onCaptureLongClicked(v);
            return true;
        });
        seekPreview.initialize(true, simpleOverlay);
        seekPreview.setDetectionMode(SeekPreview.DetectionMode.ALL);
        seekPreview.setStateCallback(new SeekPreview.StateCallbackAdapter() {
            @Override
            public void onFrameAvailable(SeekPreview seekPreview, SeekImage seekImage) {
                if(!initialized){
                    initialized = true;
                    Size size = new Size((int) (seekPreview.getWidth() / PREVIEWS_PER_SCREEN), (int) (seekPreview.getWidth() / PREVIEWS_PER_SCREEN * 4f / 3f));
                    System.out.println("seek preview width is" + seekPreview.getWidth());
                    System.out.println("seek preview size is" + size);
                    Typeface typeface = Typeface.createFromAsset(getAssets(), "roboto_regular.ttf");
                    ColorPalettePicker.LinearConfiguration linearConfiguration = new ColorPalettePicker.LinearConfiguration(getApplicationContext(),
                            Arrays.asList(luts), true, size, new ColorPalettePicker.TextAttributes.Builder().setTypeface(typeface).build());
                    colorPalettePicker.initialize(seekImage, linearConfiguration);
                }
                if(seekPreview.getFrameNumber() % 2 == 0 && colorPalettePicker.getVisibility() == View.VISIBLE){
                    colorPalettePicker.update();
                }
                updateVideoInfo();
            }
        });

        seekMediaCapture = new SeekMediaCapture(this, simpleOverlay);
        seekMediaCapture.setStateCallback(new SeekMediaCapture.StateCallbackAdapter() {
            @Override
            public void onPhotoTaken(SeekMediaCapture seekMediaCapture, CaptureResult result) {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "Capture: " + result.filename, Toast.LENGTH_SHORT).show();
//                    thermalOverlay.bringToFront();
//                    thermalOverlay.animate().alpha(0).setDuration(SHUTTER_SPEED_IN_MILLIS).setInterpolator(new AccelerateDecelerateInterpolator()).setListener(new AnimatorListenerAdapter() {
//                        @Override
//                        public void onAnimationEnd(Animator animation) {
//                            thermalOverlay.setAlpha(1.0f);
//                            seekPreview.bringToFront();
//                        }
//                    }).start();
                });
            }

            @Override
            public void onVideoRecordingStarted(SeekMediaCapture seekMediaCapture, VideoCaptureRequest captureRequest) {
                runOnUiThread(() -> {
                    captureButton.setVisibility(View.INVISIBLE);
                    stopButton.setVisibility(View.VISIBLE);
                    Toast.makeText(getApplicationContext(), "Video Recording Started: " + captureRequest.filename, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onVideoRecordingEnded(SeekMediaCapture seekMediaCapture, CaptureResult captureResult) {
                runOnUiThread(() -> {
                    captureButton.setVisibility(View.VISIBLE);
                    stopButton.setVisibility(View.INVISIBLE);
                    Toast.makeText(getApplicationContext(), "Video Recording Ended: " + captureResult.filename, Toast.LENGTH_SHORT).show();
                });
            }
        });
        seekCameraManager = new SeekCameraManager(this, null, stateCallback);
        // Create PermissionHandler
        permissionHandler = SeekUtility.PermissionHandler.getInstance();
        SeekUtility.OrientationManager.getInstance().addViews(findViewById(R.id.color_lut), switchButton);
    }

    private void updateVideoInfo(){
        runOnUiThread(() -> {
            if(seekMediaCapture.isRecording()){
                videoInfo.setText(SeekUtility.formatTime(seekMediaCapture.getElapsedTime(), 0, false));
            } else {
                videoInfo.setText("");
            }
        });
    }

    public void onCaptureClicked(View v) {
        if(photo){
            seekMediaCapture.takePhoto();
        } else {
            seekMediaCapture.startRecording();
        }
    }

    public void onStopClicked(View v) {
        seekMediaCapture.stopRecording();
    }

    public void onSwitchClicked(View v) {
        if (photo) {
            switchButton.setBackgroundResource(R.drawable.ic_switch_camera);
            captureButton.setBackgroundTintList(ColorStateList.valueOf(VIDEO_CAPTURE_BUTTON_COLOR));
            photo = false;
        } else {
            switchButton.setBackgroundResource(R.drawable.ic_switch_video);
            captureButton.setBackgroundTintList(ColorStateList.valueOf(PHOTO_CAPTURE_BUTTON_COLOR));
            photo = true;
        }
    }

    public void onColorLutClicked(View v){
        colorPalettePicker.setVisibility(colorPalettePicker.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
    }

    public void onCaptureLongClicked(View v) {
        if (thermalCamera != null) {
            thermalCamera.triggerShutter();
            Toast.makeText(getApplicationContext(), "Shutter Triggered", Toast.LENGTH_SHORT).show();
        }
    }
}