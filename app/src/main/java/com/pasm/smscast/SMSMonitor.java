package com.pasm.smscast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;


public class SMSMonitor extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Вызывается на событии "android.provider.Telephony.SMS_RECEIVED" для получения SMS
        // а также на "android.intent.action.BOOT_COMPLETED" для уведомления о перезагрузке
        // а также на "android.intent.action.MY_PACKAGE_REPLACED" для уведомления об обновлении apk
        // а также на "com.pasm.smscast.TEST_NOTIFY" для тестового уведомления


        if (context == null || intent == null)
            return;

        String action = intent.getAction();
        Log.i("" + context.getPackageName(), "SMSMonitor.onReceive:" + action);
        Intent mIntent = new Intent(context, NotifyService.class);
        mIntent.putExtra("time", System.currentTimeMillis());

        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(action)) {
            // Пришла SMS
            Bundle extras = intent.getExtras();
            if (extras != null) {
                Object[] smsextras = (Object[]) extras.get("pdus");
                String sms_from = null;
                StringBuilder sb = new StringBuilder();

                for (Object smsextra : smsextras) {
                    SmsMessage msg = SmsMessage.createFromPdu((byte[]) smsextra);
                    if (sms_from == null)
                        sms_from = msg.getOriginatingAddress();

                    sb.append(msg.getMessageBody());
                }

                mIntent.putExtra("from", sms_from);
                mIntent.putExtra("body", sb.toString());
                context.startService(mIntent);
            }
        } else {
            // На все остальные подписанные уведомления просто информируем текстом типа уведомления
            mIntent.putExtra("from", "internal");

            String extra = intent.getStringExtra("extra");
            if (extra == null) {
                extra = "";
            }
            mIntent.putExtra("body", action + extra);

            context.startService(mIntent);
        }
    }
}
