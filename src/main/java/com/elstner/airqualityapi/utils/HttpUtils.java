package com.elstner.airqualityapi.utils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

public class HttpUtils {
    // Simple IPv4 regex pattern (matches basic IPv4 format)
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^([0-9]{1,3}\\.){3}[0-9]{1,3}$"
    );

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // If multiple IPs in X-Forwarded-For, take the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        if (ip != null && IPV4_PATTERN.matcher(ip).matches()) {
            return ip;
        }
        // fallback: try to parse and extract IPv4 from possible IPv6 mapped IPv4
        if (ip != null && ip.contains(":")) {
            // Sometimes IPv4 is embedded in IPv6, e.g. ::ffff:192.168.1.1
            int lastColon = ip.lastIndexOf(':');
            String possibleIPv4 = ip.substring(lastColon + 1);
            if (IPV4_PATTERN.matcher(possibleIPv4).matches()) {
                return possibleIPv4;
            }
        }

        // if no valid IPv4 found, return IPv6 address
        return ip;
    }
}
