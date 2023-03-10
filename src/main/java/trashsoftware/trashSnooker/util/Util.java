package trashsoftware.trashSnooker.util;

import org.json.JSONObject;
import trashsoftware.trashSnooker.core.Values;

import java.io.*;
import java.sql.Timestamp;
import java.util.*;

public class Util {

    public static <T> boolean arrayContains(T[] array, T value) {
        for (T ele : array) if (ele == value) return true;
        return false;
    }

    public static <T> boolean arrayContainsEqual(T[] array, T value) {
        for (T ele : array) if (Objects.equals(ele, value)) return true;
        return false;
    }

    public static boolean arrayContains(int[] array, int value) {
        for (int ele : array) if (ele == value) return true;
        return false;
    }

    public static String timeStampFmt(Timestamp timestamp) {
        String str = timestamp.toString();
        int msDotIndex = str.lastIndexOf('.');
        String noMs = str.substring(0, msDotIndex);
        String ms = str.substring(msDotIndex + 1);
        while (ms.startsWith("0")) {
            ms = ms.substring(1);
        }
        return "'" + noMs + "." + ms + "'";
    }

    public static String secondsToString(int sec) {
        if (sec < 3600) {
            return String.format("%02d:%02d", sec / 60, sec % 60);
        } else {
            return String.format("%d:%s", sec / 3600, secondsToString(sec % 3600));
        }
    }

