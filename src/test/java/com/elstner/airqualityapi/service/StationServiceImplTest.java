package com.elstner.airqualityapi.service;

import com.elstner.airqualityapi.model.Station;
import com.elstner.airqualityapi.model.StationStatus;
import com.elstner.airqualityapi.repository.StationRepository;
import com.elstner.airqualityapi.util.TestConstants;
import com.elstner.airqualityapi.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StationServiceImpl - focuses on station auto-creation logic.
 * This is CRITICAL as it handles automatic station registration from IoT devices.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StationServiceImpl - Unit Tests")
class StationServiceImplTest {

    @Mock
    private StationRepository stationRepository;

    @InjectMocks
    private StationServiceImpl stationService;

    @BeforeEach
    void setUp() {
        reset(stationRepository);
    }

    @Test
    @DisplayName("getOrCreateStation - should return existing station when IP exists")
    void getOrCreateStation_WhenIpExists_ReturnsExistingStation() {
        // Given
        Station existingStation = TestDataBuilder.aStation()
            .withId(1L)
            .withIpAddress(TestConstants.DEFAULT_IP_ADDRESS)
            .withName("Living Room Sensor")
            .withStatus(StationStatus.ONLINE)
            .build();

        when(stationRepository.findByIpAddress(TestConstants.DEFAULT_IP_ADDRESS))
            .thenReturn(Optional.of(existingStation));

        // When
        Station result = stationService.getOrCreateStation(TestConstants.DEFAULT_IP_ADDRESS);

        // Then
        assertThat(result)
            .isNotNull()
            .isEqualTo(existingStation)
            .satisfies(station -> {
                assertThat(station.getId()).isEqualTo(1L);
                assertThat(station.getIpAddress()).isEqualTo(TestConstants.DEFAULT_IP_ADDRESS);
                assertThat(station.getName()).isEqualTo("Living Room Sensor");
                assertThat(station.getStatus()).isEqualTo(StationStatus.ONLINE);
            });

        verify(stationRepository, times(1)).findByIpAddress(TestConstants.DEFAULT_IP_ADDRESS);
        verify(stationRepository, never()).save(any(Station.class));
    }

    @Test
    @DisplayName("getOrCreateStation - should create new station when IP doesn't exist")
    void getOrCreateStation_WhenIpDoesNotExist_CreatesNewStation() {
        // Given
        when(stationRepository.findByIpAddress(TestConstants.ALTERNATE_IP_ADDRESS))
            .thenReturn(Optional.empty());

        Station savedStation = TestDataBuilder.aStation()
            .withId(2L)
            .withIpAddress(TestConstants.ALTERNATE_IP_ADDRESS)
            .withName(TestConstants.NEW_STATION_NAME)
            .withStatus(StationStatus.NEW)
            .build();

        when(stationRepository.save(any(Station.class)))
            .thenReturn(savedStation);

        // When
        Station result = stationService.getOrCreateStation(TestConstants.ALTERNATE_IP_ADDRESS);

        // Then
        assertThat(result)
            .isNotNull()
            .satisfies(station -> {
                assertThat(station.getId()).isEqualTo(2L);
                assertThat(station.getIpAddress()).isEqualTo(TestConstants.ALTERNATE_IP_ADDRESS);
                assertThat(station.getName()).isEqualTo(TestConstants.NEW_STATION_NAME);
                assertThat(station.getStatus()).isEqualTo(StationStatus.NEW);
            });

        verify(stationRepository, times(1)).findByIpAddress(TestConstants.ALTERNATE_IP_ADDRESS);
        verify(stationRepository, times(1)).save(any(Station.class));
    }

    @Test
    @DisplayName("getOrCreateStation - should save station with correct defaults when creating new")
    void getOrCreateStation_WhenCreatingNew_UsesCorrectDefaults() {
        // Given
        when(stationRepository.findByIpAddress(TestConstants.THIRD_IP_ADDRESS))
            .thenReturn(Optional.empty());

        ArgumentCaptor<Station> stationCaptor = ArgumentCaptor.forClass(Station.class);

        Station savedStation = TestDataBuilder.aStation()
            .withId(3L)
            .withIpAddress(TestConstants.THIRD_IP_ADDRESS)
            .asNew()
            .build();

        when(stationRepository.save(any(Station.class)))
            .thenReturn(savedStation);

        // When
        stationService.getOrCreateStation(TestConstants.THIRD_IP_ADDRESS);

        // Then
        verify(stationRepository).save(stationCaptor.capture());
        Station capturedStation = stationCaptor.getValue();

        assertThat(capturedStation)
            .satisfies(station -> {
                assertThat(station.getName()).isEqualTo(TestConstants.NEW_STATION_NAME);
                assertThat(station.getIpAddress()).isEqualTo(TestConstants.THIRD_IP_ADDRESS);
                assertThat(station.getStatus()).isEqualTo(StationStatus.NEW);
            });
    }

