package com.example.fitnesstracker;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private TextView tvCalories, tvGoalStatus, tvStepCount, tvDistance;
    private ProgressBar progressBar;
    private Button btnReset;

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private boolean isSensorPresent = false;

    private int totalSteps = 0;
    private int previousSteps = 0;
    private int currentSteps = 0;

    private static final double CALORIE_GOAL = 500.0;
    private static final double CALORIES_PER_STEP = 0.04;
    private static final double STEP_LENGTH_KM = 0.000762;
    private static final String PREFS_NAME = "FitnessTrackerPrefs";
    private static final String KEY_PREV_STEPS = "previousSteps";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initSensor();
        loadSavedSteps();
        updateUI();

        // Ask permission on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 1);
            }
        }
    }

    private void initViews() {
        tvCalories   = findViewById(R.id.tvCalories);
        tvGoalStatus = findViewById(R.id.tvGoalStatus);
        progressBar  = findViewById(R.id.progressBar);
        tvStepCount  = findViewById(R.id.tvStepCount);
        tvDistance   = findViewById(R.id.tvDistance);
        btnReset     = findViewById(R.id.btnReset);
        progressBar.setMax((int)(CALORIE_GOAL * 10));
        btnReset.setOnClickListener(v -> resetSteps());
    }

    private void initSensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (stepSensor != null) {
            isSensorPresent = true;
        } else {
            Toast.makeText(this, "No step sensor found!", Toast.LENGTH_LONG).show();
            tvCalories.setText("N/A");
        }
    }

    private void loadSavedSteps() {
        previousSteps = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getInt(KEY_PREV_STEPS, 0);
    }

    private void saveSteps(int steps) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putInt(KEY_PREV_STEPS, steps).apply();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            totalSteps = (int) event.values[0];
            currentSteps = totalSteps - previousSteps;
            if (currentSteps < 0) currentSteps = 0;
            updateUI();
        }
    }

    private void updateUI() {
        double calories = currentSteps * CALORIES_PER_STEP;
        double distance = currentSteps * STEP_LENGTH_KM;

        tvCalories.setText(String.format("%.1f", calories));
        tvGoalStatus.setText(String.format("%.1f / %.0f kcal", calories, CALORIE_GOAL));
        progressBar.setProgress((int) Math.min(calories * 10, CALORIE_GOAL * 10));
        tvStepCount.setText(String.valueOf(currentSteps));
        tvDistance.setText(String.format("%.2f km", distance));
    }

    private void resetSteps() {
        previousSteps = totalSteps;
        currentSteps = 0;
        saveSteps(previousSteps);
        updateUI();
        Toast.makeText(this, "Fitness reset! Burn those calories 🔥", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isSensorPresent) {
                    sensorManager.registerListener(this, stepSensor,
                            SensorManager.SENSOR_DELAY_UI);
                }
            } else {
                Toast.makeText(this,
                        "Permission denied! Steps won't be counted.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isSensorPresent)
            sensorManager.registerListener(this, stepSensor,
                    SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isSensorPresent)
            sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}