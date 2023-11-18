package cis.pintu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
              //boot received
           /*Intent startapp=new Intent(context,PintuLocker.class);
           startapp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
           startapp.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
           startapp.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
           */
           //context.startActivity(startapp);
  }

}
