package com.pasm.smscast;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class MyCipher {

    public static String encode(Context context, String key, String message) {
        try {
            byte[] encodedBytes;
            if (key.isEmpty()) {
                encodedBytes = message.getBytes(StandardCharsets.UTF_8);
            } else {
                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                digest.update(key.getBytes());
                byte[] hashKey = Arrays.copyOf(digest.digest(), 16);
                digest.update(hashKey);
                byte[] iv = Arrays.copyOf(digest.digest(), 16);

                Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
                SecretKeySpec sks = new SecretKeySpec(hashKey, "AES");
                c.init(Cipher.ENCRYPT_MODE, sks, new IvParameterSpec(iv));
                encodedBytes = c.doFinal(message.getBytes());
            }
            return Base64.encodeToString(encodedBytes, Base64.NO_WRAP | Base64.URL_SAFE);
        } catch (Exception e) {
            Log.e(String.valueOf(context.getPackageName()), "exception in Encode. " + e.getMessage());
        }
        return "";
    }
}