    public static boolean deleteFile(File file) {
        if (file == null || !file.exists()) return true;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children == null) return true;
            boolean suc = true;
            for (File c : children) {
                if (!deleteFile(c)) suc = false;
            }
            return file.delete() && suc;
        } else {
            return file.delete();
        }
    }

    public static String decimalToHex(int dec, int digits) {
        int max = (1 << (digits << 2));
        if (dec >= max) throw new ArithmeticException("Too big to convert to hex");

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < digits; i++) {
            int d = dec & 15;
            char c;
            if (d < 10) c = (char) ('0' + d);
            else c = (char) ('A' + (d - 10));
            builder.append(c);
            dec >>= 4;
        }
        builder.reverse();
        return builder.toString();
    }

    private static <T> void swap(T[] array, int i, int j) {
        T temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    public static <T> void shuffleArray(T[] array) {
        Random random = new Random();
        for (int i = array.length; i > 0; i--) {
            int index = random.nextInt(i);
            swap(array, index, i - 1);
        }
    }

    public static <T> void reverseArray(T[] array) {
        int mid = array.length / 2;
        for (int i = 0; i < mid; i++) {
            T temp = array[i];
            array[i] = array[array.length - i - 1];
            array[array.length - i - 1] = temp;
        }
    }

    @SafeVarargs
    public static <K, V> Map<K, V> mergeMaps(Map<K, V>... maps) {
        Map<K, V> res = new HashMap<>();
        for (Map<K, V> map : maps) {
            res.putAll(map);
        }
        return res;
    }

    public static void intToBytesN(long num, byte[] arr, int index, int nBytes) {
        for (int i = 0; i < nBytes; i++)
            arr[index + i] =
                    (byte) ((num >>> ((nBytes - 1 - i) << 3)) & 0xff);
    }

    public static long bytesToIntN(byte[] arr, int index, int nBytes) {
        long result = 0;
        for (int i = 0; i < nBytes; i++)
            result =
                    (arr[index + i] & 0xffL) << ((nBytes - 1 - i) << 3) | result;
        return result;
    }

    public static void int32ToBytes(int n, byte[] arr, int index) {
        intToBytesN(n, arr, index, 4);
    }

    public static int bytesToInt32(byte[] arr, int index) {
        return (int) bytesToIntN(arr, index, 4);
    }

    /**
     * Convert a long into an 8-byte array in big-endian.
     *
     * @param l the long.
     */
    public static void longToBytes(long l, byte[] arr, int index) {
        intToBytesN(l, arr, index, 8);
//        for (int i = 0; i < 8; i++) arr[index + i] = (byte) ((l >> ((7 - i) << 3)) & 0xff);
    }

    /**
     * Convert a 8-byte array into signed long in big-endian.
     *
     * @param b byte array.
     * @return signed long.
     */
    public static long bytesToLong(byte[] b, int index) {
        return bytesToIntN(b, index, 8);
//        long result = 0;
//        for (int i = 0; i < 8; i++) result = (b[index + i] & 0xffL) << ((7 - i) << 3) | result;
//        return result;
    }

    public static void doubleToBytes(double d, byte[] arr, int index) {
        long bits = Double.doubleToLongBits(d);
        longToBytes(bits, arr, index);
    }

    public static double bytesToDouble(byte[] b, int index) {
        return Double.longBitsToDouble(bytesToLong(b, index));
    }

    public static int indexOf(char c, char[] arr) {
        for (int i = 0; i < arr.length; i++) if (c == arr[i]) return i;
        return -1;
    }

    public static int indexOf(byte c, byte[] arr) {
        for (int i = 0; i < arr.length; i++) if (c == arr[i]) return i;
        return -1;
    }

    public static String timeToReadable(long ms) {
        long s = Math.round(ms / 1000.0);
        if (s < 60) {
            return "0:" + s;
        } else if (s < 3600) {
            return String.format("%d:%d", s / 60, s % 60);
        } else {
            long h = s / 3600;
            long mm_ss = s % 3600;
            return String.format("%d:%d:%d", h, mm_ss / 60, mm_ss % 60);
        }
    }

    public static double powerMultiplierOfCuePoint(double unitX, double unitY) {
        double dt = Math.hypot(Math.abs(unitX), Math.abs(unitY));  // 0-1之间
        double gap = 1 - Values.CUE_POINT_MULTIPLIER;
        return (1 - dt) * gap + Values.CUE_POINT_MULTIPLIER;
    }

    public static JSONObject readJson(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                builder.append(line);
            }
            return new JSONObject(builder.toString());
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeJson(JSONObject jsonObject, File file) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(jsonObject.toString(2));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static boolean isAllCap(String s) {
        for (char c : s.toCharArray()) {
            if (!Character.isUpperCase(c)) return false;
        }
        return true;
    }

    public static String toLowerCamelCase(String s) {
        String[] words;
        
        if (s.contains("_")) {
            words = s.split("_");
        } else if (s.contains(" ")) {
            words = s.split(" ");
        } else if (isAllCap(s)) {
            words = new String[]{s};
        } else {
            // probably upper camel, or a single word
            List<String> strings = new ArrayList<>();
            StringBuilder wordBuilder = new StringBuilder();
            for (char c : s.toCharArray()) {
                if (Character.isUpperCase(c)) {
                    if (wordBuilder.length() > 0) {
                        strings.add(wordBuilder.toString());
                        wordBuilder.setLength(0);
                    }
                } else if (Character.isDigit(c)) {
                    if (wordBuilder.length() > 0) {
                        if (!Character.isDigit(wordBuilder.charAt(0))) {
                            strings.add(wordBuilder.toString());
                            wordBuilder.setLength(0);
                        }
                    }
                } else {
                    // 是小写字母
                    if (wordBuilder.length() > 0) {
                        if (Character.isDigit(wordBuilder.charAt(0))) {
                            strings.add(wordBuilder.toString());
                            wordBuilder.setLength(0);
                        }
                    }
                }
                wordBuilder.append(c);
            }
            if (wordBuilder.length() > 0) {
                strings.add(wordBuilder.toString());
            }
            words = strings.toArray(new String[0]);
        }

        StringBuilder builder = new StringBuilder()
                .append(words[0].toLowerCase(Locale.ROOT));
        for (int i = 1; i < words.length; i++) {
            char first = Character.toUpperCase(words[i].charAt(0));
            String rest = words[i].substring(1);
            builder.append(first)
                    .append(rest.toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }
}
