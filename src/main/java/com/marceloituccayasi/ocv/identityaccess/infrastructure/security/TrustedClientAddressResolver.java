package com.marceloituccayasi.ocv.identityaccess.infrastructure.security;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.marceloituccayasi.ocv.identityaccess.infrastructure.configuration.TrustedProxyProperties;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the client address without trusting spoofable forwarding headers.
 */
@Component
public final class TrustedClientAddressResolver {

    private static final String FORWARDED_HEADER =
            "Forwarded";

    private static final String X_FORWARDED_FOR_HEADER =
            "X-Forwarded-For";

    private final List<CidrBlock> trustedProxyCidrs;

    public TrustedClientAddressResolver(
            TrustedProxyProperties properties) {

        this.trustedProxyCidrs =
                parseConfiguredCidrs(properties.getCidrs());
    }

    public String resolve(HttpServletRequest request) {
        InetAddress directPeer =
                CidrBlock.parseAddressLiteral(
                        request.getRemoteAddr());

        if (trustedProxyCidrs.isEmpty()
                || !isTrustedProxy(directPeer)) {

            return CidrBlock.canonicalAddress(
                    directPeer);
        }

        List<InetAddress> forwardedChain =
                resolveForwardedChain(request);

        if (forwardedChain == null
                || forwardedChain.isEmpty()) {

            return CidrBlock.canonicalAddress(
                    directPeer);
        }

        List<InetAddress> completeChain =
                new ArrayList<>(forwardedChain);

        completeChain.add(directPeer);

        for (int index = completeChain.size() - 1;
                index >= 0;
                index--) {

            InetAddress candidate =
                    completeChain.get(index);

            if (index == 0
                    || !isTrustedProxy(candidate)) {

                return CidrBlock.canonicalAddress(
                        candidate);
            }
        }

        return CidrBlock.canonicalAddress(
                directPeer);
    }

    private List<InetAddress> resolveForwardedChain(
            HttpServletRequest request) {

        String forwarded =
                request.getHeader(FORWARDED_HEADER);

        if (forwarded != null
                && !forwarded.isBlank()) {

            return parseForwardedHeader(forwarded);
        }

        String xForwardedFor =
                request.getHeader(
                        X_FORWARDED_FOR_HEADER);

        if (xForwardedFor == null
                || xForwardedFor.isBlank()) {

            return List.of();
        }

        return parseAddressList(xForwardedFor);
    }

    private List<InetAddress> parseForwardedHeader(
            String headerValue) {

        List<InetAddress> addresses =
                new ArrayList<>();

        try {
            for (String element
                    : headerValue.split(",")) {

                String forwardedFor = null;

                for (String parameter
                        : element.split(";")) {

                    int separator =
                            parameter.indexOf('=');

                    if (separator <= 0) {
                        continue;
                    }

                    String name =
                            parameter
                                    .substring(
                                            0,
                                            separator)
                                    .trim();

                    if (name.equalsIgnoreCase("for")) {
                        forwardedFor =
                                parameter
                                        .substring(
                                                separator + 1)
                                        .trim();

                        break;
                    }
                }

                if (forwardedFor != null) {
                    addresses.add(
                            parseForwardedAddress(
                                    forwardedFor));
                }
            }
        }
        catch (IllegalArgumentException exception) {
            return null;
        }

        return addresses;
    }

    private List<InetAddress> parseAddressList(
            String headerValue) {

        List<InetAddress> addresses =
                new ArrayList<>();

        try {
            for (String value
                    : headerValue.split(",")) {

                addresses.add(
                        parseForwardedAddress(value));
            }
        }
        catch (IllegalArgumentException exception) {
            return null;
        }

        return addresses;
    }

    private InetAddress parseForwardedAddress(
            String configuredValue) {

        String value = configuredValue.trim();

        if (value.length() >= 2
                && value.startsWith("\"")
                && value.endsWith("\"")) {

            value = value.substring(
                    1,
                    value.length() - 1);
        }

        if (value.isBlank()
                || value.equalsIgnoreCase("unknown")
                || value.startsWith("_")) {

            throw new IllegalArgumentException(
                    "Unsupported forwarded address.");
        }

        if (value.startsWith("[")) {
            int closingBracket =
                    value.indexOf(']');

            if (closingBracket <= 1) {
                throw new IllegalArgumentException(
                        "Invalid forwarded IPv6 address.");
            }

            String address =
                    value.substring(
                            1,
                            closingBracket);

            String remainder =
                    value.substring(
                            closingBracket + 1);

            if (!remainder.isEmpty()
                    && !isValidPortSuffix(remainder)) {

                throw new IllegalArgumentException(
                        "Invalid forwarded address suffix.");
            }

            return CidrBlock.parseAddressLiteral(
                    address);
        }

        int firstColon =
                value.indexOf(':');

        int lastColon =
                value.lastIndexOf(':');

        if (firstColon > 0
                && firstColon == lastColon) {

            String possibleAddress =
                    value.substring(0, firstColon);

            String possiblePort =
                    value.substring(firstColon + 1);

            if (CidrBlock.isIpv4Literal(possibleAddress)
                    && isNumericPort(possiblePort)) {

                return CidrBlock.parseAddressLiteral(
                        possibleAddress);
            }
        }

        return CidrBlock.parseAddressLiteral(
                value.toLowerCase(Locale.ROOT));
    }

    private boolean isTrustedProxy(InetAddress address) {
        return trustedProxyCidrs.stream()
                .anyMatch(cidr ->
                        cidr.contains(address));
    }

    private static List<CidrBlock> parseConfiguredCidrs(
            String configuredCidrs) {

        if (configuredCidrs == null
                || configuredCidrs.isBlank()) {

            return List.of();
        }

        List<CidrBlock> parsedCidrs =
                new ArrayList<>();

        for (String configuredCidr
                : configuredCidrs.split(",", -1)) {

            parsedCidrs.add(
                    CidrBlock.parse(configuredCidr));
        }

        return List.copyOf(parsedCidrs);
    }

    private static boolean isValidPortSuffix(
            String value) {

        return value.startsWith(":")
                && isNumericPort(
                        value.substring(1));
    }

    private static boolean isNumericPort(
            String value) {

        if (value.isBlank()) {
            return false;
        }

        try {
            int port =
                    Integer.parseInt(value);

            return port >= 0 && port <= 65_535;
        }
        catch (NumberFormatException exception) {
            return false;
        }
    }

}
