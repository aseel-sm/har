package com.example.har;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Timer;

public class MainActivity extends AppCompatActivity implements SensorEventListener,TextToSpeech.OnInitListener {

    private static final int TIME_STAMP = 100;
    private static final String TAG = "MainActivity";

    private static List<Float> ax,ay,az;
    private static List<Float> gx,gy,gz;
    private static List<Float> lx,ly,lz;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer, mGyroscope, mLinearAcceleration;

    private float[] results;
    private ActivityClassifier classifier;

    private TextToSpeech textToSpeech;

    private TextView bikingTextView, downstairsTextView, joggingTextView, sittingTextView, standingTextView, upstairsTextView, walkingTextView;

    private Switch ClassifierSwitch;
    private Switch SoundSwitch;

    private static String[] labels = {"Biking","DownStairs", "Jogging", "Sitting", "Standing","Upstairs","Waliking",};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initLayoutItems();

        ax=new ArrayList<>(); ay=new ArrayList<>(); az=new ArrayList<>();
        gx=new ArrayList<>(); gy=new ArrayList<>(); gz=new ArrayList<>();
        lx=new ArrayList<>(); ly=new ArrayList<>(); lz=new ArrayList<>();


        ClassifierSwitch = (Switch) findViewById(R.id.classifierSwitch);
        SoundSwitch = (Switch) findViewById(R.id.soundSwitch);

        ClassifierSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked){
                    onPause();
                }
                else {
                    onResume();
                }
            }
        });


        mSensorManager=(SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer=mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mLinearAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        classifier=new ActivityClassifier(getApplicationContext());

        mSensorManager.registerListener(this,mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this,mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this,mLinearAcceleration, SensorManager.SENSOR_DELAY_FASTEST);

        textToSpeech = new TextToSpeech(this, this);
        textToSpeech.setLanguage(Locale.US);
        textToSpeech.setSpeechRate(0.5f);
        ClassifierSwitch.setChecked(true);

    }

    private void initLayoutItems() {
        bikingTextView = findViewById(R.id.biking_prob);
        downstairsTextView = findViewById(R.id.downstairs_prob);
        joggingTextView = findViewById(R.id.jogging_prob);
        sittingTextView  = findViewById(R.id.sitting_prob);
        standingTextView = findViewById(R.id.standing_prob);
        upstairsTextView = findViewById(R.id.upstairs_prob);
        walkingTextView = findViewById(R.id.walking_prob);
    }





    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            ax.add(event.values[0]);
            ay.add(event.values[1]);
            az.add(event.values[2]);
        } else if(sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gx.add(event.values[0]);
            gy.add(event.values[1]);
            gz.add(event.values[2]);
        } else {
            lx.add(event.values[0]);
            ly.add(event.values[1]);
            lz.add(event.values[2]);
        }

        predictActivity();
    }

    @Override
    public void onInit(int status) {
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void predictActivity() {
        List<Float> data=new ArrayList<>();
        if (ax.size() >= TIME_STAMP && ay.size() >= TIME_STAMP && az.size() >= TIME_STAMP
        && gx.size() >= TIME_STAMP && gy.size() >= TIME_STAMP && gz.size() >= TIME_STAMP
        && lx.size() >= TIME_STAMP && ly.size() >= TIME_STAMP && lz.size() >= TIME_STAMP) {
            data.addAll(ax.subList(0,TIME_STAMP));
            data.addAll(ay.subList(0,TIME_STAMP));
            data.addAll(az.subList(0,TIME_STAMP));

            data.addAll(gx.subList(0,TIME_STAMP));
            data.addAll(gy.subList(0,TIME_STAMP));
            data.addAll(gz.subList(0,TIME_STAMP));

            data.addAll(lx.subList(0,TIME_STAMP));
            data.addAll(ly.subList(0,TIME_STAMP));
            data.addAll(lz.subList(0,TIME_STAMP));

            results = classifier.predictProbabilities(toFloatArray(data));
            Log.i(TAG, "predictActivity: "+ Arrays.toString(results));

            bikingTextView.setText(Float.toString(round(results[0],2)) );
            downstairsTextView.setText( Float.toString(round(results[1],2)));
            joggingTextView.setText(Float.toString( round(results[2],2)));
            sittingTextView.setText(Float.toString( round(results[3],2)));
            standingTextView.setText(Float.toString( round(results[4],2)));
            upstairsTextView.setText(Float.toString( round(results[5],2)));;
            walkingTextView.setText(Float.toString(round(results[6],2)));

            data.clear();
            ax.clear(); ay.clear(); az.clear();
            gx.clear(); gy.clear(); gz.clear();
            lx.clear();ly.clear(); lz.clear();



            float max = -1;
            int idx = -1;
            for (int i = 0; i < results.length; i++) {
                if (results[i] > max) {
                    idx = i;
                    max = results[i];
                }
            }
            if(SoundSwitch.isChecked() == true){
                textToSpeech.speak(labels[idx], TextToSpeech.QUEUE_ADD, null, null);
            }


        }
    }

    private float round(float value, int decimal_places) {
        BigDecimal bigDecimal=new BigDecimal(Float.toString(value));
        bigDecimal = bigDecimal.setScale(decimal_places, BigDecimal.ROUND_HALF_UP);
        return bigDecimal.floatValue();
    }

    private float[] toFloatArray(List<Float> data) {
        int i=0;
        float[] array=new float[data.size()];
        for (Float f:data) {
            array[i++] = (f != null ? f: Float.NaN);
        }
        return array;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this,mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this,mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this,mLinearAcceleration, SensorManager.SENSOR_DELAY_FASTEST);
    }

    protected void onPause() {


        mSensorManager.unregisterListener(this,mAccelerometer);
        mSensorManager.unregisterListener(this,mGyroscope);
        mSensorManager.unregisterListener(this,mLinearAcceleration);
        mSensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this);
    }
}