package com.elstner.airqualityapi.model;

import com.elstner.airqualityapi.util.TestConstants;
import com.elstner.airqualityapi.util.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for Measurement model - focuses on absolute humidity calculation.
 * Formula: AH = 6.112 * e^((17.67 * T) / (T + 243.5)) * (RH / 100)
 */
@DisplayName("Measurement - Model Tests")
class MeasurementTest {

    @ParameterizedTest
    @CsvSource({
        "20.0, 50.0, 8.65",    // Normal room conditions
        "22.0, 55.0, 11.49",   // Slightly warmer and more humid
        "25.0, 60.0, 13.80",   // Warm conditions
        "0.0, 100.0, 4.85",    // Freezing, max humidity
        "-10.0, 80.0, 1.84",   // Below freezing
        "30.0, 30.0, 9.09",    // Hot, dry
        "35.0, 70.0, 27.72",   // Very hot and humid
        "15.0, 45.0, 5.83",    // Cool and moderate
        "5.0, 40.0, 2.71",     // Cold
        "28.0, 65.0, 18.06"    // Warm and humid
    })
    @DisplayName("Absolute humidity calculation - should match meteorological formula for various conditions")
    void absoluteHumidityCalculation_VariousConditions_MatchesFormula(
        float temperature,
        float humidity,
        float expectedAbsoluteHumidity
    ) {
        // Given
        Station station = TestDataBuilder.aStation().build();

        // When
        Measurement measurement = new Measurement(station, temperature, humidity, TestConstants.VOLTAGE_FULL);

        // Then
        assertThat(measurement.getAbsoluteHumidity())
            .isNotNull()
            .isCloseTo(expectedAbsoluteHumidity, within(0.2f))
            .describedAs("Absolute humidity for T=%.1f°C, RH=%.1f%% should be ~%.2f g/m³",
                temperature, humidity, expectedAbsoluteHumidity);
    }

    @Test
    @DisplayName("Constructor - should calculate absolute humidity automatically")
    void constructor_WithValidData_CalculatesAbsoluteHumidity() {
        // Given
        Station station = TestDataBuilder.aStation().build();
        float temperature = 22.0f;
        float humidity = 55.0f;

        // When
        Measurement measurement = new Measurement(station, temperature, humidity, 3.3f);

        // Then
        assertThat(measurement.getAbsoluteHumidity())
            .isNotNull()
            .isGreaterThan(0f)
            .describedAs("Absolute humidity should be calculated automatically in constructor");
    }

    @Test
    @DisplayName("Constructor - should handle null temperature gracefully")
    void constructor_WithNullTemperature_DoesNotCalculateAbsoluteHumidity() {
        // Given
        Station station = TestDataBuilder.aStation().build();

        // When
        Measurement measurement = new Measurement(station, null, 55.0f, 3.3f);

        // Then
        assertThat(measurement.getTemperature()).isNull();
        assertThat(measurement.getAbsoluteHumidity()).isNull();
    }

    @Test
    @DisplayName("Constructor - should handle null humidity gracefully")
    void constructor_WithNullHumidity_DoesNotCalculateAbsoluteHumidity() {
        // Given
        Station station = TestDataBuilder.aStation().build();

        // When
        Measurement measurement = new Measurement(station, 22.0f, null, 3.3f);

        // Then
        assertThat(measurement.getHumidity()).isNull();
        assertThat(measurement.getAbsoluteHumidity()).isNull();
    }

    @Test
    @DisplayName("Constructor - should set all fields correctly")
    void constructor_WithValidData_SetsAllFields() {
        // Given
        Station station = TestDataBuilder.aStation().build();
        float temperature = 22.0f;
        float humidity = 55.0f;
        float voltage = 3.3f;

        // When
        Measurement measurement = new Measurement(station, temperature, humidity, voltage);

        // Then
        assertThat(measurement.getStation()).isEqualTo(station);
        assertThat(measurement.getTemperature()).isEqualTo(temperature);
        assertThat(measurement.getHumidity()).isEqualTo(humidity);
        assertThat(measurement.getVoltage()).isEqualTo(voltage);
        assertThat(measurement.getAbsoluteHumidity()).isNotNull();
    }

    @Test
    @DisplayName("Default constructor - should initialize with default timestamp")
    void defaultConstructor_ShouldInitializeTimestamp() {
        // When
        Measurement measurement = new Measurement();

        // Then
        assertThat(measurement.getTimestamp())
            .isNotNull()
            .describedAs("Timestamp should be initialized by default");
    }

    @Test
    @DisplayName("Timestamp - should use Europe/Vienna timezone by default")
    void timestamp_ShouldUseViennaTimezone() {
        // Given
        Measurement measurement = TestDataBuilder.aMeasurement().build();

        // When
        ZonedDateTime timestamp = measurement.getTimestamp();

        // Then
        assertThat(timestamp)
            .isNotNull()
            .extracting(ZonedDateTime::getZone)
            .isEqualTo(ZoneId.of(TestConstants.TIMEZONE));
    }

