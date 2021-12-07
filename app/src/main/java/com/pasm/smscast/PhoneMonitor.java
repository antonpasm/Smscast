package com.pasm.smscast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Locale;


public class PhoneMonitor extends BroadcastReceiver {
    // Вызывается на событии "android.intent.action.PHONE_STATE" для получения информации о звонках

    private static PhoneStateListener phoneStateListener;
    private static String lastIncomingNumber = null;
    private static long lastIncomingTimeMillis;

    @Override
    public void onReceive(Context context, Intent intent) {

        if (context == null || intent == null)
            return;

        String action = intent.getAction();
        Log.i("" + context.getPackageName(), "PhoneMonitor.onReceive:" + action);

        MySettings.init(context.getApplicationContext());

        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {

            if (phoneStateListener == null) {

                // Нужно создать и зарегистрировать только 1 копию PhoneStateListener, иначе будут множественные вызовы onCallStateChanged
                phoneStateListener = new PhoneStateListener(){

                    @Override
                    public void onCallStateChanged(int state, String incomingNumber) {

                        if (MySettings.MissedCall) {

                            // Вроде не нужно, но на всякий случай
                            if (incomingNumber == null) {
                                incomingNumber = "";
                            }
                            if (state == TelephonyManager.CALL_STATE_IDLE) {
                                // Вызов завершился
                                if (incomingNumber.equals(lastIncomingNumber)) {
                                    long minutes, seconds;
                                    seconds = (System.currentTimeMillis() - lastIncomingTimeMillis) / 1000;
                                    minutes = seconds / 60;
                                    seconds %= 60;

                                    Intent mIntent = new Intent(context, NotifyService.class);
                                    mIntent.putExtra("time", System.currentTimeMillis());
                                    mIntent.putExtra("from", "".equals(lastIncomingNumber) ? context.getString(R.string.unknownNumber) : lastIncomingNumber);
                                    mIntent.putExtra("body", String.format(Locale.US, "%s (%d:%02d)", context.getString(R.string.call_missed), minutes, seconds));
                                    context.startService(mIntent);
                                }
                                lastIncomingNumber = null;

                            } else if (state == TelephonyManager.CALL_STATE_RINGING) {
                                // Входящий звонок. Запомним номер
                                lastIncomingNumber = incomingNumber;
                                lastIncomingTimeMillis = System.currentTimeMillis();

                            } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                                // Сняли трубку. Значит игнорим этот звонок
                                lastIncomingNumber = null;
                            }
                        } else {
                            // Забудем про входящие звонки, если в настройках отключили мониторинг звонков
                            lastIncomingNumber = null;
                        }
                    }
                };

                TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                telephony.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            }

//            Bundle bundle = intent.getExtras();
//            if (bundle != null) {
//                for (String key : bundle.keySet()) {
//                    Log.e(getPackageName(), "PhoneMonitor " + key + ":" + String.valueOf(bundle.get(key)));
//                    //phoneState += "\n" + key + ": " + String.valueOf(bundle.get(key));
//                }
//            }

        }
    }
}
