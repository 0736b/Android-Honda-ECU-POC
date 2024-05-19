package com.example.hondak_linediagnostic;

public class ByteUtil {

    // Method to convert byte array to a formatted hex string
    public static String toHexString(byte[] byteArray) {
        if (byteArray == null || byteArray.length == 0) {
            return "";
        }

        StringBuilder hexString = new StringBuilder(byteArray.length * 3);
        for (byte b : byteArray) {
            hexString.append(String.format("%02X ", b));
        }

        return hexString.toString().trim();
    }

}
