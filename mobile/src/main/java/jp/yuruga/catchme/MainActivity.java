package jp.yuruga.catchme;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.NotificationCompat.WearableExtender;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jp.yuruga.catchme.R;


public class MainActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener, NodeApi.NodeListener, DataApi.DataListener{

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    public double latitude = 0;
    public double longitude = 0;

    //log tag
    private static final String TAG = "MainActivity";
    /** Request code for launching the Intent to resolve Google Play services errors. */
    private static final int REQUEST_RESOLVE_ERROR = 1000;

    //action
    //public static final String ACTION_OPEN_WEAR_APP = "jp.yuruga.catchme.OPEN_WEAR_APP";

    //data api key and path
    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String CATCH_ACTION_PATH = "/catch";
    private static final String POSITION_PATH = "/position";
    private static final String POSITION_KEY = "position";

    private static final int NOTIFICATION_CATCHED = 0;
    private static final int NOTIFICATION_CATCH = 1;

    //google api
    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;
    private Handler mHandler;

    // Send DataItems.
    private ScheduledExecutorService mGeneratorExecutor;
    private ScheduledFuture<?> mDataItemGeneratorFuture;
    private int mPositionData = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();
        mGeneratorExecutor   = new ScheduledThreadPoolExecutor(1);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        setUpMapIfNeeded();

        setUpData();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mDataItemGeneratorFuture = mGeneratorExecutor.scheduleWithFixedDelay(
                new PositionDataSender(), 1, 5, TimeUnit.SECONDS);
        setUpMapIfNeeded();
    }

    @Override
    public void onPause() {
        super.onPause();
        mDataItemGeneratorFuture.cancel(true /* mayInterruptIfRunning */);
    }

