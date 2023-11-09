package com.github.wnameless.spring.bulkapi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Regexs {
    public static final Pattern REGEX_SP_CH = Pattern.compile("[\\\\\\[\\.\\[\\]\\{\\}\\(\\)\\*\\+\\-\\?\\^\\$\\]\\|]");

    private Regexs() {
    }

    public static String escapeSpecialCharacters(String input, Pattern... excludedPatterns) {
        List<Matcher> matchers = patterns2Matchers(excludedPatterns, input);
        initMatchers(matchers);
        Matcher m = REGEX_SP_CH.matcher(input);
        StringBuffer sb = new StringBuffer();

        while(m.find()) {
            if (escapableCharacter(m.start(), matchers)) {
                m.appendReplacement(sb, "\\\\" + input.charAt(m.start()));
            }
        }

        m.appendTail(sb);
        return sb.toString();
    }

    private static boolean escapableCharacter(int chIdx, List<Matcher> matchers) {
        boolean advancing = false;

        do {
            Iterator var3 = matchers.iterator();

            while(var3.hasNext()) {
                Matcher m = (Matcher)var3.next();
                if (!m.hitEnd() && m.end() - 1 < chIdx && m.find()) {
                    advancing = true;
                }

                try {
                    if (chIdx >= m.start() && chIdx <= m.end() - 1) {
                        return false;
                    }
                } catch (IllegalStateException var6) {
                }
            }
        } while(advancing);

        return true;
    }

    private static void initMatchers(List<Matcher> matchers) {
        Iterator var1 = matchers.iterator();

        while(var1.hasNext()) {
            Matcher m = (Matcher)var1.next();
            m.find();
        }

    }

    private static List<Matcher> patterns2Matchers(Pattern[] patterns, String input) {
        List<Matcher> matchers = new ArrayList();
        Pattern[] var3 = patterns;
        int var4 = patterns.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            Pattern p = var3[var5];
            matchers.add(p.matcher(input));
        }

        return matchers;
    }
}
