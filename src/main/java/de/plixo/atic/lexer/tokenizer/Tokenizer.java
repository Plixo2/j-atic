package de.plixo.atic.lexer.tokenizer;

import de.plixo.atic.exceptions.FailedTokenCaptureException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class Tokenizer {
    public static <T> List<TokenRecord<T>> apply(String text, T[] tokens,
                                                                 BiFunction<T, String, Boolean> tokenPeekPredicate,
                                                                 BiFunction<T, String, Boolean> tokenCapturePredicate) {
        List<TokenRecord<T>> records = new ArrayList<>();
        int charCount = 0;
        final int length = text.length();
        final StringBuilder capturedChars = new StringBuilder();
        while (charCount < length) {
            final String subString = text.substring(charCount);
            final Optional<T> matchedToken = findFirst(tokens,
                    f -> tokenPeekPredicate.apply(f, subString));
            if (matchedToken.isEmpty()) {
                throw new FailedTokenCaptureException("Failed to capture start token of " + subString);
            }
            capturedChars.append(text.charAt(charCount));
            final int countCopy = charCount;
            charCount += 1;
            while (charCount < length) {
                capturedChars.append(text.charAt(charCount));
                charCount += 1;
                if (!tokenCapturePredicate.apply(matchedToken.get(), capturedChars.toString())) {
                    capturedChars.deleteCharAt(capturedChars.length() - 1);
                    charCount -= 1;
                    break;
                }
            }
            records.add(new TokenRecord<>(matchedToken.get(), capturedChars.toString(),countCopy,charCount));
            capturedChars.setLength(0);
        }

        return records;
    }

    private static <T> Optional<T> findFirst(T[] list, Predicate<T> predicate) {
        for (T t : list) {
            if (predicate.test(t)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }


}
