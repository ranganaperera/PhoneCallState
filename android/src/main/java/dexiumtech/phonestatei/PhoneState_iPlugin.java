package dexiumtech.phonestatei;

import android.content.Context;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import android.util.Log;

import java.util.Date;


/** PhoneState_iPlugin */
public class PhoneState_iPlugin implements EventChannel.StreamHandler {

    private static final String PHONE_STATE =
            "PHONE_STATE_99";


    /** Plugin registration. */
    public static void registerWith(Registrar registrar) {

        final EventChannel phoneStateCallChannel =
                new EventChannel(registrar.messenger(), PHONE_STATE);
        phoneStateCallChannel.setStreamHandler(
                new PhoneState_iPlugin(registrar.context()));

    }

    private PhoneStateListener mPhoneListener;
    private final TelephonyManager telephonyManager;
    private static int lastState = TelephonyManager.CALL_STATE_IDLE;
    private static Date callStartTime;
    private static boolean isIncoming;
    private static String savedNumber;

    /** flag used for state */
    public static Boolean phoneCallOn=false;


    /** telephone manager */
    private PhoneState_iPlugin(Context context) {
        telephonyManager = (TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE);
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        mPhoneListener = createPhoneStateListener(events);
        telephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    public void onCancel(Object arguments) {
        ///
    }

    PhoneStateListener createPhoneStateListener(final EventChannel.EventSink events){
        return new PhoneStateListener(){
            @Override
            public void onCallStateChanged (int state, String number){
                String message = "";
                if(lastState == state){
                    //No change, debounce extras
                    return;
                }
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                        isIncoming = true;
                        savedNumber = number;
                        message = "RINGING "+number;
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        //Transition of ringing->offhook are pickups of incoming calls.  Nothing done on them
                        if(lastState != TelephonyManager.CALL_STATE_RINGING){
                            isIncoming = false;
                            message = "ANSWERED "+number;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        //Went to idle-  this is the end of a call.  What type depends on previous state(s)
                        if(lastState == TelephonyManager.CALL_STATE_RINGING){
                            //Ring but no pickup-  a miss
                            message = "MISS_CALL "+number;
                        }
                        else if(isIncoming){
                            message = "INCOMING_CALL_DONE "+number;
                        }
                        else{
                            message = "OUTGOING_CALL_DONE "+number;
                        }
                        break;
                }
                lastState = state;
                events.success(message);
            }
        };
    }

}
