package com.pasm.smscast;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PinSearcher {

    public final List<String> pins;
    public final String message;

    private final ArrayList<Integer> idx = new ArrayList<>();

    public interface Decorator {
        String pin(String pin);
        String text(String text);
    }

    /**
     * @param message Обрабатываемая строка
     */
    public PinSearcher(String message) {
        this.message = message;

        pins = new ArrayList<>();
        Matcher m = Pattern.compile("(?<![^\\s\\-:;,!?]|\\*\\s)(\\d{4,6})(?![^\\s.,:;]|[.,]\\d)").matcher(message);
        while (m.find()) {
            idx.add(m.start());
            idx.add(m.end());

            String pin = m.group();
            pins.add(pin);
        }
    }

    public String decorate(Decorator decorator) {
        StringBuilder sb = new StringBuilder();
        int from = 0;
        for(int i = 0; i < idx.size(); i += 2) {
            int b = idx.get(i);
            int e = idx.get(i+1);
            sb.append(decorator.text(message.substring(from, b)));
            sb.append(decorator.pin(message.substring(b, e)));
            from = e;
        }
        sb.append(decorator.text(message.substring(from)));
        return sb.toString();
    }

}