//    @Override
//    protected void onDestroy()
//    {
//        super.onDestroy();
//        removeSelfData();
//    }


    @Override
    protected void onNewIntent(Intent intent) {
        LOGD(TAG,"onNewIntent");
        super.onNewIntent(intent);
        String action = intent.getAction();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {            // Try to obtain the map from the SupportMapFragment.

                setUpMap();
            }
        }
    }
    private void setUpMap() {
//        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));

        //Google MapのMyLocationレイヤーを使用可能にする
        mMap.setMyLocationEnabled(true);
        mMap.setIndoorEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(true);

        //MapEvents
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                latitude = latLng.latitude;
                longitude = latLng.longitude;
                updateSelfData();
                Toast.makeText(getApplicationContext(), "タップ位置\n緯度：" + latLng.latitude + "\n経度:" + latLng.longitude, Toast.LENGTH_LONG).show();
            }
        });

        mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                //Toast.makeText(getApplicationContext(), "タップ位置\n緯度：" + location.getLatitude() + "\n経度:" + location.getLongitude(), Toast.LENGTH_LONG).show();
            }
        });

        //システムサービスのLOCATION_SERVICEからLocationManager objectを取得
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        //retrieve providerへcriteria objectを生成
        Criteria criteria = new Criteria();

        //Best providerの名前を取得
        String provider = locationManager.getBestProvider(criteria, true);

        //現在位置を取得
        Location location = locationManager.getLastKnownLocation(provider);

        //現在位置の緯度を取得
        latitude = location.getLatitude();

        //現在位置の経度を取得
        longitude = location.getLongitude();

        //現在位置からLatLng objectを生成
        LatLng latLng = new LatLng(latitude, longitude);

        //Google Mapに現在地を表示
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        //Google Mapの Zoom値を指定
        mMap.animateCamera(CameraUpdateFactory.zoomTo(20));
    }

    public void onSendNotificationClicked(View view) {
        LOGD(TAG,"sendNotificationClicked");
        //createNotificationAndSend();
    }

    private void sendNotification(int notificationId){
        String title;
        String text;
        switch (notificationId)
        {

            case NOTIFICATION_CATCH:
            {
                title = getResources().getString(R.string.notification_title_win);
                text = "";
                break;
            }
            case NOTIFICATION_CATCHED:
            {
                title = getResources().getString(R.string.notification_title_lose);
                text = "";
            }
        }

        //main notification
        NotificationCompat.Builder notifBulder = new NotificationCompat.Builder(this)
                .setContentTitle("You are catched!")
                .setContentText("")
                //.setSound(Uri.parse("android.resource://jp.mdnht.drawmessenger/raw/yo"))
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                .setSmallIcon(R.drawable.common_signin_btn_icon_dark);
                //.setLargeIcon(imageBitmap);


        /*// Create the action
        NotificationCompat.Action action =
                new NotificationCompat.Action.Builder(R.drawable.common_signin_btn_icon_disabled_focus_light,"launch wear app", actionPendingIntent)
                        .build();*/

        // Create a WearableExtender to add functionality for wearables
        Notification notif = notifBulder.build();
               // new WearableExtender()
                        //.addPage(secondPageNotification)
                        //.addAction(action)
                        //.extend(notifBulder)
                        //.build();

        // Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);

        // Build the notification and issues it with notification manager.
        notificationManager.notify(notificationId, notif);
        //notificationId ++;
    }



    private void sendPosition(long[] data) {
        //long[] data = new long[3];
        PutDataMapRequest dataMapReq = PutDataMapRequest.create(POSITION_PATH);
        dataMapReq.getDataMap().putLongArray(POSITION_KEY, data);
        PutDataRequest request = dataMapReq.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        LOGD(TAG, "Sending data was successful: " + dataItemResult.getStatus()
                                .isSuccess());
                    }
                });

    }

    private class StartWearableActivityTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... args) {
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                sendStartActivityMessage(node);
            }
            return null;
        }
    }

    private void sendStartActivityMessage(String node) {
        LOGD(TAG,"sending start activity message");
        Wearable.MessageApi.sendMessage(
                mGoogleApiClient, node, START_ACTIVITY_PATH, new byte[0]).setResultCallback(
                new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Failed to send message with status code: "
                                    + sendMessageResult.getStatus().getStatusCode());
                        }
                    }
                }
        );
    }


    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        LOGD(TAG, "onDataChanged(): " + dataEvents);
    }

    @Override
    public void onPeerConnected(Node peer) {
        LOGD(TAG, "onPeerConnected: " + peer);
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        LOGD(TAG, "onPeerDisConnected: " + peer);
    }



    //GoogleApiClient
    @Override
    public void onConnected(Bundle connectionHint) {
        LOGD(TAG, "Google API Client was connected");
        mResolvingError = false;
        //mStartActivityBtn.setEnabled(true);
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.NodeApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        LOGD(TAG, "Connection to Google API client was suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            Log.e(TAG, "Connection to Google API client has failed");
            mResolvingError = false;
            //mStartActivityBtn.setEnabled(false);
            //mSendPhotoBtn.setEnabled(false);
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        LOGD(TAG, "onMessageReceived() A message from watch was received:" + messageEvent
                .getRequestId() + " " + messageEvent.getPath());
        String path = messageEvent.getPath();
        if(CATCH_ACTION_PATH == path)
        {

        }
    }

    private Collection<String> getNodes() {
        HashSet<String> results= new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }
        return results;
    }


    /** Generates a DataItem based on an incrementing count. */
    private class PositionDataSender implements Runnable {

        private int count = 0;


        @Override
        public void run() {
            LOGD(TAG, "send position data"+mPositionData);
            /*PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(COUNT_PATH);
            putDataMapRequest.getDataMap().putInt(COUNT_KEY, count++);
            PutDataRequest request = putDataMapRequest.asPutDataRequest();

            LOGD(TAG, "Generating DataItem: " + request);
            if (!mGoogleApiClient.isConnected()) {
                return;
            }
            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            if (!dataItemResult.getStatus().isSuccess()) {
                                Log.e(TAG, "ERROR: failed to putDataItem, status code: "
                                        + dataItemResult.getStatus().getStatusCode());
                            }
                        }
                    });*/

            PutDataMapRequest dataMapReq = PutDataMapRequest.create(POSITION_PATH);
            dataMapReq.getDataMap().putInt(POSITION_KEY, mPositionData);
            PutDataRequest request = dataMapReq.asPutDataRequest();
            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            LOGD(TAG, "Sending data was successful: " + dataItemResult.getStatus()
                                    .isSuccess());
                        }
                    });

            mPositionData ++;
        }
    }

    /**
     * As simple wrapper around Log.d
     */
    private static void LOGD(final String tag, String message) {
        if (Log.isLoggable(tag, Log.INFO)) {
            Log.d(tag, message);
        }
    }

    /**
     * firebase
     */
    private Firebase _usersRef;
    private String _macAddress;
    private Map<String, User> _users;
    private Boolean _isfirst = false;
    public class User {
        private double latitude;
        private double longitude;
        private double distance;
        public User() {}
        public User(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
        public double getLatitude() {
            return latitude;
        }
        public double getLongitude() {
            return longitude;
        }
        public void setLatitude(double val) {
            latitude = val;
        }
        public void setLongitude(double val) {
            longitude = val;
        }
        public double getDistance() {
            return distance;
        }
        public void setDistance(double d) {
            distance = d;
        }
    }

    public void setUpData(){

        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        _macAddress = wifiInfo.getMacAddress();
        if(_macAddress == null){
            _macAddress = "emu";
        }

        Firebase ref = new Firebase("https://amber-inferno-5746.firebaseio.com/");
        _usersRef = ref.child("users");
        _usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                _users = (Map<String, User>)dataSnapshot.getValue();
                if(_users == null){
                    _users = new HashMap<String, User>();
                }
                if(!_isfirst){
                    _isfirst = true;
                    updateSelfData();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    public void updateSelfData()
    {
        if(_users.containsKey(_macAddress)){
            _users.remove(_macAddress);
        }
        User user = new User(latitude, longitude);
        _users.put(_macAddress, user);
        _usersRef.setValue(_users);
    }

    public Map<String, User> getUsers(){
        User self = _users.get(_macAddress);
        User user;
        for (String key : _users.keySet()) {
            if(key == _macAddress){
                user = _users.get(key);
                user.setDistance(getDistance(self.getLatitude(), self.getLongitude(), user.getLatitude(), user.getLongitude()));
            }else{
                self.setDistance(0);
            }
        }
        return _users;
    }

    public User getNearUser() {
        Map<String, User> users = getUsers();
        double min = -1;
        double currentMin;
        User user = null;
        for (String key : users.keySet()) {
            currentMin = users.get(key).getDistance();
            if(min < 0 || users.get(key).getDistance() < min){
                min = currentMin;
                user = users.get(key);
            }
        }
        return user;
    }


    public double getDistance(double fLat, double fLon, double tLat, double tLon){
        double er = 6378.137f;
        double diffLat = Math.PI / 180 * (tLat - fLat);
        double diffLon = Math.PI / 180 * (tLon - fLon);
        double disLat = er * diffLat;
        double disLon = Math.cos(Math.PI / 180 * fLat) * er * diffLon;
        double dis = Math.sqrt(Math.pow(disLon, 2) + Math.pow(disLat, 2));
        return dis * 1000;
    }

    public void removeSelfData(){
        if(_users.containsKey(_macAddress)){
            _users.remove(_macAddress);
        }
        _usersRef.setValue(_users);
    }
}
