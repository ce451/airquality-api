package com.elstner.airqualityapi.utils;

import com.elstner.airqualityapi.util.TestConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for HttpUtils - IP extraction from HTTP requests.
 * This is CRITICAL as station identification depends on accurate IP extraction.
 */
@DisplayName("HttpUtils - IP Extraction Tests")
class HttpUtilsTest {

    @Test
    @DisplayName("getClientIp - should extract IP from X-Forwarded-For header")
    void getClientIp_WithForwardedHeader_ReturnsFirstIp() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(TestConstants.DEFAULT_IP_ADDRESS);

        // When
        String result = HttpUtils.getClientIp(request);

        // Then
        assertThat(result).isEqualTo(TestConstants.DEFAULT_IP_ADDRESS);
    }

    @Test
    @DisplayName("getClientIp - should extract first IP when multiple IPs in X-Forwarded-For")
    void getClientIp_WithMultipleForwardedIps_ReturnsFirstIp() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        String multipleIps = TestConstants.DEFAULT_IP_ADDRESS + ", " + TestConstants.PROXY_IP;
        when(request.getHeader("X-Forwarded-For")).thenReturn(multipleIps);

        // When
        String result = HttpUtils.getClientIp(request);

        // Then
        assertThat(result)
            .isEqualTo(TestConstants.DEFAULT_IP_ADDRESS)
            .describedAs("Should extract first IP from comma-separated list");
    }

    @Test
    @DisplayName("getClientIp - should trim whitespace from extracted IP")
    void getClientIp_WithWhitespace_ReturnsTrimmedIp() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        String ipWithSpaces = " " + TestConstants.DEFAULT_IP_ADDRESS + " , " + TestConstants.PROXY_IP;
        when(request.getHeader("X-Forwarded-For")).thenReturn(ipWithSpaces);

        // When
        String result = HttpUtils.getClientIp(request);

        // Then
        assertThat(result)
            .isEqualTo(TestConstants.DEFAULT_IP_ADDRESS)
            .doesNotContainAnyWhitespaces();
    }

    @Test
    @DisplayName("getClientIp - should fallback to RemoteAddr when X-Forwarded-For is null")
    void getClientIp_WithoutForwardedHeader_ReturnsRemoteAddr() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(TestConstants.ALTERNATE_IP_ADDRESS);

        // When
        String result = HttpUtils.getClientIp(request);

        // Then
        assertThat(result).isEqualTo(TestConstants.ALTERNATE_IP_ADDRESS);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"unknown"})
    @DisplayName("getClientIp - should fallback to RemoteAddr when X-Forwarded-For is null/empty/unknown")
    void getClientIp_WithInvalidForwardedHeader_ReturnsRemoteAddr(String invalidHeader) {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(invalidHeader);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(TestConstants.ALTERNATE_IP_ADDRESS);

        // When
        String result = HttpUtils.getClientIp(request);

        // Then
        assertThat(result).isEqualTo(TestConstants.ALTERNATE_IP_ADDRESS);
    }

    @Test
    @DisplayName("getClientIp - should try Proxy-Client-IP when X-Forwarded-For is unknown")
    void getClientIp_WithProxyClientIP_ReturnsProxyClientIP() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(request.getHeader("Proxy-Client-IP")).thenReturn(TestConstants.DEFAULT_IP_ADDRESS);

        // When
        String result = HttpUtils.getClientIp(request);

        // Then
        assertThat(result).isEqualTo(TestConstants.DEFAULT_IP_ADDRESS);
    }

    @Test
    @DisplayName("getClientIp - should try WL-Proxy-Client-IP when X-Forwarded-For and Proxy-Client-IP are unknown")
    void getClientIp_WithWLProxyClientIP_ReturnsWLProxyClientIP() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(TestConstants.DEFAULT_IP_ADDRESS);

        // When
        String result = HttpUtils.getClientIp(request);

        // Then
        assertThat(result).isEqualTo(TestConstants.DEFAULT_IP_ADDRESS);
    }

    @ParameterizedTest
    @CsvSource({
        "192.168.1.100, 192.168.1.100",
        "10.0.0.1, 10.0.0.1",
        "172.16.0.1, 172.16.0.1",
        "127.0.0.1, 127.0.0.1",
        "255.255.255.255, 255.255.255.255"
    })
    @DisplayName("getClientIp - should correctly handle various IPv4 formats")
    void getClientIp_VariousIPv4Formats_ReturnsCorrectIp(String inputIp, String expectedIp) {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(inputIp);

        // When
        String result = HttpUtils.getClientIp(request);

        // Then
        assertThat(result).isEqualTo(expectedIp);
    }

    @Test
    @DisplayName("getClientIp - should handle IPv6 address")
    void getClientIp_WithIPv6_ReturnsIPv6() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(TestConstants.IPV6_ADDRESS);

        // When
        String result = HttpUtils.getClientIp(request);

        // Then
        assertThat(result).isEqualTo(TestConstants.IPV6_ADDRESS);
    }

    @Test
    @DisplayName("getClientIp - should extract IPv4 from IPv6-mapped IPv4 address")
    void getClientIp_WithIPv6MappedIPv4_ReturnsIPv4() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        String ipv6MappedIpv4 = "::ffff:" + TestConstants.DEFAULT_IP_ADDRESS;
        when(request.getHeader("X-Forwarded-For")).thenReturn(ipv6MappedIpv4);

        // When
        String result = HttpUtils.getClientIp(request);

        // Then
        assertThat(result)
            .isEqualTo(TestConstants.DEFAULT_IP_ADDRESS)
            .describedAs("Should extract IPv4 from IPv6-mapped address");
    }

    @Test
    @DisplayName("getClientIp - should handle full IPv6-mapped IPv4 with ports")
    void getClientIp_WithIPv6MappedIPv4AndPort_ReturnsIPv4() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        String complexAddress = "::1:192.168.1.100";
        when(request.getHeader("X-Forwarded-For")).thenReturn(complexAddress);

        // When
        String result = HttpUtils.getClientIp(request);

        // Then
        assertThat(result)
            .isEqualTo(TestConstants.DEFAULT_IP_ADDRESS)
            .describedAs("Should extract IPv4 from complex IPv6 format");
    }

    @Test
    @DisplayName("getClientIp - should return pure IPv6 when no IPv4 can be extracted")
    void getClientIp_WithPureIPv6_ReturnsIPv6() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        String pureIpv6 = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";
        when(request.getHeader("X-Forwarded-For")).thenReturn(pureIpv6);

        // When
        String result = HttpUtils.getClientIp(request);

        // Then
        assertThat(result)
            .isEqualTo(pureIpv6)
            .describedAs("Should return IPv6 when no IPv4 can be extracted");
    }

    @Test
    @DisplayName("getClientIp - should handle real-world proxy chain scenario")
    void getClientIp_RealWorldProxyChain_ReturnsClientIp() {
        // Given - Real-world scenario: client -> proxy1 -> proxy2 -> server
        HttpServletRequest request = mock(HttpServletRequest.class);
        String proxyChain = "203.0.113.45, 198.51.100.178, 192.0.2.23";
        when(request.getHeader("X-Forwarded-For")).thenReturn(proxyChain);

        // When
        String result = HttpUtils.getClientIp(request);

        // Then
        assertThat(result)
            .isEqualTo("203.0.113.45")
            .describedAs("Should return the original client IP, not proxy IPs");
    }

    @ParameterizedTest
    @CsvSource({
        "'999.999.999.999', '999.999.999.999'",  // Invalid but returned as-is
        "'192.168.1', '192.168.1'",              // Incomplete IPv4
        "'invalid-ip', 'invalid-ip'"             // Completely invalid
    })
    @DisplayName("getClientIp - should handle edge cases with invalid IPs")
    void getClientIp_WithInvalidIPFormat_ReturnsAsIs(String invalidIp, String expected) {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(invalidIp);

        // When
        String result = HttpUtils.getClientIp(request);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("getClientIp - should prioritize X-Forwarded-For over other headers")
    void getClientIp_WithMultipleHeaders_PrioritizesXForwardedFor() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(TestConstants.DEFAULT_IP_ADDRESS);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(TestConstants.ALTERNATE_IP_ADDRESS);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(TestConstants.THIRD_IP_ADDRESS);
        when(request.getRemoteAddr()).thenReturn(TestConstants.PROXY_IP);

        // When
        String result = HttpUtils.getClientIp(request);

        // Then
        assertThat(result)
            .isEqualTo(TestConstants.DEFAULT_IP_ADDRESS)
            .describedAs("X-Forwarded-For should take priority");
    }
}
