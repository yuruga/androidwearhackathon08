package awear.hackathon2014.jp.catchme;

import android.app.Activity;
import android.content.ComponentName;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;
import android.hardware.SensorManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorEvent;
import android.util.Log;
import android.content.Intent;

public class SearchActivity extends Activity {

    private static final String TAG = "SearchActivity";

    private TextView mTextView;
    private SensorManager mSensorManager;
    private Sensor mMagneticField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });
        mSensorManager = (SensorManager)this.getSystemService(Context.SENSOR_SERVICE);
        mMagneticField = this.mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        Log.d(TAG, "created");
        startService(new Intent(this, MyService.class));
    }
    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(new SensorEventListener () {
            public void onAccuracyChanged (Sensor sensor, int accuracy) {
                Log.d(TAG, "onAccuracyChanged");
            }
            public void onSensorChanged (SensorEvent event) {
                Log.d(TAG, "onSensorChanged");
            }
        }, mMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
    }
}
