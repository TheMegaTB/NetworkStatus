package venot.us.networkstatus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class ServiceReceiver extends BroadcastReceiver {
    public static TelephonyManager telephony;
    public static AndroidPhoneStateListener phoneListener = null;

    public void onReceive(Context context, Intent intent) {
        phoneListener = new AndroidPhoneStateListener();
        telephony = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        telephony.listen(phoneListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

}