    @Test
    @DisplayName("getOrCreateStation - should handle multiple calls with same IP without duplicates")
    void getOrCreateStation_MultipleCallsSameIp_ReturnsConsistentResult() {
        // Given
        Station existingStation = TestDataBuilder.aStation()
            .withId(1L)
            .withIpAddress(TestConstants.DEFAULT_IP_ADDRESS)
            .build();

        when(stationRepository.findByIpAddress(TestConstants.DEFAULT_IP_ADDRESS))
            .thenReturn(Optional.of(existingStation));

        // When - Call multiple times
        Station result1 = stationService.getOrCreateStation(TestConstants.DEFAULT_IP_ADDRESS);
        Station result2 = stationService.getOrCreateStation(TestConstants.DEFAULT_IP_ADDRESS);
        Station result3 = stationService.getOrCreateStation(TestConstants.DEFAULT_IP_ADDRESS);

        // Then
        assertThat(result1).isEqualTo(existingStation);
        assertThat(result2).isEqualTo(existingStation);
        assertThat(result3).isEqualTo(existingStation);

        verify(stationRepository, times(3)).findByIpAddress(TestConstants.DEFAULT_IP_ADDRESS);
        verify(stationRepository, never()).save(any(Station.class));
    }

    @Test
    @DisplayName("getOrCreateStation - should create different stations for different IPs")
    void getOrCreateStation_DifferentIps_CreatesDifferentStations() {
        // Given
        when(stationRepository.findByIpAddress(anyString()))
            .thenReturn(Optional.empty());

        Station station1 = TestDataBuilder.aStation()
            .withId(1L)
            .withIpAddress(TestConstants.DEFAULT_IP_ADDRESS)
            .asNew()
            .build();

        Station station2 = TestDataBuilder.aStation()
            .withId(2L)
            .withIpAddress(TestConstants.ALTERNATE_IP_ADDRESS)
            .asNew()
            .build();

        when(stationRepository.save(any(Station.class)))
            .thenReturn(station1, station2);

        // When
        Station result1 = stationService.getOrCreateStation(TestConstants.DEFAULT_IP_ADDRESS);
        Station result2 = stationService.getOrCreateStation(TestConstants.ALTERNATE_IP_ADDRESS);

        // Then
        assertThat(result1.getIpAddress()).isEqualTo(TestConstants.DEFAULT_IP_ADDRESS);
        assertThat(result2.getIpAddress()).isEqualTo(TestConstants.ALTERNATE_IP_ADDRESS);
        assertThat(result1.getId()).isNotEqualTo(result2.getId());

        verify(stationRepository, times(2)).save(any(Station.class));
    }

    @Test
    @DisplayName("getOrCreateStation - should preserve existing station's custom name")
    void getOrCreateStation_ExistingStation_PreservesCustomName() {
        // Given
        String customName = "Kitchen Temperature Sensor";
        Station existingStation = TestDataBuilder.aStation()
            .withId(1L)
            .withIpAddress(TestConstants.DEFAULT_IP_ADDRESS)
            .withName(customName)
            .withStatus(StationStatus.ONLINE)
            .build();

        when(stationRepository.findByIpAddress(TestConstants.DEFAULT_IP_ADDRESS))
            .thenReturn(Optional.of(existingStation));

        // When
        Station result = stationService.getOrCreateStation(TestConstants.DEFAULT_IP_ADDRESS);

        // Then
        assertThat(result.getName())
            .isEqualTo(customName)
            .describedAs("Existing station's custom name should be preserved");

        verify(stationRepository, never()).save(any(Station.class));
    }