    @Test
    @DisplayName("Setters - should allow updating temperature")
    void setTemperature_ShouldUpdateValue() {
        // Given
        Measurement measurement = TestDataBuilder.aMeasurement().build();
        float newTemperature = 25.0f;

        // When
        measurement.setTemperature(newTemperature);

        // Then
        assertThat(measurement.getTemperature()).isEqualTo(newTemperature);
    }

    @Test
    @DisplayName("Setters - should allow updating humidity")
    void setHumidity_ShouldUpdateValue() {
        // Given
        Measurement measurement = TestDataBuilder.aMeasurement().build();
        float newHumidity = 65.0f;

        // When
        measurement.setHumidity(newHumidity);

        // Then
        assertThat(measurement.getHumidity()).isEqualTo(newHumidity);
    }

    @Test
    @DisplayName("Setters - should allow updating voltage")
    void setVoltage_ShouldUpdateValue() {
        // Given
        Measurement measurement = TestDataBuilder.aMeasurement().build();
        float newVoltage = 2.8f;

        // When
        measurement.setVoltage(newVoltage);

        // Then
        assertThat(measurement.getVoltage()).isEqualTo(newVoltage);
    }

    @Test
    @DisplayName("Setters - should allow updating timestamp")
    void setTimestamp_ShouldUpdateValue() {
        // Given
        Measurement measurement = TestDataBuilder.aMeasurement().build();
        ZonedDateTime newTimestamp = ZonedDateTime.now(ZoneId.of(TestConstants.TIMEZONE)).minusHours(2);

        // When
        measurement.setTimestamp(newTimestamp);

        // Then
        assertThat(measurement.getTimestamp()).isEqualTo(newTimestamp);
    }

    @Test
    @DisplayName("Absolute humidity - extreme cold conditions")
    void absoluteHumidity_ExtremeCold_ReturnsLowValue() {
        // Given - Arctic winter conditions
        Station station = TestDataBuilder.aStation().build();
        float temperature = -40.0f;
        float humidity = 50.0f;

        // When
        Measurement measurement = new Measurement(station, temperature, humidity, 3.3f);

        // Then
        assertThat(measurement.getAbsoluteHumidity())
            .isNotNull()
            .isLessThan(1.0f)
            .describedAs("Absolute humidity should be very low in extreme cold");
    }

    @Test
    @DisplayName("Absolute humidity - tropical conditions")
    void absoluteHumidity_TropicalConditions_ReturnsHighValue() {
        // Given - Tropical humid conditions
        Station station = TestDataBuilder.aStation().build();
        float temperature = 35.0f;
        float humidity = 90.0f;

        // When
        Measurement measurement = new Measurement(station, temperature, humidity, 3.3f);

        // Then
        assertThat(measurement.getAbsoluteHumidity())
            .isNotNull()
            .isGreaterThan(30.0f)
            .describedAs("Absolute humidity should be very high in tropical conditions");
    }

    @Test
    @DisplayName("TestDataBuilder - should create measurement with correct defaults")
    void testDataBuilder_CreatesValidMeasurement() {
        // Given & When
        Measurement measurement = TestDataBuilder.aMeasurement().build();

        // Then
        assertThat(measurement)
            .isNotNull()
            .satisfies(m -> {
                assertThat(m.getTemperature()).isEqualTo(TestConstants.DEFAULT_TEMPERATURE);
                assertThat(m.getHumidity()).isEqualTo(TestConstants.DEFAULT_HUMIDITY);
                assertThat(m.getVoltage()).isEqualTo(TestConstants.DEFAULT_VOLTAGE);
                assertThat(m.getAbsoluteHumidity()).isNotNull();
                assertThat(m.getTimestamp()).isNotNull();
            });
    }

    @Test
    @DisplayName("TestDataBuilder - should allow customization")
    void testDataBuilder_AllowsCustomization() {
        // Given
        Station station = TestDataBuilder.aStation().build();
        float customTemp = 30.0f;
        float customHumidity = 70.0f;

        // When
        Measurement measurement = TestDataBuilder.aMeasurement()
            .withStation(station)
            .withTemperature(customTemp)
            .withHumidity(customHumidity)
            .build();

        // Then
        assertThat(measurement.getTemperature()).isEqualTo(customTemp);
        assertThat(measurement.getHumidity()).isEqualTo(customHumidity);
        assertThat(measurement.getStation()).isEqualTo(station);
    }

    @Test
    @DisplayName("TestDataBuilder - cold preset should create cold conditions")
    void testDataBuilder_ColdPreset_CreatesCorrectConditions() {
        // When
        Measurement measurement = TestDataBuilder.aMeasurement().cold().build();

        // Then
        assertThat(measurement.getTemperature()).isEqualTo(TestConstants.COLD_TEMPERATURE);
        assertThat(measurement.getHumidity()).isEqualTo(TestConstants.COLD_HUMIDITY);
    }

    @Test
    @DisplayName("TestDataBuilder - hot preset should create hot conditions")
    void testDataBuilder_HotPreset_CreatesCorrectConditions() {
        // When
        Measurement measurement = TestDataBuilder.aMeasurement().hot().build();

        // Then
        assertThat(measurement.getTemperature()).isEqualTo(TestConstants.HOT_TEMPERATURE);
        assertThat(measurement.getHumidity()).isEqualTo(TestConstants.HOT_HUMIDITY);
    }
}
