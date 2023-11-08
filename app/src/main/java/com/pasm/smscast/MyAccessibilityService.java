package com.pasm.smscast;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;


// Этот сервис используется как заглушка, для того чтобы что-то лучше работало... не помню.
//   На Xiaomi были какие-то проблемы
//   Вроде для того, чтобы можно было копировать PIN из уведомления.
//   На MIUI есть проблемы с доступом к буферу обмена из сервиса. В активной activity проблем вроде нет
//   толи приложение выгружалось из памяти, и это добавил, чтобы не выгружалось

public class MyAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

    }

    @Override
    public void onInterrupt() {

    }
}
