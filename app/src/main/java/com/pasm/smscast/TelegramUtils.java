package com.pasm.smscast;

import android.content.Context;
import android.util.Log;

import com.android.volley.NetworkResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


class TelegramUtils {
    public static final String telegramBotAPI = "https://api.telegram.org/bot";

    // alt link tg://resolve?domain=BotFather
    // public static final String linkBotFather = "https://t.me/BotFather";

    public static final String method_getUpdates = "/getUpdates";
    public static final String method_sendMessage = "/sendMessage";


    /**
     * Экранировать текст для Telegram
     * escapeHtml не подходит, т.к. например заменяет множество пробеловл на <tt>&amp;nbsp;</tt> и переводы строки, что для телеграма нормально
     * @param text что экранируем
     * @return заэкранировали
     */
    public static String escapeHtml(String text) {
        // первым заменить "&", потом остальные, т.к. дальше используются заменённые символы
        text = text.replaceAll("&", "&amp;");
        text = text.replaceAll("<", "&lt;");
        text = text.replaceAll(">", "&gt;");
        return text;
    }


    /**
     * Отправить сообщение в Telegram
     * @param BotToken Токен бота
     * @param ChatID ID чата
     * @param message Текст сообщения. Допустимы теги форматироания telegram. Остальной текст должен быть экранирован ф-ией {@link #escapeHtml(String)}
     */
    public static void sendToTelegramHtml(Context context, String BotToken, String ChatID, String message) {

        if (BotToken == null || BotToken.isEmpty() || ChatID == null || ChatID.isEmpty())
            return;

        String url = telegramBotAPI + BotToken + method_sendMessage;

        Map<String, String> params = new HashMap<>();
        params.put("chat_id", ChatID);
        params.put("parse_mode", "html");
        params.put("text", message);
        MyHttp.httpPOST(
                context,
                url,
                params,
                response -> {
//                    Log.i(getPackageName(), "Sent to telegram probably successfully. " + response);
                },
                error -> MyHttp.logError(error, "Failed to send to telegram.", url, context)
        );
    }


    /**
     * Попытаемся определить chat ID
     */
    public interface ChatIDResult {
        void result(int result);
        void error(CharSequence message);
    }

    public static void getChatID(Context context, String BotToken, ChatIDResult cb) {
        if (BotToken.isEmpty()) {
            cb.error(context.getString(R.string.telegram_error_need_token));
            return;
        }
        String url = telegramBotAPI + BotToken + method_getUpdates;
        MyHttp.httpGET(
                context,
                url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.getBoolean("ok")) {
                            JSONArray arr = json.getJSONArray("result");
                            int id = arr.getJSONObject(arr.length() - 1).getJSONObject("message").getJSONObject("chat").getInt("id");
                            cb.result(id);
                        }
                    } catch (JSONException e) {
                        Log.e(context.getPackageName(), "getChatID: " + response);
                        cb.error(context.getString(R.string.telegram_error_no_id));
                    }
                },
                error -> {
                    MyHttp.logError(error, null, url, context);
                    CharSequence Message;
                    NetworkResponse response = error.networkResponse;
                    if (response != null) {
                        String data = new String(response.data);
                        try {
                            JSONObject json = new JSONObject(data);
                            Message = json.getString("description");
                        } catch (JSONException e) {
                            Message = data;
                        }

                    } else {
                        Message = "" + error;
                    }
                    cb.error(Message);
                }
        );
    }


}
