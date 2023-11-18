package cis.pintu;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import org.json.JSONObject;


public class GCMRegistrationService extends FirebaseInstanceIdService {

    private static final String TAG = "GCMRegistrationIntent";

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        //Log.d(TAG, "Refreshed_token: " + refreshedToken);
        sendRegistrationToServer(refreshedToken);
    }

    private void sendRegistrationToServer(String token) {
       // Log.d("Token", token);
        try {
            JSONObject data = new JSONObject();
            data.put("uuid", new Helper().getUUID(getApplicationContext()));
            data.put("token", token);
            data.put("macid", Helper.getMacAddr());
            new NR(Helper.baseurl + "token.php", 8000, "POST", data.toString(), getApplicationContext()).networkRequest(new ResultCallback() {
                @Override
                public void onSuccess(String result) {

                }

                @Override
                public void onError(String result) {

                }
            });
        }catch (Exception er){}

    }


}
