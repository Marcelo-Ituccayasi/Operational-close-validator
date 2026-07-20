package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * Parsed IPv4 or IPv6 CIDR block.
 */
final class CidrBlock {

    private final byte[] networkAddress;
    private final int prefixLength;

    private CidrBlock(
            byte[] networkAddress,
            int prefixLength) {

        this.networkAddress = networkAddress.clone();
        this.prefixLength = prefixLength;
    }

    static CidrBlock parse(String configuredValue) {
        if (configuredValue == null
                || configuredValue.isBlank()) {

            throw invalidCidr(configuredValue);
        }

        String value = configuredValue.trim();
        int separator = value.indexOf('/');

        if (separator <= 0
                || separator != value.lastIndexOf('/')
                || separator == value.length() - 1) {

            throw invalidCidr(configuredValue);
        }

        String addressValue =
                value.substring(0, separator).trim();

        String prefixValue =
                value.substring(separator + 1).trim();

        InetAddress address;

        try {
            address = parseAddressLiteral(addressValue);
        }
        catch (IllegalArgumentException exception) {
            throw invalidCidr(configuredValue);
        }

        int prefix;

        try {
            prefix = Integer.parseInt(prefixValue);
        }
        catch (NumberFormatException exception) {
            throw invalidCidr(configuredValue);
        }

        int maximumPrefix =
                address.getAddress().length * Byte.SIZE;

        if (prefix < 0 || prefix > maximumPrefix) {
            throw invalidCidr(configuredValue);
        }

        return new CidrBlock(
                address.getAddress(),
                prefix);
    }

    boolean contains(InetAddress candidate) {
        Objects.requireNonNull(candidate);

        byte[] candidateAddress =
                candidate.getAddress();

        if (candidateAddress.length
                != networkAddress.length) {

            return false;
        }

        int completeBytes =
                prefixLength / Byte.SIZE;

        int remainingBits =
                prefixLength % Byte.SIZE;

        for (int index = 0;
                index < completeBytes;
                index++) {

            if (candidateAddress[index]
                    != networkAddress[index]) {

                return false;
            }
        }

        if (remainingBits == 0) {
            return true;
        }

        int mask =
                (0xFF << (Byte.SIZE - remainingBits))
                        & 0xFF;

        int networkByte =
                networkAddress[completeBytes] & mask;

        int candidateByte =
                candidateAddress[completeBytes] & mask;

        return networkByte == candidateByte;
    }

    static InetAddress parseAddressLiteral(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "IP address must not be blank.");
        }

        String trimmed = value.trim();

        if (trimmed.indexOf('%') >= 0) {
            throw invalidAddress(trimmed, null);
        }

        if (looksLikeIpv4Literal(trimmed)) {
            return parseIpv4Literal(trimmed);
        }

        if (!looksLikeIpv6Literal(trimmed)) {
            throw invalidAddress(trimmed, null);
        }

        try {
            InetAddress parsed =
                    InetAddress.getByName(trimmed);

            if (!(parsed instanceof Inet6Address)) {
                throw invalidAddress(trimmed, null);
            }

            return parsed;
        }
        catch (UnknownHostException exception) {
            throw invalidAddress(
                    trimmed,
                    exception);
        }
    }

    static boolean isIpv4Literal(String value) {
        try {
            return parseAddressLiteral(value)
                    instanceof Inet4Address;
        }
        catch (IllegalArgumentException exception) {
            return false;
        }
    }

    static String canonicalAddress(InetAddress address) {
        return Objects.requireNonNull(address)
                .getHostAddress();
    }

    private static InetAddress parseIpv4Literal(
            String value) {

        String[] octets =
                value.split("\\.", -1);

        if (octets.length != 4) {
            throw invalidAddress(value, null);
        }

        byte[] address = new byte[4];

        for (int index = 0;
                index < octets.length;
                index++) {

            String octet = octets[index];

            if (octet.isEmpty()
                    || octet.length() > 3
                    || !containsOnlyDecimalDigits(octet)) {

                throw invalidAddress(value, null);
            }

            int numericOctet;

            try {
                numericOctet =
                        Integer.parseInt(octet);
            }
            catch (NumberFormatException exception) {
                throw invalidAddress(
                        value,
                        exception);
            }

            if (numericOctet > 255) {
                throw invalidAddress(value, null);
            }

            address[index] =
                    (byte) numericOctet;
        }

        try {
            return InetAddress.getByAddress(address);
        }
        catch (UnknownHostException exception) {
            throw invalidAddress(
                    value,
                    exception);
        }
    }

    private static boolean looksLikeIpv4Literal(
            String value) {

        return value.indexOf('.') >= 0
                && value.indexOf(':') < 0;
    }

    private static boolean looksLikeIpv6Literal(
            String value) {

        if (value.indexOf(':') < 0) {
            return false;
        }

        for (int index = 0;
                index < value.length();
                index++) {

            char character =
                    value.charAt(index);

            boolean hexadecimal =
                    character >= '0'
                            && character <= '9'
                    || character >= 'a'
                            && character <= 'f'
                    || character >= 'A'
                            && character <= 'F';

            if (!hexadecimal
                    && character != ':'
                    && character != '.') {

                return false;
            }
        }

        return true;
    }

    private static boolean containsOnlyDecimalDigits(
            String value) {

        for (int index = 0;
                index < value.length();
                index++) {

            char character =
                    value.charAt(index);

            if (character < '0'
                    || character > '9') {

                return false;
            }
        }

        return true;
    }

    private static IllegalArgumentException invalidAddress(
            String value,
            Exception cause) {

        String message =
                "Invalid IP address literal: "
                        + value;

        if (cause == null) {
            return new IllegalArgumentException(
                    message);
        }

        return new IllegalArgumentException(
                message,
                cause);
    }

    private static IllegalArgumentException invalidCidr(
            String value) {

        return new IllegalArgumentException(
                "Invalid trusted proxy CIDR: "
                        + String.valueOf(value));
    }

}
