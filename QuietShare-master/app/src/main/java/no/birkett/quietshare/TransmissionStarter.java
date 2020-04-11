package no.birkett.quietshare;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import org.quietmodem.Quiet.FrameTransmitter;
import org.quietmodem.Quiet.FrameTransmitterConfig;
import org.quietmodem.Quiet.ModemException;

import java.io.IOException;
import java.nio.charset.Charset;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

public class TransmissionStarter extends Service {


    boolean isServiceStarted = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    String ProfileFormat = "audible";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            startUltrasoundService(intent.getExtras().getInt("delay"));
        } else {

        }
        return START_STICKY;
    }

    private Handler mHandler;

    private HandlerThread mHandlerThread;

    private void startUltrasoundService(int delay) {
        if (isServiceStarted) {
            return;
        } else {
            isServiceStarted = true;


            initiateTransmission();
            initiateReceiver();
            mHandlerThread = new HandlerThread("HandlerThread");
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Your task goes here

                    if (isServiceStarted) {

                        send();
                        mHandler.postDelayed(this, delay);

                    } else {

                    }

                }
            }, delay);
        }
    }

    private FrameTransmitter transmitter;

    private void initiateTransmission() {
        FrameTransmitterConfig transmitterConfig;
        try {
            transmitterConfig = new FrameTransmitterConfig(
                    this, ProfileFormat);
            transmitter = new FrameTransmitter(transmitterConfig);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ModemException e) {
            throw new RuntimeException(e);
        }
    }

    private void send() {
        String payload = "Customdata" + "%" + System.currentTimeMillis();
        try {
            transmitter.send(payload.getBytes());
        } catch (IOException e) {
            // our message might be too long or the transmit queue full
        }
    }

    private Subscription frameSubscription = Subscriptions.empty();

    private void initiateReceiver() {

        frameSubscription.unsubscribe();
        frameSubscription = FrameReceiverObservable.create(this, ProfileFormat).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(buf -> {
            String receivedData = new String(buf, Charset.forName("UTF-8"));

            long receivedTs = System.currentTimeMillis() - Long.parseLong(receivedData.split("%")[1]);
            Long time = System.currentTimeMillis() / 1000;
            double distance = (35 * (receivedTs));

            if (distance < 400) {
                generateAlertNotification();
            }


        }, error -> {
            // receiveStatus.setText("error " + error.toString());
        });

    }


    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(2, createNotification());
    }


    public Notification createNotification() {

        Context mContext;
        NotificationManager mNotificationManager;
        NotificationCompat.Builder mBuilder;
        final String NOTIFICATION_CHANNEL_ID = "10001";
        /**Creates an explicit intent for an Activity in your app**/
        Intent resultIntent = new Intent(TransmissionStarter.this, TransmitActivity.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(TransmissionStarter.this,
                0 /* Request code */, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder = new NotificationCompat.Builder(TransmissionStarter.this);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.setContentTitle("Check for distance")
                .setContentText("This is your favourite check for distance service running")
                .setAutoCancel(false)
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .setContentIntent(resultPendingIntent);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "NOTIFICATION_CHANNEL_NAME", importance);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            assert mNotificationManager != null;
            mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);
            mNotificationManager.createNotificationChannel(notificationChannel);
        }

        return mBuilder.build();
    }

    public void generateAlertNotification() {

        Context mContext;
        NotificationManager mNotificationManager;
        NotificationCompat.Builder mBuilder;
        final String NOTIFICATION_CHANNEL_ID = "10001";
        /**Creates an explicit intent for an Activity in your app**/
        Intent resultIntent = new Intent(TransmissionStarter.this, TransmitActivity.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(TransmissionStarter.this,
                0 /* Request code */, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder = new NotificationCompat.Builder(TransmissionStarter.this);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.setContentTitle("Varna")
                .setContentText("Behåll tillräckligt med social avstånd")
                .setAutoCancel(false)
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .setContentIntent(resultPendingIntent);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "NOTIFICATION_CHANNEL_NAME", importance);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            assert mNotificationManager != null;
            mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);
            mNotificationManager.createNotificationChannel(notificationChannel);
        }

        assert mNotificationManager != null;
        mNotificationManager.notify(7 /* Request Code */, mBuilder.build());
    }


    @Override
    public void onDestroy() {
        isServiceStarted = false;
        frameSubscription.unsubscribe();
        transmitter.close();
        super.onDestroy();
    }
}




