package com.pasm.smscast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;


public class SMSMonitor extends BroadcastReceiver {


//    public SMSMonitor() {
//        Log.i("pasm", "SMSMonitor.constructor " + this);
//    }
//
//    protected void finalize ( ) {
//        Log.i("pasm", "SMSMonitor.finalize " + this);
//    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // Вызывается на событии "android.provider.Telephony.SMS_RECEIVED" для получения SMS
        // а также на "android.intent.action.BOOT_COMPLETED" для уведомления о перезагрузке
        // а также на "android.intent.action.MY_PACKAGE_REPLACED" для уведомления об обновлении apk
        // а также на "com.pasm.smscast.TEST_NOTIFY" для тестового уведомления


        if (context == null || intent == null)
            return;

        String action = intent.getAction();
        Log.i(String.valueOf(context.getPackageName()), "SMSMonitor.onReceive:" + action);

        MySettings.init(context.getApplicationContext());

        Intent mIntent = new Intent(context, NotifyService.class);
        mIntent.putExtra("time", System.currentTimeMillis());

        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(action)) {
            // Пришла SMS
            if (MySettings.SMSMonitor) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    Object[] sms_extras = (Object[]) extras.get("pdus");
                    String sms_from = null;
                    StringBuilder sb = new StringBuilder();

                    if (sms_extras != null) {
                        for (Object sms_extra : sms_extras) {
                            SmsMessage msg = SmsMessage.createFromPdu((byte[]) sms_extra);
                            if (sms_from == null)
                                sms_from = msg.getOriginatingAddress();

                            sb.append(msg.getMessageBody());
                        }
                    }

                    mIntent.putExtra("from", sms_from);
                    mIntent.putExtra("body", sb.toString());
                    try {
                        context.startService(mIntent);
                    } catch (Exception e) {
                        Log.e(String.valueOf(context.getPackageName()), "exception in SMSMonitor. " + e.getMessage());
                    }
                }
            }
        } else {
            // На все остальные подписанные уведомления просто информируем текстом типа уведомления
            mIntent.putExtra("from", "internal");

            String body;
            if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
                body = String.format("Обновлено до версии %s", BuildConfig.VERSION_NAME);
            } else {
                String extra;
                extra = intent.getStringExtra("extra");
                body = extra == null ? action : String.format("%s %s", action, extra);
            }
            mIntent.putExtra("body", body);

            try {
                context.startService(mIntent);
            } catch (Exception e) {
                Log.e(String.valueOf(context.getPackageName()), "exception in SMSMonitor.other. " + e.getMessage());
            }
        }
    }
}
