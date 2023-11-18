package cis.pintu;

/**
 * Created by cis on 14/09/17.
 */

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MessageService extends FirebaseMessagingService {

    private static final String TAG = "MessageService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
               //Log.d(TAG, "MessagePayload : " + remoteMessage.getData());
                PintuManager.getInstance().scheduleJob(getApplication().getApplicationContext(),remoteMessage.getData());
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) { }
    }

}