package com.elstner.airqualityapi.util;

/**
 * Centralized test constants to ensure consistency across all test classes.
 */
public final class TestConstants {

    private TestConstants() {
        // Utility class - prevent instantiation
    }

    // IP Addresses
    public static final String DEFAULT_IP_ADDRESS = "192.168.1.100";
    public static final String ALTERNATE_IP_ADDRESS = "192.168.1.101";
    public static final String THIRD_IP_ADDRESS = "192.168.1.102";
    public static final String IPV6_ADDRESS = "2001:db8::1";
    public static final String PROXY_IP = "10.0.0.1";

    // Station Constants
    public static final String DEFAULT_STATION_NAME = "Test Station";
    public static final String NEW_STATION_NAME = "New Station";

    // Measurement Constants
    public static final Float DEFAULT_TEMPERATURE = 22.0f;
    public static final Float DEFAULT_HUMIDITY = 55.0f;
    public static final Float DEFAULT_VOLTAGE = 3.3f;

    // Temperature Boundaries
    public static final Float MIN_VALID_TEMPERATURE = -99.0f;
    public static final Float MIN_INVALID_TEMPERATURE = -101.0f;
    public static final Float MAX_NORMAL_TEMPERATURE = 40.0f;

    // Humidity Boundaries
    public static final Float MIN_VALID_HUMIDITY = 0.1f;
    public static final Float MIN_INVALID_HUMIDITY = -1.0f;
    public static final Float MAX_HUMIDITY = 100.0f;

    // Voltage Constants
    public static final Float VOLTAGE_FULL = 3.3f;
    public static final Float VOLTAGE_LOW = 2.8f;
    public static final Float VOLTAGE_CRITICAL = 2.5f;

    // StationGroup Constants
    public static final String DEFAULT_GROUP_NAME = "Test Group";
    public static final String LIVING_ROOM_GROUP = "Living Room";
    public static final String BEDROOM_GROUP = "Bedroom";

    // Timezone
    public static final String TIMEZONE = "Europe/Vienna";

    // Retention
    public static final int DEFAULT_RETENTION_DAYS = 30;

    // HTTP Headers
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String X_REAL_IP = "X-Real-IP";

    // Test Data Scenarios
    public static final Float COLD_TEMPERATURE = 5.0f;
    public static final Float COLD_HUMIDITY = 40.0f;

    public static final Float HOT_TEMPERATURE = 35.0f;
    public static final Float HOT_HUMIDITY = 70.0f;

    public static final Float FREEZING_TEMPERATURE = 0.0f;
    public static final Float FREEZING_HUMIDITY = 100.0f;

    // Absolute Humidity Test Values (pre-calculated for validation)
    // Formula: AH = 6.112 * e^((17.67 * T) / (T + 243.5)) * (RH / 100)
    public static final Float EXPECTED_AH_22C_55RH = 11.49f;  // ~11.49 g/m³
    public static final Float EXPECTED_AH_0C_100RH = 4.85f;   // ~4.85 g/m³
    public static final Float EXPECTED_AH_35C_70RH = 27.72f;  // ~27.72 g/m³

    // Tolerance for float comparisons
    public static final Float FLOAT_TOLERANCE = 0.1f;
}
