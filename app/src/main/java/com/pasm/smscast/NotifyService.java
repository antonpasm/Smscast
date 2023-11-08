package com.pasm.smscast;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.text.Html;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;


public class NotifyService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {

        MySettings.init(getApplicationContext());

        if (intent != null) {
            Long time = intent.getLongExtra("time", 0);
            String from = intent.getStringExtra("from");
            if (from == null) {
                from = getString(R.string.unknownNumber);
            } else {
                // Например, при запросе баланса МТС приходит сообщение от номера "Balance\r"
                from = from.trim();
            }
            String body = intent.getStringExtra("body");
            if (body == null) {
                body = "empty";
            } else {
                body = body.trim();
            }

            // Попытаемся достать имя контакта из контактов телефона
            String contact = contactName(from); // null if not in contact list

            // Фильтр по белому списку
            WhiteListFilter whiteListFilter = new WhiteListFilter(MySettings.WhiteList);
            if (whiteListFilter.filter(from, contact)) {

                String senderPrefix = MySettings.SenderPrefix;

                // Сформировать строку "От кого"
                String fromContact = (contact == null) ? String.format("%s%s", senderPrefix, from) : String.format("%s%s (%s)", senderPrefix, contact, from);

                // Отфильтровать регулярками. (Убрать рекламу)
                RegexpFilter filter = new RegexpFilter(MySettings.RegexpFilterList);
                body = filter.filter(from, contact, body);
                if (filter.lastError != null) {
                    // Хоть как-то уведомить юзера об ошибках в фильтрах
                    if (MySettings.SendToTelegram)
                        sendToTelegram(
                                time,
                                "<Regexp Filter error>",
                                String.format("<code>%s</code>", TelegramUtils.escapeHtml(filter.lastError))
                        );
                    if (MySettings.SendToServer)
                        sendToServer(
                                time,
                                "<Regexp Filter error>",
                                filter.lastError);
                }

                // Достать PIN коды из SMS
                PinSearcher pinSearcher = new PinSearcher(body);

                // Экранировать текст
                String fromContactEsc = Html.escapeHtml(fromContact);
                String bodyEsc = pinSearcher.decorate(new PinSearcher.Decorator() {
                    @Override
                    public String pin(String pin) {
                        return String.format("<u>%s</u>", pin);
                    }

                    @Override
                    public String text(String text) {
                        // escapeHtml не заменяет \n на <br>
                        return Html.escapeHtml(text).replace("&#10;", "<br>");
                    }
                });

                // Скопировать PIN в буфер, если нужно
                if (MySettings.DoCopyPin && !pinSearcher.pins.isEmpty()) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("pin", pinSearcher.pins.get(0));
                    clipboard.setPrimaryClip(clip);
                }

                // Показать уведомление, если нужно
                if (MySettings.DoNotify)
                    showNotification(fromContactEsc, bodyEsc, pinSearcher.pins);

                // Показать тост, если нужно
                if (MySettings.DoToast)
                    showToast(fromContactEsc, bodyEsc);

                // Отправить в telegram, если нужно
                if (MySettings.SendToTelegram)
                    sendToTelegram(
                            time,
                            fromContact,
                            pinSearcher.decorate(new PinSearcher.Decorator() {
                                @Override
                                public String pin(String pin) {
                                    // 'code' tag allow copy text
                                    return String.format("<code>%s</code>", pin);
                                }

                                @Override
                                public String text(String text) {
                                    return TelegramUtils.escapeHtml(text);
                                }
                            })
                    );

                // Отправить на сервер, если нужно
                if (MySettings.SendToServer)
                    sendToServer(time, fromContact, body);

            }
        }
        return START_NOT_STICKY;
    }

    /**
     * Попытаемся достать имя контакта из контактов телефона
     *
     * @param from номер телефона
     * @return имя контакта или null, если не нашли номер в контактах
     */
    private String contactName(String from) {
        if ("".equals(from)) {
            return null;
        }

        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(from));

        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};
        String contact = null;
        try {
            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    contact = cursor.getString(0);
                }
                cursor.close();
            }
        } catch (SecurityException e) {
            Log.e(String.valueOf(getPackageName()), "exception in contactName. " + e.getMessage());
        }
        return contact;
    }

    private String formatDateTime(Long time) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd.MM.yyyy", Locale.getDefault());
        return dateFormat.format(time);
    }


    /**
     * Отправить сообщение в Telegram
     * @param time Временная метка
     * @param from Текст "от кого"
     * @param body Текст сообщения. Допустимы теги форматирования telegram. Остальной текст должен быть экранирован ф-ией TelegramUtils.escapeHtml(String)
     */
    private void sendToTelegram(Long time, String from, String body) {
        String message = String.format("<code>%s</code>\n<b>%s</b>\n%s", formatDateTime(time), TelegramUtils.escapeHtml(from), body);
        TelegramUtils.sendToTelegramHtml(
                this,
                MySettings.BotToken,
                MySettings.ChatID,
                message
        );
    }


    private void sendToServer(Long time, String from, String body) {
        if (MySettings.URL.isEmpty())
            return;
        // Первые 16 байт (при размере блока 128 бит) шифруемой строки нужно сделать по возможности случайным, чтобы начало криптограммы не имела повторяющегося значения
        String message = MyCipher.encode(this, MySettings.HostPassword, String.format("%s\r\n%s\r\n%s", time.toString(), from, body));
        MyHttp.httpGET(
                this,
                MySettings.URL + message,
                response -> {
//                    Log.i(getPackageName(), "Sent to server probably successfully. " + response);
                },
                error -> MyHttp.logError(error, "Failed to send.", MySettings.URL, this)
        );
    }

    /**
     * Android уведомление с кнопками копирования pin
     * @param from - Отправитель. Текст должен быть экранирован Http.escapeHtml
     * @param text - Сообщение. Текст должен быть экранирован Http.escapeHtml
     * @param pins - Список pin
     */
    private void showNotification(String from, String text, List<String> pins) {
//        PendingIntent activityIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        CharSequence title, message;
        title = Html.fromHtml(from);
        message = Html.fromHtml(text);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        int NOTIFY_ID = 1;
        String CHANNEL_ID = "smscast_channel";
        String channelName = getString(R.string.notify_channel_name);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(false);
            channel.enableVibration(false);
            channel.enableLights(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            notificationManager.createNotificationChannel(channel);

            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder
                .setSmallIcon(R.drawable.ic_small)
                .setContentTitle(title)
                .setContentText(message)
                //.setPriority(Notification.PRIORITY_MIN)
                .setPriority(Notification.PRIORITY_DEFAULT)
                // todo: Запустить стандартный просмоторщик смс... как? это вообще возможно?
//                .setContentIntent(activityIntent)    // событие на нажатие на уведомление
                .setShowWhen(true).setWhen(System.currentTimeMillis())      // Включить отображение временной метки
                .setStyle(new Notification.BigTextStyle().bigText(message))    // Возможность развернуть Notify и увидеть весь текст
                .setAutoCancel(false);

        // Добавить Action на копирование найденных pin. Не более 3-х
        for (int i = 0; i < 3 && i < pins.size(); i++) {
            Intent intent = new Intent(this, CopyBroadcastReceiver.class);
            intent.putExtra("extra", pins.get(i));
            int flags;

            flags = PendingIntent.FLAG_CANCEL_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pIntent = PendingIntent.getBroadcast(this, i, intent, flags);
            builder.addAction(new Notification.Action(R.drawable.ic_copy, pins.get(i), pIntent));   // иконки на кнопках были убраны начиная с Nougat
        }

        notificationManager.notify(NOTIFY_ID, builder.build());
    }


    /**
     * Тост уведомление
     * @param from - Отправитель. Текст должен быть экранирован Http.escapeHtml
     * @param text - Сообщение. Текст должен быть экранирован Http.escapeHtml
     */
    private void showToast(String from, String text) {
        CharSequence message;
        message = Html.fromHtml(String.format("<b>%s</b><br>%s", from, text));
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }


//    private void sendSMS(Long time, String from, String body) {
//
//        //     <uses-permission android:name="android.permission.SEND_SMS" />
//
//        String phoneNumber = "0123456789";
//        try {
////            String message = "Hello World! Now we are going to demonstrate " +
////                    "how to send a message with more than 160 characters from your Android application.";
////            SmsManager smsManager = SmsManager.getDefault();
////            ArrayList<String> parts = smsManager.divideMessage(message);
////            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
//
//            String message = String.format("%s\n%s\n%s", time.toString(), from, body);
//            SmsManager smsManager = SmsManager.getDefault();
//            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }


}
