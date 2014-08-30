package awear.hackathon2014.jp.catchme;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.NotificationCompat.WearableExtender;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Collection;
import java.util.HashSet;


public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener, NodeApi.NodeListener, DataApi.DataListener{

    //log tag
    private static final String TAG = "MainActivity";
    /** Request code for launching the Intent to resolve Google Play services errors. */
    private static final int REQUEST_RESOLVE_ERROR = 1000;

    //action
    public static final String ACTION_OPEN_WEAR_APP = "awear.hackathon2014.jp.OPEN_WEAR_APP";

    //data api key and path
    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String POSITION_PATH = "/image";
    private static final String POSITION_KEY = "photo";

    private int notificationId = 0;

    //google api
    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        LOGD(TAG,"onNewIntent");
        super.onNewIntent(intent);
        String action = intent.getAction();
        if(action == ACTION_OPEN_WEAR_APP)
        {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(notificationId);
            new StartWearableActivityTask().execute();
        }
        /*if(action == ACTION_SEND_NOTIFICATION)
        {
            String imageUrl = intent.getExtras().getString("url");
            new DownloadImageTask().execute(imageUrl);
        }*/
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

    public void onSendNotificationClicked(View view) {
        LOGD(TAG,"sendNotificationClicked");
        createNotificationAndSend();
    }

    private void createNotificationAndSend(){
        //main notification
        NotificationCompat.Builder notifBulder = new NotificationCompat.Builder(this)
                .setContentTitle(getResources().getString(R.string.notification_title))
                .setContentText(getResources().getString(R.string.notification_text01))
                //.setSound(Uri.parse("android.resource://jp.mdnht.drawmessenger/raw/yo"))
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                .setSmallIcon(R.drawable.common_signin_btn_icon_dark);
                //.setLargeIcon(imageBitmap);

        //extender for page2
        /*WearableExtender extender2 = new WearableExtender()
                .setHintShowBackgroundOnly(true)
                .setBackground(imageBitmap);*/
        // Create second page notification
        /*Notification secondPageNotification = new NotificationCompat.Builder(this)
                .setContentTitle("pege2")
                .setContentText("test")
                .extend(extender2)
                .build();*/

        // Create an intent for the reply action
        Intent actionIntent = new Intent(this, MainActivity.class);
        actionIntent.setAction(ACTION_OPEN_WEAR_APP);
        PendingIntent actionPendingIntent =
                PendingIntent.getActivity(this, 0, actionIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        /*Intent actionIntent = new Intent(this, MessageReceiver.class);
        actionIntent.setAction("jp.mdnht.drawmessenger.OPEN_WEAR_APP");
        PendingIntent actionPendingIntent =
                PendingIntent.getBroadcast(this, 1, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);*/

        // Create the action
        NotificationCompat.Action action =
                new NotificationCompat.Action.Builder(R.drawable.common_signin_btn_icon_disabled_focus_light,"launch wear app", actionPendingIntent)
                        .build();

        // Create a WearableExtender to add functionality for wearables
        Notification notif =
                new WearableExtender()
                        //.addPage(secondPageNotification)
                        .addAction(action)
                        .extend(notifBulder)
                        .build();

        // Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);

        // Build the notification and issues it with notification manager.
        notificationManager.notify(notificationId, notif);
        //notificationId ++;
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
        LOGD(TAG, "onDataChangeed: " + dataEvents);
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


    /**
     * As simple wrapper around Log.d
     */
    private static void LOGD(final String tag, String message) {
        if (Log.isLoggable(tag, Log.INFO)) {
            Log.d(tag, message);
        }
    }
}
