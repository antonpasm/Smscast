package com.pasm.smscast;

import android.text.TextUtils;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class RegexpFilter {

    private final List<Pair<String, String>> l;

    public String lastError;

    public RegexpFilter(String settings) {
        l = new ArrayList<>();
        String[] s = settings.split("\n");
        for(int i = 0; i < (s.length & -2); i += 2) {
            l.add(new Pair<>(s[i], s[i + 1]));
        }
    }

    public String filter(String number, String contact, String body) {
        lastError = null;
        List<String> errors = new ArrayList<>();
        for(Pair<String, String> p: l) {
            try {
                if (number.matches(p.first) ||
                    (contact != null && contact.matches(p.first))) {

                    Matcher m = Pattern.compile(p.second, Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(body);
                    if (m.groupCount() > 0) {
                        // Если есть группы, то их вырезаем
                        StringBuilder sb = new StringBuilder();
                        int from = 0;
                        while (m.find()) {
                            for (int g = 1; g <= m.groupCount(); ++g) {
                                sb.append(body.substring(from, m.start(g)));
                                from = m.end(g);
                            }
                        }
                        sb.append(body.substring(from));
                        body = sb.toString();
                    } else {
                        // Иначе вся строка и есть шаблон
                        body = m.replaceAll("");
                    }
                }
            } catch (PatternSyntaxException e) {
                errors.add(e.getMessage());
            }
        }
        if (errors.size() > 0) {
            lastError = TextUtils.join("\n", errors);
        }
        return body;
    }

}
