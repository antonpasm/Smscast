package com.pasm.smscast;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Pair;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class MainActivity extends Activity {

    public static final String MyTestNotify = BuildConfig.APPLICATION_ID + ".TEST_NOTIFY";
    private final BroadcastReceiver MyTestReceiver = new SMSMonitor();
    private TextView tvPermissionNotify;
    private TextView tvNotificationNotify;
    private TextView tvBatteryOptimizationNotify;
    private TextView tvAccessibilityNotify;
    private Switch swSMSMonitor;
    private Switch swCallMonitor;
    private Switch swNotificationMonitor;
    private LinearLayout grNotificationMonitor;
    private CheckBox cbNotificationIgnoreOngoing;
    private SeekBar sbNotificationInfoLevel;
    private EditText etNotificationIgnoreList;
    private Switch swDoCopyPin;
    private Switch swDoNotify;
    private Switch swDoToast;
    private Switch swSendToTelegram;
    private LinearLayout grSendToTelegram;
    private TextView tvTelegramHelp;
    private EditText etBotToken;
    private EditText etChatID;
    private Switch swSendToServer;
    private LinearLayout grSendToServer;
    private EditText etURL;
    private EditText etHostPassword;
    private EditText etRegexpFilter;
    private EditText etWhiteList;
    private EditText etSenderPrefix;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Отобразить иконку на панели
        ActionBar ab = getActionBar();
        if (ab != null) ab.hide();
        tvPermissionNotify = findViewById(R.id.tvPermissionNotify);
        tvNotificationNotify = findViewById(R.id.tvNotificationNotify);
        tvBatteryOptimizationNotify = findViewById(R.id.tvBatteryOptimizationNotify);
        tvAccessibilityNotify = findViewById(R.id.tvAccessibilityNotify);
        swSMSMonitor = findViewById(R.id.swSMSMonitor);
        swCallMonitor = findViewById(R.id.swCallMonitor);
        swNotificationMonitor = findViewById(R.id.swNotificationMonitor);
        grNotificationMonitor = findViewById(R.id.grNotificationMonitor);
        cbNotificationIgnoreOngoing = findViewById(R.id.cbNotificationIgnoreOngoing);
        sbNotificationInfoLevel = findViewById(R.id.sbNotificationInfoLevel);
        etNotificationIgnoreList = findViewById(R.id.etNotificationIgnoreList);
        swDoCopyPin = findViewById(R.id.swDoCopyPin);
        swDoNotify = findViewById(R.id.swDoNotify);
        swDoToast = findViewById(R.id.swDoToast);
        swSendToTelegram = findViewById(R.id.swSendToTelegram);
        grSendToTelegram = findViewById(R.id.grSendToTelegram);
        tvTelegramHelp = findViewById(R.id.tvTelegramHelp);
        etBotToken = findViewById(R.id.etBotToken);
        etChatID = findViewById(R.id.etChatID);
        swSendToServer = findViewById(R.id.swSendToServer);
        grSendToServer = findViewById(R.id.grSendToServer);
        etURL = findViewById(R.id.etURL);
        etHostPassword = findViewById(R.id.etHostPassword);
        etRegexpFilter = findViewById(R.id.etRegexpFilter);
        etWhiteList = findViewById(R.id.etWhiteList);
        etSenderPrefix = findViewById(R.id.etSenderPrefix);

        // Показывать справку по Telegram, если поля будут пустые
        TextWatcher telegramField = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                telegramHelp();
            }
        };
        etBotToken.addTextChangedListener(telegramField);
        etChatID.addTextChangedListener(telegramField);

        // Добавить текст и сделать кликабельные ссылки
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
        tvTelegramHelp.setText(ss);
        tvTelegramHelp.setMovementMethod(LinkMovementMethod.getInstance());

        // Грузим сохранённые настройки
        MySettings.init(getApplicationContext());

        swSMSMonitor.setChecked(MySettings.SMSMonitor);
        swCallMonitor.setChecked(MySettings.CallMonitor);
        swNotificationMonitor.setChecked(MySettings.NotificationMonitor);
        cbNotificationIgnoreOngoing.setChecked(MySettings.NotificationIgnoreOngoing);
        sbNotificationInfoLevel.setProgress(MySettings.NotificationInfoLevel);
        etNotificationIgnoreList.setText(MySettings.NotificationMonitorIgnoreList);
        swDoCopyPin.setChecked(MySettings.DoCopyPin);
        swDoNotify.setChecked(MySettings.DoNotify);
        swDoToast.setChecked(MySettings.DoToast);
        swSendToTelegram.setChecked(MySettings.SendToTelegram);
        etBotToken.setText(MySettings.BotToken);
        etChatID.setText(MySettings.ChatID);
        swSendToServer.setChecked(MySettings.SendToServer);
        etURL.setText(MySettings.URL);
        etHostPassword.setText(MySettings.HostPassword);
        etRegexpFilter.setText(MySettings.RegexpFilterList);
        etWhiteList.setText(MySettings.WhiteList);
        etSenderPrefix.setText(MySettings.SenderPrefix);

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
    }

    private void telegramHelp() {
        // Если уже настроено, то спрятать
        tvTelegramHelp.setVisibility(
                MySettings.BotToken.isEmpty() || MySettings.ChatID.isEmpty() ||
                etBotToken.getText().toString().isEmpty() || etChatID.getText().toString().isEmpty()  ? View.VISIBLE : View.GONE
        );
    }

    /**
     * Попытаемся определить chat ID
     */
    private void getChatID() {
        String token = etBotToken.getText().toString();
        Context context = this;

        TelegramUtils.getChatID(context, token, new TelegramUtils.ChatIDResult() {
            @Override
            public void result(int result) {
                etChatID.setText(String.valueOf(result));
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
        OnSwitchNotificationMonitor(null);
        checkPermissions();
        checkNotificationAccess();
        checkBatteryOptimization();
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
        MySettings.SMSMonitor = swSMSMonitor.isChecked();
        MySettings.CallMonitor = swCallMonitor.isChecked();
        MySettings.NotificationMonitor = swNotificationMonitor.isChecked();
        MySettings.NotificationIgnoreOngoing = cbNotificationIgnoreOngoing.isChecked();
        MySettings.NotificationInfoLevel = sbNotificationInfoLevel.getProgress();
        MySettings.NotificationMonitorIgnoreList = etNotificationIgnoreList.getText().toString();
        MySettings.DoCopyPin = swDoCopyPin.isChecked();
        MySettings.DoNotify = swDoNotify.isChecked();
        MySettings.DoToast = swDoToast.isChecked();
        MySettings.SendToTelegram = swSendToTelegram.isChecked();
        MySettings.BotToken = etBotToken.getText().toString();
        MySettings.ChatID = etChatID.getText().toString();
        MySettings.SendToServer = swSendToServer.isChecked();
        MySettings.URL = etURL.getText().toString();
        MySettings.HostPassword = etHostPassword.getText().toString();
        MySettings.RegexpFilterList = etRegexpFilter.getText().toString();
        MySettings.WhiteList = etWhiteList.getText().toString();
        MySettings.SenderPrefix = etSenderPrefix.getText().toString();
        MySettings.store();
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();

        telegramHelp();
        checkPermissions();
        checkNotificationAccess();
    }

    /**
     * Сформировать тестовое сообщение и проверить рассылку
     */
    @SuppressWarnings("unused")
    public void OnTestClick(View view) {
        Intent intent = new Intent(MyTestNotify);
        // Добавить что-то типа PIN
        long pin = System.currentTimeMillis() % 10000;
        intent.putExtra("extra", String.format(Locale.US, "\ntest: %04d %04d.", pin, pin ^ 1807));
        sendBroadcast(intent);
    }

    @SuppressWarnings("unused")
    public void OnSwitchSMSMonitor(View view) {
        checkPermissions();
    }

    @SuppressWarnings("unused")
    public void OnSwitchCallMonitor(View view) {
        checkPermissions();
    }

    @SuppressWarnings("unused")
    public void OnSwitchNotificationMonitor(View view) {
        grNotificationMonitor.setVisibility(swNotificationMonitor.isChecked() ? View.VISIBLE : View.GONE);
        checkNotificationAccess();
    }

    @SuppressWarnings("unused")
    public void OnSwitchSendToTelegram(View view) {
        grSendToTelegram.setVisibility(swSendToTelegram.isChecked() ? View.VISIBLE : View.GONE);
    }

    @SuppressWarnings("unused")
    public void OnSwitchSendToServer(View view) {
        grSendToServer.setVisibility(swSendToServer.isChecked() ? View.VISIBLE : View.GONE);
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
     * Открыть настройки "Доступ к уведомлениям", чтобы юзер мог включить доступ
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    @SuppressWarnings("unused")
    public void onNotificationNotifyClick(View view) {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    /**
     * Открыть настройки "Экономия заряда батареи", чтобы юзер мог переключить на "Не экономить"
     */
    @SuppressWarnings("unused")
    public void onBatteryOptimizationNotifyClick(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
        }

//        boolean requestAvailable = false;
//        boolean isIgnoring = false;
//        String packageName = getPackageName();
//        // Проверить есть ли у меня разрешение на запуск стандартного диалога разрешения на игнорирование оптимизации батареи
//        try {
//            PackageInfo pi = getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
//            String[] permissions = pi.requestedPermissions;
//            requestAvailable = permissions != null && Arrays.asList(permissions).contains("android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS");
//        } catch (PackageManager.NameNotFoundException e) {
//        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
//            isIgnoring = pm.isIgnoringBatteryOptimizations(packageName);
//        }
//        Intent intent = new Intent();
//        if (requestAvailable && !isIgnoring) {
//            // Вызвать стандартный диалог
//            // Для работы этого диалога, у приложения в манифесте должно быть прописано
//            // разрешение android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
//            // которое не любит GooglePlay. Пока туда не лезем, можно игнорировать...
//            // Если разрешения не прописано, то startActivity будет проигнорировано
//            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
//            intent.setData(Uri.parse("package:" + packageName));
//        } else {
//            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
//        }
//        startActivity(intent);
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
            final Map<String, Pair<Integer, Boolean>> p = new LinkedHashMap<>();
            p.put(Manifest.permission.RECEIVE_SMS, new Pair<>(R.string.notify_permission_RECEIVE_SMS, swSMSMonitor.isChecked()));
            p.put(Manifest.permission.READ_PHONE_STATE, new Pair<>(R.string.notify_permission_PHONE_STATE, swCallMonitor.isChecked()));
            p.put(Manifest.permission.READ_CALL_LOG, new Pair<>(R.string.notify_permission_CALL_LOG, swCallMonitor.isChecked()));
            p.put(Manifest.permission.READ_CONTACTS, new Pair<>(R.string.notify_permission_READ_CONTACTS, swSMSMonitor.isChecked() || swCallMonitor.isChecked()));
            p.put(Manifest.permission.INTERNET, new Pair<>(R.string.notify_permission_INTERNET, swSendToTelegram.isChecked() || swSendToServer.isChecked()));
            p.put(Manifest.permission.RECEIVE_BOOT_COMPLETED, new Pair<>(R.string.notify_permission_BOOT_COMPLETED, true));
            for (Map.Entry<String, Pair<Integer, Boolean>> pdc : p.entrySet()) {
                if (pdc.getValue().second && checkSelfPermission(pdc.getKey()) != PackageManager.PERMISSION_GRANTED) {
                    notify.add("* " + getString(pdc.getValue().first));
                    if (shouldShowRequestPermissionRationale(pdc.getKey())) {
                        missing.add(pdc.getKey());
                    }
                }
            }
        }

        tvPermissionNotify.setText(TextUtils.join("\n", notify));
        tvPermissionNotify.setVisibility(notify.size() > 0 ? View.VISIBLE : View.GONE);

        if (missing.size() > 0) {
            //noinspection ConstantConditions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(missing.toArray(new String[0]), 0);
            }
        }
    }

    /**
     * Проверим состояние Notification Access.
     */
    private void checkNotificationAccess() {
        boolean show = swNotificationMonitor.isChecked();
        if (show) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            final boolean isEnabled;
            ComponentName cn = new ComponentName(this, NotificationMonitor.class.getName());
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                isEnabled = nm.isNotificationListenerAccessGranted(cn);
            } else {
                ContentResolver cr = getContentResolver();
                String listeners = Settings.Secure.getString(cr, "enabled_notification_listeners");
                isEnabled = listeners != null && listeners.contains(cn.flattenToString());
            }

            if (isEnabled) {
                tvNotificationNotify.setText(R.string.notify_notification_on);
                if (MySettings.NotificationInfoLevel == 0)
                    show = false;
            } else {
                tvNotificationNotify.setText(R.string.notify_notification_off);
            }
        }
        tvNotificationNotify.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Проверим состояние IgnoringBatteryOptimization.
     */
    private void checkBatteryOptimization() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            boolean isIgnoring = pm.isIgnoringBatteryOptimizations(getPackageName());
            if (isIgnoring) {
                tvBatteryOptimizationNotify.setText("* Ок. Игнорируем оптимизацию батареи");
            } else {
                tvBatteryOptimizationNotify.setText("* Приложение может быть закрыто оптимизатором батареи");
            }
        } else {
            tvNotificationNotify.setVisibility(View.GONE);
        }
    }

    /**
     * Проверим состояние Accessibility Service.
     * Это где-нибудь нужно, кроме как на MIUI? Да и на MIUI есть более правильное решение?
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
        if (isEnabled) {
            tvAccessibilityNotify.setText(R.string.notify_accessibility_on);
        } else {
            tvAccessibilityNotify.setText(R.string.notify_accessibility_off);
        }
        // tv.setVisibility(!isEnabled ? View.VISIBLE : View.GONE);
    }

}
