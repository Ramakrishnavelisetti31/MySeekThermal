package com.example.seekdeliverain;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import com.google.android.material.tabs.TabLayout;
import com.thermal.seekware.ColorBar;
import com.thermal.seekware.GLContext;
import com.thermal.seekware.GLObject;
import com.thermal.seekware.GLSprite;
import com.thermal.seekware.GLText;
import com.thermal.seekware.SeekCamera;
import com.thermal.seekware.SeekOverlay;
import com.thermal.seekware.SeekUtility;
import com.thermal.seekware.Thermography;

import java.util.Objects;

public class SimpleOverlay extends SeekOverlay {
    private static final float DEFAULT_FONT_SIZE = 0.04f;
    private static final Scale DEFAULT_SCALE = Scale.SHORT_DIMENSION;
    private static final PointF DEFAULT_COLOR_BAR_SIZE = new PointF(0.775f, 0.05f);
    private static final PointF DEFAULT_RETICLE_SIZE = new PointF(0.2f, 0.2f);
    private static final int DEFAULT_SPOT_COLOR = 0xCCFFFFFF;
    private static final PointF DEFAULT_WATERMARK_SIZE = new PointF(0.25f, 0.25f);
    private static final int DEFAULT_WATERMARK_COLOR = 0xCCFFFFFF;

    private GLSprite centerSpotSprite;
    private GLText glText;

