package io.github.yasasbanukaofficial;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer {
    private static final Pattern WORD_PATTERN =
            Pattern.compile("[A-Z]?[a-z]+|[A-Z]+(?![a-z])|\\d+|[a-z]+");

    public static List<String> tokenize(String name) {
        List<String> tokens = new ArrayList<>();
        if (name == null || name.isEmpty()) return tokens;

        String normalized = name.replaceAll("[-_]", " ");
        String[] parts = normalized.split("\\s+");

        for (String part : parts) {
            Matcher matcher = WORD_PATTERN.matcher(part);
            while (matcher.find()) tokens.add(matcher.group().toLowerCase());
        }

        return tokens;
    }
}
