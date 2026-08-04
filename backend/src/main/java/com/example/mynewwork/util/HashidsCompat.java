package com.example.mynewwork.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Hashids v2.x 实现，与 Node.js hashids 库 (hashids@2.3.0) 兼容。
 */
public class HashidsCompat {

    private static final String DEFAULT_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
    private static final String DEFAULT_SEPS = "cfhistuCFHISTU";
    private static final double SEP_DIV = 3.5;
    private static final int GUARD_DIV = 12;

    private final int minLength;
    private final List<Character> salt;
    private final List<Character> alphabet;
    private final List<Character> seps;
    private final List<Character> guards;

    public HashidsCompat(String salt, int minLength) {
        this.minLength = minLength;
        this.salt = toCharList(salt);

        Set<Character> uniqueAlpha = new LinkedHashSet<>();
        for (char c : DEFAULT_ALPHABET.toCharArray()) uniqueAlpha.add(c);
        List<Character> alphaList = new ArrayList<>(uniqueAlpha);

        Set<Character> sepsSet = new LinkedHashSet<>();
        for (char c : DEFAULT_SEPS.toCharArray()) {
            if (alphaList.contains(c)) sepsSet.add(c);
        }
        List<Character> sepsList = new ArrayList<>(sepsSet);

        // Filter seps chars from alphabet (JS: g.filter(t => !l.includes(t)))
        List<Character> filteredAlpha = new ArrayList<>();
        for (char c : alphaList) {
            if (!sepsSet.contains(c)) filteredAlpha.add(c);
        }

        this.seps = consistentShuffle(sepsList, this.salt);

        if (this.seps.isEmpty() || (double) filteredAlpha.size() / this.seps.size() > SEP_DIV) {
            int sepsLen = (int) Math.ceil(filteredAlpha.size() / SEP_DIV);
            if (sepsLen == 1) sepsLen++;
            if (sepsLen > this.seps.size()) {
                int diff = sepsLen - this.seps.size();
                this.seps.addAll(filteredAlpha.subList(0, diff));
                filteredAlpha = filteredAlpha.subList(diff, filteredAlpha.size());
            } else {
                this.seps.subList(sepsLen, this.seps.size()).clear();
            }
        }

        this.alphabet = consistentShuffle(filteredAlpha, this.salt);

        int guardCount = (int) Math.ceil((double) this.alphabet.size() / GUARD_DIV);
        if (this.alphabet.size() < 3) {
            this.guards = new ArrayList<>(this.seps.subList(0, guardCount));
            this.seps.subList(0, guardCount).clear();
        } else {
            this.guards = new ArrayList<>(this.alphabet.subList(0, guardCount));
            this.alphabet.subList(0, guardCount).clear();
        }
    }

    public String encode(long... numbers) {
        if (numbers.length == 0) return "";
        return new String(toCharArray(_encode(numbers)));
    }

    public long[] decode(String hash) {
        if (hash == null || hash.isEmpty()) return new long[0];
        long[] result = _decode(hash);
        return result;
    }

    private List<Character> _encode(long[] numbers) {
        List<Character> alph = new ArrayList<>(this.alphabet);

        int numberHashInt = 0;
        for (int i = 0; i < numbers.length; i++) {
            numberHashInt += (int) (numbers[i] % (i + 100));
        }

        char lottery = alph.get(numberHashInt % alph.size());
        List<Character> result = new ArrayList<>();
        result.add(lottery);

        for (int i = 0; i < numbers.length; i++) {
            long num = numbers[i];
            List<Character> buffer = new ArrayList<>();
            buffer.add(lottery);
            buffer.addAll(this.salt);
            buffer.addAll(alph);
            alph = consistentShuffle(alph, buffer);

            List<Character> hash = hash(num, alph);
            result.addAll(hash);

            if (i + 1 < numbers.length) {
                int sepIdx = (int) (num % (hash.get(0) + i));
                result.add(this.seps.get(sepIdx % this.seps.size()));
            }
        }

        if (result.size() < minLength) {
            int guardIdx = (numberHashInt + result.get(0)) % this.guards.size();
            result.add(0, this.guards.get(guardIdx));

            if (result.size() < minLength) {
                guardIdx = (numberHashInt + result.get(2)) % this.guards.size();
                result.add(this.guards.get(guardIdx));
            }
        }

        int halfLen = alph.size() / 2;
        while (result.size() < minLength) {
            alph = consistentShuffle(alph, alph);
            List<Character> newResult = new ArrayList<>(alph.subList(halfLen, alph.size()));
            newResult.addAll(result);
            newResult.addAll(alph.subList(0, halfLen));
            result = newResult;

            int excess = result.size() - minLength;
            if (excess > 0) {
                result = result.subList(excess / 2, excess / 2 + minLength);
            }
        }

        return result;
    }

