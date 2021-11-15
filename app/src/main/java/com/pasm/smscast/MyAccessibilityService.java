package com.pasm.smscast;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;


// Этот сервис используется как заглушка, для того чтобы чтото лучше работало..
// не помню. на Xiaomi были какието проблемы
// Вроде для того, чтобы можно было копировать PIN из уведомления.
// На MIUI есть проблемы с доступом к буферу обмена из чсервиса. В активной activity проблем вроде нет
// толи приложение выгоружалось из памяти, и это добавил, чтобы не выгружалось

public class MyAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

    }

    @Override
    public void onInterrupt() {

    }
}
