package com.pasm.smscast;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.widget.Toast;


public class CopyBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (context == null || intent == null)
            return;

        Bundle extras = intent.getExtras();
        if (extras != null) {
            String text = extras.getString("extra");
            if (text != null) {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("pin", text);
                clipboard.setPrimaryClip(clip);

                // Закрыть шторку уведомления т.к. автоматом не закрывается при нажатии на Action Button
                context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

                Toast.makeText(context, Html.fromHtml(String.format(context.getString(R.string.copied), text)), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