    private long[] _decode(String hash) {
        String guardStr = joinCharsRegex(this.guards);
        String[] parts = hash.split(guardStr);

        int i = 0;
        if (parts.length == 2 || parts.length == 3) i = 1;

        String hashBreakdown = parts[i];
        if (hashBreakdown.isEmpty()) return new long[0];

        char lottery = hashBreakdown.charAt(0);
        String remaining = hashBreakdown.substring(1);

        String sepStr = joinCharsRegex(this.seps);
        String[] hashArray = remaining.split(sepStr);

        List<Character> alph = new ArrayList<>(this.alphabet);
        List<Long> ret = new ArrayList<>();

        for (String subHash : hashArray) {
            if (subHash.isEmpty()) continue;
            List<Character> buffer = new ArrayList<>();
            buffer.add(lottery);
            buffer.addAll(this.salt);
            buffer.addAll(alph);
            buffer = buffer.subList(0, alph.size());
            alph = consistentShuffle(alph, buffer);
            ret.add(unhash(subHash, alph));
        }

        long[] arr = new long[ret.size()];
        for (int k = 0; k < ret.size(); k++) arr[k] = ret.get(k);

        String verify = encode(arr);
        if (!verify.equals(hash)) {
            return new long[0];
        }

        return arr;
    }

    private static List<Character> hash(long number, List<Character> alphabet) {
        List<Character> result = new ArrayList<>();
        long num = number;
        do {
            result.add(0, alphabet.get((int) (num % alphabet.size())));
            num /= alphabet.size();
        } while (num > 0);
        return result;
    }

    private static long unhash(String input, List<Character> alphabet) {
        long number = 0;
        for (int i = 0; i < input.length(); i++) {
            int pos = alphabet.indexOf(input.charAt(i));
            if (pos == -1) return -1;
            number = number * alphabet.size() + pos;
        }
        return number;
    }

    private static List<Character> consistentShuffle(List<Character> alphabet, List<Character> salt) {
        if (salt.isEmpty()) return new ArrayList<>(alphabet);

        List<Character> result = new ArrayList<>(alphabet);
        for (int i = result.size() - 1, v = 0, p = 0; i > 0; i--, v++) {
            v %= salt.size();
            int ascVal = salt.get(v);
            p += ascVal;
            int j = (ascVal + v + p) % i;

            char temp = result.get(j);
            result.set(j, result.get(i));
            result.set(i, temp);
        }
        return result;
    }

    private static List<Character> toCharList(String s) {
        List<Character> list = new ArrayList<>();
        for (char c : s.toCharArray()) list.add(c);
        return list;
    }

    private static char[] toCharArray(List<Character> list) {
        char[] arr = new char[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    private static String joinChars(List<Character> chars) {
        StringBuilder sb = new StringBuilder();
        for (char c : chars) sb.append(c);
        return sb.toString();
    }

    private static String joinCharsRegex(List<Character> chars) {
        StringBuilder sb = new StringBuilder();
        for (char c : chars) {
            if ("\\[](){}.*+?^$|".indexOf(c) >= 0) sb.append('\\');
            sb.append(c);
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        HashidsCompat h = new HashidsCompat("(*%!~%^ynU-0882++=", 8);

        System.out.print("decode 1E3wBBoxAR9: [");
        long[] decoded = h.decode("1E3wBBoxAR9");
        for (int i = 0; i < decoded.length; i++) {
            if (i > 0) System.out.print(", ");
            System.out.print(decoded[i]);
        }
        System.out.println("]");

        System.out.println("encode 819049738245509: " + h.encode(819049738245509L));
        System.out.println("encode 811624383429829: " + h.encode(811624383429829L));
        System.out.println("decode EJvavW4V3gJ: [" + h.decode("EJvavW4V3gJ")[0] + "]");
        System.out.println("roundtrip 819049738245509: " + h.decode(h.encode(819049738245509L))[0]);
        System.out.println("roundtrip 811624383429829: " + h.decode(h.encode(811624383429829L))[0]);
        System.out.println("encode 832238406698373: " + h.encode(832238406698373L));
    }
}
