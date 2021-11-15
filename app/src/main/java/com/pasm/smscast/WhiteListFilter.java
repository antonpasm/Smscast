package com.pasm.smscast;

import java.util.ArrayList;
import java.util.List;


class WhiteListFilter {

    private final List<String> l;

    public WhiteListFilter(String settings) {
        l = new ArrayList<>();
        for(String s: settings.split("\n")) {
            s = s.trim();
            if (s.isEmpty()) continue;
            l.add(s);
        }
    }

    public boolean filter(String number, String contact) {
        if (l.size() == 0)
            return true;

        for(String s: l) {
            if (s.equalsIgnoreCase(number) || s.equalsIgnoreCase(contact))
                return true;
        }
        return false;
    }

}
