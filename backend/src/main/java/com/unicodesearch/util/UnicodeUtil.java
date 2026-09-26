package com.unicodesearch.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Unicode-aware text preprocessing utilities.
 *
 * Three representations of a text are used throughout the engine:
 *
 *   original   - exactly what is on disk (never modified, used for display)
 *   normalized - NFC form, case and line breaks preserved (used for snippets)
 *   searchable - normalized + Latin case folding, SAME LENGTH as normalized
 *                (used by the inverted index and every matcher)
 *
 * Because searchable and normalized have identical length, a match position
 * found in the searchable text is a valid index into the normalized text,
 * which is what lets snippets highlight the original-cased words.
 *
 * Indian scripts (Telugu, Devanagari, Tamil, Bengali) use combining marks
 * that can be encoded in more than one code point sequence; NFC makes
 * identical-looking text compare equal. Indic scripts have no case, so case
 * folding only touches Latin letters.
 */
public final class UnicodeUtil {

    private static final Pattern PUNCTUATION =
            Pattern.compile("[\\p{Punct}\\u0964\\u0965\\uFF0C\\u3002]");

    private UnicodeUtil() {
    }

    /** A token and its start offset inside the text it was extracted from. */
    public record Token(String text, int start) { }

    /** NFC normalization (canonical composition). */
    public static String normalize(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFC);
    }

    /**
     * Case-folds Latin letters ONLY, character by character, so the result has
     * exactly the same length as the input. Indic characters pass through.
     * O(n) time, O(n) space.
     */
    public static String foldCase(String text) {
        if (text == null || text.isEmpty()) return "";
        char[] out = text.toCharArray();
        for (int i = 0; i < out.length; i++) {
            char c = out[i];
            if (c < 0x0250 && Character.isUpperCase(c)) {
                out[i] = Character.toLowerCase(c);
            }
        }
        return new String(out);
    }

    /**
     * Searchable representation: NFC + Latin case folding. Line breaks and
     * spacing are preserved so positions stay aligned with the normalized text.
     */
    public static String toSearchable(String text) {
        return foldCase(normalize(text));
    }

    /** NFC + trim + whitespace collapse (kept for backward compatibility). */
    public static String preprocess(String text) {
        String normalized = normalize(text);
        return normalized.trim().replaceAll("\\s+", " ");
    }

    /** Strips punctuation for tokenization purposes only. */
    public static String stripPunctuation(String text) {
        return PUNCTUATION.matcher(text).replaceAll(" ").replaceAll("\\s+", " ").trim();
    }

    /**
     * True if the character can be part of a word. Letters and digits, plus
     * the combining marks (vowel signs, viramas, nuktas) and ZWJ/ZWNJ that
     * Indic scripts need INSIDE a word -- treating those as separators would
     * cut Telugu/Hindi/Tamil/Bengali words into meaningless pieces.
     */
    public static boolean isTokenChar(char c) {
        if (Character.isLetterOrDigit(c)) return true;
        int type = Character.getType(c);
        return type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK
                || c == '\u200C' || c == '\u200D';
    }

    /**
     * Tokenizes a text that is ALREADY in searchable form and returns every
     * token with its start offset. O(n).
     */
    public static List<Token> tokenizeWithOffsets(String searchableText) {
        List<Token> tokens = new ArrayList<>();
        int n = searchableText.length();
        int i = 0;
        while (i < n) {
            if (!isTokenChar(searchableText.charAt(i))) { i++; continue; }
            int start = i;
            while (i < n && isTokenChar(searchableText.charAt(i))) i++;
            tokens.add(new Token(searchableText.substring(start, i), start));
        }
        return tokens;
    }

    /**
     * Tokenizes arbitrary text (a user query, for instance) into searchable
     * tokens: NFC + Latin case folding + word splitting. This is the SAME
     * pipeline used on the corpus, so query keywords and index terms are
     * always in compatible representations.
     */
    public static List<String> tokenize(String text) {
        List<String> out = new ArrayList<>();
        for (Token t : tokenizeWithOffsets(toSearchable(text))) out.add(t.text());
        return out;
    }

    /** Detects the dominant Unicode script of a piece of text. */
    public static String detectScript(String text) {
        if (text == null || text.isEmpty()) return "unknown";
        int telugu = 0, devanagari = 0, tamil = 0, bengali = 0, latin = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x0C00 && c <= 0x0C7F) telugu++;
            else if (c >= 0x0900 && c <= 0x097F) devanagari++;
            else if (c >= 0x0B80 && c <= 0x0BFF) tamil++;
            else if (c >= 0x0980 && c <= 0x09FF) bengali++;
            else if (Character.isLetter(c) && c < 0x0250) latin++;
        }
        int max = Math.max(Math.max(telugu, devanagari), Math.max(tamil, Math.max(bengali, latin)));
        if (max == 0) return "unknown";
        if (max == telugu) return "telugu";
        if (max == devanagari) return "hindi";
        if (max == tamil) return "tamil";
        if (max == bengali) return "bengali";
        return "english";
    }
}