    @Test
    @DisplayName("getOrCreateStation - should preserve existing station's status")
    void getOrCreateStation_ExistingStation_PreservesStatus() {
        // Given
        Station existingStation = TestDataBuilder.aStation()
            .withId(1L)
            .withIpAddress(TestConstants.DEFAULT_IP_ADDRESS)
            .withStatus(StationStatus.MAINTENANCE)
            .build();

        when(stationRepository.findByIpAddress(TestConstants.DEFAULT_IP_ADDRESS))
            .thenReturn(Optional.of(existingStation));

        // When
        Station result = stationService.getOrCreateStation(TestConstants.DEFAULT_IP_ADDRESS);

        // Then
        assertThat(result.getStatus())
            .isEqualTo(StationStatus.MAINTENANCE)
            .describedAs("Existing station's status should be preserved");

        verify(stationRepository, never()).save(any(Station.class));
    }

    @Test
    @DisplayName("getOrCreateStation - should handle IPv6 addresses")
    void getOrCreateStation_WithIPv6_CreatesStation() {
        // Given
        when(stationRepository.findByIpAddress(TestConstants.IPV6_ADDRESS))
            .thenReturn(Optional.empty());

        Station savedStation = TestDataBuilder.aStation()
            .withId(4L)
            .withIpAddress(TestConstants.IPV6_ADDRESS)
            .asNew()
            .build();

        when(stationRepository.save(any(Station.class)))
            .thenReturn(savedStation);

        // When
        Station result = stationService.getOrCreateStation(TestConstants.IPV6_ADDRESS);

        // Then
        assertThat(result)
            .isNotNull()
            .satisfies(station -> {
                assertThat(station.getIpAddress()).isEqualTo(TestConstants.IPV6_ADDRESS);
                assertThat(station.getStatus()).isEqualTo(StationStatus.NEW);
            });

        verify(stationRepository, times(1)).save(any(Station.class));
    }

    @Test
    @DisplayName("getOrCreateStation - should retrieve station with existing group assignment")
    void getOrCreateStation_WithStationGroup_PreservesGroupAssignment() {
        // Given
        Station existingStation = TestDataBuilder.aStation()
            .withId(1L)
            .withIpAddress(TestConstants.DEFAULT_IP_ADDRESS)
            .withStationGroupId(100L)
            .withDisplayOrder(5)
            .build();

        when(stationRepository.findByIpAddress(TestConstants.DEFAULT_IP_ADDRESS))
            .thenReturn(Optional.of(existingStation));

        // When
        Station result = stationService.getOrCreateStation(TestConstants.DEFAULT_IP_ADDRESS);

        // Then
        assertThat(result)
            .satisfies(station -> {
                assertThat(station.getStationGroupId()).isEqualTo(100L);
                assertThat(station.getDisplayOrder()).isEqualTo(5);
            })
            .describedAs("Station's group assignment should be preserved");

        verify(stationRepository, never()).save(any(Station.class));
    }

    @Test
    @DisplayName("getOrCreateStation - repository interaction sequence for new station")
    void getOrCreateStation_NewStation_CorrectRepositoryInteractionSequence() {
        // Given
        when(stationRepository.findByIpAddress(TestConstants.DEFAULT_IP_ADDRESS))
            .thenReturn(Optional.empty());

        Station savedStation = TestDataBuilder.aStation()
            .withId(1L)
            .withIpAddress(TestConstants.DEFAULT_IP_ADDRESS)
            .asNew()
            .build();

        when(stationRepository.save(any(Station.class)))
            .thenReturn(savedStation);

        // When
        stationService.getOrCreateStation(TestConstants.DEFAULT_IP_ADDRESS);

        // Then - Verify interaction order
        var inOrder = inOrder(stationRepository);
        inOrder.verify(stationRepository).findByIpAddress(TestConstants.DEFAULT_IP_ADDRESS);
        inOrder.verify(stationRepository).save(any(Station.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    @DisplayName("getOrCreateStation - repository interaction sequence for existing station")
    void getOrCreateStation_ExistingStation_OnlyCallsFindByIpAddress() {
        // Given
        Station existingStation = TestDataBuilder.aStation()
            .withId(1L)
            .withIpAddress(TestConstants.DEFAULT_IP_ADDRESS)
            .build();

        when(stationRepository.findByIpAddress(TestConstants.DEFAULT_IP_ADDRESS))
            .thenReturn(Optional.of(existingStation));

        // When
        stationService.getOrCreateStation(TestConstants.DEFAULT_IP_ADDRESS);

        // Then - Verify only find is called, not save
        verify(stationRepository, times(1)).findByIpAddress(TestConstants.DEFAULT_IP_ADDRESS);
        verifyNoMoreInteractions(stationRepository);
    }
}