    public SimpleOverlay(Context context) {
        super(context);
        centerSpotSprite = new GLSprite(context);
        glText = new GLText(context);
        centerSpotSprite.setStateCallback(new GLObject.StateCallbackAdapter() {
            @Override
            public void onLongPress(GLObject glObject, MotionEvent e, Size screenSize) {
                centerSpotSprite.setVisible(!centerSpotSprite.isVisible());
            }

            @Override
            public void onClick(GLObject glObject, MotionEvent e, Size screenSize) {
                // Toggle Temperature.Units
                if (Thermography.getTemperatureUnit() == SeekUtility.Temperature.Unit.CELSIUS) {
                    Thermography.setTemperatureUnit(SeekUtility.Temperature.Unit.FAHRENHEIT);
                } else {
                    Thermography.setTemperatureUnit(SeekUtility.Temperature.Unit.CELSIUS);
                }
            }
        });
        getColorBar().setStateCallback(new GLObject.StateCallbackAdapter() {
            @Override
            public void onLongPress(GLObject glObject, MotionEvent e, Size screenSize) {
                getColorBar().setVisible(!getColorBar().isVisible());
            }
            @Override
            public void onClick(GLObject glObject, MotionEvent e, Size size) {
                if (getSeekImage().getThermography() != null) {
                    Thermography thermography = getSeekImage().getThermography();
                    SeekCamera seekCamera = thermography.getSeekCamera();
                    Dialog dialog = new Dialog(context);
                    dialog.setCancelable(true);
                    dialog.setContentView(R.layout.color_bar_dialog);
                    Button resetButton = dialog.findViewById(R.id.reset);
                    Button cancelButton = dialog.findViewById(R.id.cancel);
                    Button confirmButton = dialog.findViewById(R.id.confirm);
                    EditText minLockValue = dialog.findViewById(R.id.min_lock_value);
                    EditText maxLockValue = dialog.findViewById(R.id.max_lock_value);
                    RadioGroup radioGroup = dialog.findViewById(R.id.radio_group);
                    RadioGroup dynamicRadioGroup = dialog.findViewById(R.id.dynamic_radio_group);
                    TabLayout agcTabs = dialog.findViewById(R.id.agc_tabs);
                    RadioGroup cornerRadioGroup = dialog.findViewById(R.id.corner_radio_group);
                    RadioGroup borderRadioGroup = dialog.findViewById(R.id.border_radio_group);
                    LinearLayout locks = dialog.findViewById(R.id.locks);
                    agcTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                        @Override
                        public void onTabSelected(TabLayout.Tab tab) {
                            switch (Objects.requireNonNull(tab.getText()).toString()) {
                                case "LINEAR":
                                    locks.setVisibility(View.VISIBLE);
                                    dynamicRadioGroup.setVisibility(View.INVISIBLE);
                                    if (seekCamera.getLinearMinMaxMode() == SeekCamera.LinearMinMaxMode.AUTO || seekCamera.getLinearMinMaxMode() == null) {
                                        resetButton.setVisibility(View.INVISIBLE);
                                    } else {
                                        resetButton.setVisibility(View.VISIBLE);
                                    }
                                    break;
                                case "HIST EQ":
                                    dynamicRadioGroup.setVisibility(View.VISIBLE);
                                    locks.setVisibility(View.INVISIBLE);
                                    resetButton.setVisibility(View.INVISIBLE);
                                    break;
                            }
                        }

                        @Override
                        public void onTabUnselected(TabLayout.Tab tab) {

                        }

                        @Override
                        public void onTabReselected(TabLayout.Tab tab) {

                        }
                    });
                    int index = getSeekImage().getSeekCamera().getCharacteristics().getAgcMode() == SeekCamera.AGCMode.LINEAR ? 0 : 1;
                    TabLayout.Tab tab = agcTabs.getTabAt(index);
                    if (tab != null) {
                        tab.select();
                    }
                    dynamicRadioGroup.check(getColorBar().isDynamic() ? R.id.dynamic : R.id.full);
                    cornerRadioGroup.check(getColorBar().hasRoundedCorners() ? R.id.rounded : R.id.straight);
                    borderRadioGroup.check(getColorBar().getBorderSize() != 0 ? R.id.border : R.id.no_border);
                    radioGroup.check(Thermography.getTemperatureUnit() == SeekUtility.Temperature.Unit.CELSIUS ?
                            R.id.celsius : R.id.fahrenheit);
                    if (getSeekImage().getSeekCamera().getCharacteristics().getAgcMode() == SeekCamera.AGCMode.HISTEQ) {
                        locks.setVisibility(View.INVISIBLE);
                        resetButton.setVisibility(View.INVISIBLE);
                        dynamicRadioGroup.setVisibility(View.VISIBLE);
                    } else {
                        locks.setVisibility(View.VISIBLE);
                        dynamicRadioGroup.setVisibility(View.INVISIBLE);
                        if (seekCamera.getLinearMinMaxMode() == SeekCamera.LinearMinMaxMode.AUTO || seekCamera.getLinearMinMaxMode() == null) {
                            resetButton.setVisibility(View.INVISIBLE);
                        } else {
                            resetButton.setVisibility(View.VISIBLE);
                        }
                    }

                    if (seekCamera != null && seekCamera.getLinearMinMaxMode() != null && getSeekImage().getSeekCamera().getCharacteristics().getAgcMode() == SeekCamera.AGCMode.LINEAR) {
                        switch (seekCamera.getLinearMinMaxMode()) {
                            case MANUAL:
                                if (minLockValue != null) {
                                    minLockValue.setText(seekCamera.getLinearMinLockValue().toShortString());
                                }
                                if (maxLockValue != null) {
                                    maxLockValue.setText(seekCamera.getLinearMaxLockValue().toShortString());
                                }
                                break;
                            case MIN_LOCKED:
                                if (minLockValue != null) {
                                    minLockValue.setText(seekCamera.getLinearMinLockValue().toShortString());
                                }
                                if (maxLockValue != null) {
                                    maxLockValue.setText("");
                                }
                                break;
                            case MAX_LOCKED:
                                if (minLockValue != null) {
                                    minLockValue.setText("");
                                }
                                if (maxLockValue != null) {
                                    maxLockValue.setText(seekCamera.getLinearMaxLockValue().toShortString());
                                }
                                break;
                            case AUTO:
                                if (minLockValue != null) {
                                    minLockValue.setText("");
                                }
                                if (maxLockValue != null) {
                                    maxLockValue.setText("");
                                }
                                break;
                        }
                    }
                    resetButton.setOnClickListener(v -> {
                        if (seekCamera != null) {
                            seekCamera.setLinearMinMaxMode(SeekCamera.LinearMinMaxMode.AUTO);
                        }
                        if (minLockValue != null) {
                            minLockValue.setText("");
                        }
                        if (maxLockValue != null) {
                            maxLockValue.setText("");
                        }
                        resetButton.setVisibility(View.INVISIBLE);
                    });
                    if (cancelButton != null) {
                        cancelButton.setOnClickListener(v -> dialog.dismiss());
                    }
                    if (confirmButton != null) {
                        confirmButton.setOnClickListener(v -> {
                            if (seekCamera != null) {
                                if (agcTabs.getSelectedTabPosition() == 0) {
                                    seekCamera.setAGCMode(SeekCamera.AGCMode.LINEAR);
                                } else {
                                    seekCamera.setAGCMode(SeekCamera.AGCMode.HISTEQ);
                                }
                            }
                            getColorBar().setRoundedCorners(cornerRadioGroup.getCheckedRadioButtonId() == R.id.rounded);
                            getColorBar().setDynamic(dynamicRadioGroup.getCheckedRadioButtonId() == R.id.dynamic);
                            getColorBar().setBorderSize(borderRadioGroup.getCheckedRadioButtonId() == R.id.border ? ColorBar.DEFAULT_BORDER_SIZE : 0f);
                            if (radioGroup.getCheckedRadioButtonId() == R.id.celsius) {
                                Thermography.setTemperatureUnit(SeekUtility.Temperature.Unit.CELSIUS);
                            } else {
                                Thermography.setTemperatureUnit(SeekUtility.Temperature.Unit.FAHRENHEIT);
                            }
                            if (seekCamera != null) {
                                if (minLockValue != null && maxLockValue != null) {
                                    if (!minLockValue.getText().toString().equals("") && !maxLockValue.getText().toString().equals("")) {
                                        seekCamera.setLinearMinMaxMode(SeekCamera.LinearMinMaxMode.MANUAL);
                                        seekCamera.setLinearMinLockValue(
                                                new SeekUtility.Temperature(Integer.parseInt(
                                                        minLockValue.getText().toString()),
                                                        Thermography.getTemperatureUnit()));
                                        seekCamera.setLinearMaxLockValue(
                                                new SeekUtility.Temperature(Integer.parseInt(
                                                        maxLockValue.getText().toString()),
                                                        Thermography.getTemperatureUnit()));
                                    } else if (!minLockValue.getText().toString().equals("")) {
                                        seekCamera.setLinearMinMaxMode(SeekCamera.LinearMinMaxMode.MIN_LOCKED);
                                        seekCamera.setLinearMinLockValue(
                                                new SeekUtility.Temperature(Integer.parseInt(
                                                        minLockValue.getText().toString()),
                                                        Thermography.getTemperatureUnit()));
                                    } else if (!maxLockValue.getText().toString().equals("")) {
                                        seekCamera.setLinearMinMaxMode(SeekCamera.LinearMinMaxMode.MAX_LOCKED);
                                        seekCamera.setLinearMaxLockValue(
                                                new SeekUtility.Temperature(Integer.parseInt(
                                                        maxLockValue.getText().toString()),
                                                        Thermography.getTemperatureUnit()));
                                    }
                                }
                            }
                            dialog.dismiss();
                        });
                    }
                    dialog.show();
                }
            }
        });
        centerSpotSprite.setSprite(SeekUtility.getBitmapFromDrawable(context, R.drawable.ic_center_reticle));
        addGLObject(centerSpotSprite);
        addGLObject(glText);
        addGLObject(getColorBar());
    }

    @Override
    protected void loadFont(GLContext glContext, Size size) {
        glText.loadFont(glContext, "roboto_medium.ttf", DEFAULT_FONT_SIZE, Scale.SHORT_DIMENSION, size, 0, 0);
    }

    @Override
    protected void renderBackground(GLContext glContext, int i, Size size, Size size1, float[] floats) {

    }

    @Override
    protected void renderForeground(GLContext glContext, int orientation, Size screenSize, Size uncroppedSize, float[] vpMatrix) {
        renderColorBar(glContext, orientation, screenSize, vpMatrix);
    }

    @Override
    protected void renderSpot(GLContext drawingContext, Thermography.Spot spot, Point center, Size screenSize, Size uncroppedSize, int orientation, float[] vpMatrix) {
        if(spot.getType() == Thermography.Spot.Type.CENTER){
            Size size = toPixelSize(DEFAULT_RETICLE_SIZE, screenSize, DEFAULT_SCALE, DEFAULT_SCALE);
            centerSpotSprite.beginDraw(drawingContext, vpMatrix);
            // draw spot reticle
            centerSpotSprite.draw(size, center, 0f, DEFAULT_SPOT_COLOR);
            centerSpotSprite.endDraw();
            GLText.FontCharacteristics fontCharacteristics = glText.getFontCharacteristics(drawingContext);
            if (fontCharacteristics == null) {
                fontCharacteristics = new GLText.FontCharacteristics();
            }
            int textOffset = (int) (Math.min(screenSize.getWidth(), screenSize.getHeight()) * DEFAULT_RETICLE_SIZE.x / 2 - fontCharacteristics.getCharHeight(GLText.DEFAULT_SCALE_Y) / 2);
            switch (orientation) {
                case 0:
                    center.y += center.y > textOffset * 2 ? -textOffset : textOffset;
                    break;
                case 90:
                    center.x += center.x < screenSize.getWidth() - textOffset * 2 ? textOffset : -textOffset;
                    break;
                case 180:
                    center.y += center.y < screenSize.getHeight() - textOffset * 2 ? textOffset : -textOffset;
                    break;
                case 270:
                    center.x += center.x > textOffset * 2 ? -textOffset : textOffset;
                    break;
                default:
                    break;
            }
            if(centerSpotSprite.isVisible() && !spot.getTemperature().toString().equals("")){
//                 draw text overlay
                glText.beginDraw(drawingContext, vpMatrix);
                glText.draw(spot.getTemperature().toString(), center, orientation);
                glText.endDraw();
            }
        }
    }

    private void renderColorBar(GLContext drawingContext, int orientation, Size screenSize, float[] vpMatrix) {
        ColorBar colorBar = getColorBar();
        Size size = toPixelSize(DEFAULT_COLOR_BAR_SIZE, screenSize, DEFAULT_SCALE, DEFAULT_SCALE);
        if(screenSize.getWidth() > screenSize.getHeight()){
            // update color bar settings
            colorBar.setFlipped(orientation == 270 || orientation == 180);
            colorBar.beginDraw(drawingContext, vpMatrix);
            int openSpaceVertical = screenSize.getHeight() - size.getWidth();
            Point colorPoint = new Point(size.getHeight() / 2, screenSize.getHeight() / 2);
            colorBar.draw(size, colorPoint, 90);
            colorBar.endDraw();
            // Render temperature text if min/max mode is off
            if (colorBar.isVisible()) {
                if (minSpot != null && maxSpot != null && !minSpot.getTemperature().toString().equals("NaN") &&
                        !maxSpot.getTemperature().toString().equals("NaN")) {
                    SeekUtility.Temperature maxTemp = maxSpot.getTemperature();
                    SeekUtility.Temperature minTemp = minSpot.getTemperature();
                    boolean upright = orientation == 0 || orientation == 90;
                    GLText.FontCharacteristics fontCharacteristics = glText.getFontCharacteristics(drawingContext);
                    if (fontCharacteristics == null) {
                        fontCharacteristics = new GLText.FontCharacteristics();
                    }
                    int maxOffset = orientation % 180 == 0 ? (int) fontCharacteristics.getLength(maxTemp.toString(), GLText.DEFAULT_SCALE_X) / 4 : 0;
                    int minOffset = orientation % 180 == 0 ? (int) fontCharacteristics.getLength(minTemp.toString(), GLText.DEFAULT_SCALE_X) / 4 : 0;
                    int rightX = screenSize.getHeight() - openSpaceVertical / 4;
                    int leftX = openSpaceVertical / 4;
                    Point minDrawPoint = new Point( colorPoint.x + minOffset * 2, upright ? rightX : leftX);
                    Point maxDrawPoint = new Point( colorPoint.x + maxOffset * 2, upright ? leftX : rightX);
                    glText.beginDraw(drawingContext, vpMatrix);
                    glText.draw(maxTemp.toString(), minDrawPoint, orientation);
                    glText.draw(minTemp.toString(), maxDrawPoint, orientation);
                    glText.endDraw();
                }
            }
        } else {
            // update color bar settings
            colorBar.setFlipped(orientation == 90 || orientation == 180);
            colorBar.beginDraw(drawingContext, vpMatrix);
            int openSpaceHorizontal = screenSize.getWidth() - size.getWidth();
            Point colorPoint = new Point(screenSize.getWidth() / 2, screenSize.getHeight() - size.getHeight() / 2);
            colorBar.draw(size, colorPoint);
            colorBar.endDraw();
            // Render temperature text if min/max mode is off
            if (colorBar.isVisible()) {
                if (minSpot != null && maxSpot != null && !minSpot.getTemperature().toString().equals("NaN") &&
                        !maxSpot.getTemperature().toString().equals("NaN")) {
                    SeekUtility.Temperature maxTemp = maxSpot.getTemperature();
                    SeekUtility.Temperature minTemp = minSpot.getTemperature();
                    boolean upright = orientation == 0 || orientation == 270;
                    GLText.FontCharacteristics fontCharacteristics = glText.getFontCharacteristics(drawingContext);
                    if (fontCharacteristics == null) {
                        fontCharacteristics = new GLText.FontCharacteristics();
                    }
                    int maxOffset = orientation % 180 == 0 ? 0 : (int) fontCharacteristics.getLength(maxTemp.toString(), GLText.DEFAULT_SCALE_X) / 4;
                    int minOffset = orientation % 180 == 0 ? 0 : (int) fontCharacteristics.getLength(minTemp.toString(), GLText.DEFAULT_SCALE_X) / 4;
                    int rightX = screenSize.getWidth() - openSpaceHorizontal / 4;
                    int leftX = openSpaceHorizontal / 4;
                    Point minDrawPoint = new Point(upright ? rightX : leftX, colorPoint.y - minOffset * 2);
                    Point maxDrawPoint = new Point(upright ? leftX : rightX, colorPoint.y - maxOffset * 2);
                    glText.beginDraw(drawingContext, vpMatrix);
                    glText.draw(maxTemp.toString(), minDrawPoint, orientation);
                    glText.draw(minTemp.toString(), maxDrawPoint, orientation);
                    glText.endDraw();
                }
            }
        }
    }
}
