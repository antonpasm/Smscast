package com.pasm.smscast;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Pair;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class MainActivity extends Activity {

    public static final String MyTestNotify = BuildConfig.APPLICATION_ID + ".TEST_NOTIFY";
    private final BroadcastReceiver MyTestReceiver = new SMSMonitor();

    private EditText edBotToken;
    private EditText edChatID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Отобразить иконку на панели
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.hide();
//            ab.setDisplayShowHomeEnabled(true);
        }

        edBotToken = findViewById(R.id.etBotToken);
        edChatID = findViewById(R.id.etChatID);

        // Добавить текст и сделать кликабельные ссылки
        TextView tv = findViewById(R.id.tvTelegramHelp);
        String msg = getString(R.string.telegram_help) + getString(R.string.apply);
        Spanned text = Html.fromHtml(msg);
        SpannableString ss = new SpannableString(text);
        URLSpan[] currentSpans = text.getSpans(0, text.length(), URLSpan.class);
        for (URLSpan urlspan : currentSpans) {
            if (urlspan.getURL().isEmpty()) {
                // Подменить URLSpan на мой ClickableSpan
                ss.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        getChatID();
                    }
                }, text.getSpanStart(urlspan), text.getSpanEnd(urlspan), text.getSpanFlags(urlspan));
                ss.removeSpan(urlspan);
            }
        }
        tv.setText(ss);
        tv.setMovementMethod(LinkMovementMethod.getInstance());

        // Грузим сохранённые настройки
        MySettings.restore(getSharedPreferences(getResources().getString(R.string.app_name), MODE_PRIVATE));

        edBotToken.setText(MySettings.BotToken);
        edChatID.setText(MySettings.ChatID);
        ((EditText) findViewById(R.id.etURL)).setText(MySettings.URL);
        ((EditText) findViewById(R.id.etHostPassword)).setText(MySettings.HostPassword);
        ((EditText) findViewById(R.id.etRegexpFilter)).setText(MySettings.RegexpFilterList);
        ((EditText) findViewById(R.id.etWhiteList)).setText(MySettings.WhiteList);
        ((Switch) findViewById(R.id.swDoNotify)).setChecked(MySettings.DoNotify);
        ((Switch) findViewById(R.id.swDoCopyPin)).setChecked(MySettings.DoCopyPin);
        ((Switch) findViewById(R.id.swMissedCall)).setChecked(MySettings.MissedCall);
        ((Switch) findViewById(R.id.swDoToast)).setChecked(MySettings.DoToast);
        ((Switch) findViewById(R.id.swSendToTelegram)).setChecked(MySettings.SendToTelegram);
        ((Switch) findViewById(R.id.swSendToServer)).setChecked(MySettings.SendToServer);
        ((EditText) findViewById(R.id.etSenderPrefix)).setText(MySettings.SenderPrefix);


        // Долгий тап на btnTest скопирует logcat в буфер
        findViewById(R.id.btnTest).setOnLongClickListener(v -> {
            StringBuilder log = new StringBuilder();
            try {
                Process process = Runtime.getRuntime().exec("logcat -d");
                InputStream stdout = process.getInputStream();
                byte[] buf = new byte[8192];
                int len;
                do {
                    len = stdout.read(buf, 0, buf.length);
                    if (len == -1)
                        break;
                    log.append(new String(buf, 0, len));
                } while (true);
            } catch (IOException ignore) {
            }

            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("log", log.toString());
            clipboard.setPrimaryClip(clip);

            Toast.makeText(getApplicationContext(), R.string.logcat_copied, Toast.LENGTH_SHORT).show();
            return true;
        });


        telegramHelp();
    }

    private void telegramHelp() {
        // Если уже настроено, то спрятать
        findViewById(R.id.tvTelegramHelp).setVisibility(MySettings.BotToken.isEmpty() || MySettings.ChatID.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /**
     * Попытаемся определить chat ID
     */
    private void getChatID() {
        String token = edBotToken.getText().toString();
        Context context = this;

        TelegramUtils.getChatID(context, token, new TelegramUtils.ChatIDResult() {
            @Override
            public void result(int result) {
                edChatID.setText(String.valueOf(result));
            }

            @Override
            public void error(CharSequence message) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(MyTestReceiver, new IntentFilter(MyTestNotify));
        OnSwitchSendToTelegram(null);
        OnSwitchSendToServer(null);
        checkPermissions();
        checkAccessibilityService();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(MyTestReceiver);
        super.onPause();
    }

    /**
     * Сохранить настройки
     */
    public void OnApplyClick(@SuppressWarnings("unused") View view) {
        MySettings.BotToken = edBotToken.getText().toString();
        MySettings.ChatID = edChatID.getText().toString();
        MySettings.URL = ((EditText) findViewById(R.id.etURL)).getText().toString();
        MySettings.HostPassword = ((EditText) findViewById(R.id.etHostPassword)).getText().toString();
        MySettings.RegexpFilterList = ((EditText) findViewById(R.id.etRegexpFilter)).getText().toString();
        MySettings.WhiteList = ((EditText) findViewById(R.id.etWhiteList)).getText().toString();
        MySettings.DoNotify = ((Switch) findViewById(R.id.swDoNotify)).isChecked();
        MySettings.DoCopyPin = ((Switch) findViewById(R.id.swDoCopyPin)).isChecked();
        MySettings.MissedCall = ((Switch) findViewById(R.id.swMissedCall)).isChecked();
        MySettings.DoToast = ((Switch) findViewById(R.id.swDoToast)).isChecked();
        MySettings.SendToTelegram = ((Switch) findViewById(R.id.swSendToTelegram)).isChecked();
        MySettings.SendToServer = ((Switch) findViewById(R.id.swSendToServer)).isChecked();
        MySettings.SenderPrefix = ((EditText) findViewById(R.id.etSenderPrefix)).getText().toString();
        MySettings.store();

        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();

        telegramHelp();
        checkPermissions();
    }

    /**
     * Сформировать тестовое сообщение и проверить рассылку
     */
    @SuppressWarnings("unused")
    public void OnTestClick(View view) {
        Intent intent = new Intent(MyTestNotify);
        // Добавить чтото типа PIN
        long pin = System.currentTimeMillis() % 10000;
        intent.putExtra("extra", String.format(Locale.US, " millis: %04d %04d.", pin, pin ^ 1807));
        sendBroadcast(intent);
    }

    @SuppressWarnings("unused")
    public void OnSwitchSendToTelegram(View view) {
        findViewById(R.id.groupSendToTelegram).setVisibility(((Switch) findViewById(R.id.swSendToTelegram)).isChecked() ? View.VISIBLE : View.GONE);
    }

    @SuppressWarnings("unused")
    public void OnSwitchSendToServer(View view) {
        findViewById(R.id.groupSendToServer).setVisibility(((Switch) findViewById(R.id.swSendToServer)).isChecked() ? View.VISIBLE : View.GONE);
    }

    /**
     * Открыть настройки приложения, чтобы было удобно включить отключить разрешения
     */
    @SuppressWarnings("unused")
    public void onPermissionNotifyClick(View view) {
        Intent intent = new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    /**
     * Открыть настройки "Спец. возможности", чтобы юзер мог включить мой Accessibility Service
     */
    @SuppressWarnings("unused")
    public void onAccessibilityNotifyClick(View view) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    /**
     * Проверим какие разрешения даны, в соответствии с настройками программы и отобразим панельку, если требуются доп. разрешения
     */
    private void checkPermissions() {
        List<String> notify = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final Map<String, Pair<Integer, Boolean>> pdcs = new HashMap<>();
            pdcs.put(Manifest.permission.RECEIVE_SMS, new Pair<>(R.string.notify_permission_RECEIVE_SMS, true));
            pdcs.put(Manifest.permission.READ_PHONE_STATE, new Pair<>(R.string.notify_permission_PHONE_STATE, MySettings.MissedCall));
            pdcs.put(Manifest.permission.READ_CALL_LOG, new Pair<>(R.string.notify_permission_CALL_LOG, MySettings.MissedCall));
            pdcs.put(Manifest.permission.READ_CONTACTS, new Pair<>(R.string.notify_permission_READ_CONTACTS, true));
            pdcs.put(Manifest.permission.INTERNET, new Pair<>(R.string.notify_permission_INTERNET, MySettings.SendToTelegram || MySettings.SendToServer));
            pdcs.put(Manifest.permission.RECEIVE_BOOT_COMPLETED, new Pair<>(R.string.notify_permission_BOOT_COMPLETED, true));
            for (Map.Entry<String, Pair<Integer, Boolean>> pdc : pdcs.entrySet()) {
                if (pdc.getValue().second && checkSelfPermission(pdc.getKey()) != PackageManager.PERMISSION_GRANTED) {
                    notify.add("* " + getString(pdc.getValue().first));
                    if (shouldShowRequestPermissionRationale(pdc.getKey())) {
                        missing.add(pdc.getKey());
                    }
                }
            }
        }

        TextView tv = findViewById(R.id.tvPermissionNotify);
        tv.setText(TextUtils.join("\n", notify));
        tv.setVisibility(notify.size() > 0 ? View.VISIBLE : View.GONE);

        if (missing.size() > 0) {
            //noinspection ConstantConditions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(missing.toArray(new String[0]), 0);
            }
        }
    }

    /**
     * Проверим состояние Accessibility Service.
     * Это гденить нужно кроме как на MIUI? Да и на MIUI есть более правильное решение?
     */
    private void checkAccessibilityService() {
        final String accessibilityServiceName = getPackageName() + "/." + MyAccessibilityService.class.getSimpleName();
        AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        boolean isEnabled = false;
        for (AccessibilityServiceInfo id : am.getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK)) {
            isEnabled = accessibilityServiceName.equals(id.getId());
            if (isEnabled)
                break;
        }
        TextView tv = findViewById(R.id.tvAccessibilityNotify);
        if (isEnabled) {
            tv.setText(R.string.notify_accessibility_on);
        } else {
            tv.setText(R.string.notify_accessibility_off);
        }
        // tv.setVisibility(!isEnabled ? View.VISIBLE : View.GONE);
    }

}
