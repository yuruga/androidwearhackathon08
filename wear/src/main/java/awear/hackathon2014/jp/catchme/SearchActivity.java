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

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class SearchActivity extends Activity {

    private static final String TAG = "SearchActivity";

    private TextView mTextView;
    private SensorManager mSensorManager;
    private Sensor mMagneticField;
    GoogleApiClient mGoogleApiClient;


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

        Log.d(TAG, "send test");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        PutDataMapRequest dataMap = PutDataMapRequest.create(MyService.POSITION_PATH);
                        dataMap.getDataMap().putInt(MyService.POSITION_KEY, (int)(Math.random()*1000));
                        PutDataRequest request = dataMap.asPutDataRequest();
                        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, request);
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
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
