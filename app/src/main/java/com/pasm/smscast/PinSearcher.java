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
     * Правила для определения pin
     * <p>
     * непрерывная последовательность от 4 до 6 цифр
     * <p>
     * перед pin могут быть только символы ` -:;,!?`
     * перед pin не должно быть последовательности `* ` (**** 12345 - не pin)
     * перед pin не должно быть последовательности `цифра-` (0-12345 - не pin)
     * <p>
     * после pin могут быть только символы ` .,:;`
     * после pin не должно быть `.цифра` или `,цифра` (1234.5678 - не pin)
     * после pin не должно быть ` руб.` или ` р.` или ` ₽` (1234 руб. - не pin)
     *
     * @param message Обрабатываемая строка
     */
    public PinSearcher(String message) {
        this.message = message;

        pins = new ArrayList<>();
        Matcher m = Pattern.compile("(?<![^\\s\\-:;,!?]|\\*\\s|\\d-)(\\d{4,6})(?![^\\s.,:;]|[.,]\\d|\\s*руб\\.|\\s*р\\.|\\s*₽)").matcher(message);
        while (m.find()) {
            idx.add(m.start());
            idx.add(m.end());

            String pin = m.group();
            if (!pins.contains(pin))
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
