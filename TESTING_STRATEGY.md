# Comprehensive Testing Strategy for AirQuality API

**Version:** 1.0
**Last Updated:** 2025-11-05
**Target Coverage:** 80%+ (excluding generated code)

---

## Executive Summary

This document outlines a comprehensive testing strategy for the AirQuality API, a Spring Boot REST API that manages IoT weather station measurements. Currently, the project has **minimal test coverage (~2%)** with only a single context load test.

### Current State:
- ✅ JUnit 5, Mockito, AssertJ available
- ✅ H2 in-memory database configured for tests
- ❌ **0% coverage** across all layers (controllers, services, repositories, mappers)
- ❌ No integration tests
- ❌ No WebSocket tests
- ❌ No scheduled task tests

### Target State:
- ✅ 80%+ code coverage
- ✅ Comprehensive unit tests for all layers
- ✅ Integration tests with database
- ✅ WebSocket and scheduled task tests
- ✅ CI/CD pipeline with automated testing

---

## Table of Contents

1. [Testing Principles](#testing-principles)
2. [Test Types & Layers](#test-types--layers)
3. [Tooling & Dependencies](#tooling--dependencies)
4. [Implementation Roadmap](#implementation-roadmap)
5. [Layer-Specific Testing Patterns](#layer-specific-testing-patterns)
6. [Test Configuration](#test-configuration)
7. [Code Coverage Targets](#code-coverage-targets)
8. [CI/CD Integration](#cicd-integration)
9. [Best Practices](#best-practices)

---

## Testing Principles

### Core Principles

1. **Test Pyramid Approach**
   - **70% Unit Tests** - Fast, isolated, focused on single components
   - **20% Integration Tests** - Test component interactions
   - **10% End-to-End Tests** - Full workflow validation

2. **Fail Fast Philosophy**
   - Tests should fail immediately when code breaks
   - Clear, descriptive error messages
   - Run tests on every commit

3. **Test Isolation**
   - Each test is independent and can run in any order
   - No shared mutable state between tests
   - Use `@DirtiesContext` sparingly (performance impact)

4. **Realistic Test Data**
   - Use test data builders/factories
   - Avoid magic numbers and strings
   - Test edge cases and boundary conditions

5. **Maintainability**
   - Tests should be as simple as possible
   - Avoid testing implementation details
   - Follow DRY principle with test utilities

---

## Test Types & Layers

### 1. Unit Tests (70% of tests)

**Purpose:** Test individual components in isolation with mocked dependencies

#### Components to Test:

##### A. Service Layer
- **StationServiceImpl**
  - `getOrCreateStation()` - test station creation and retrieval
  - Test edge cases: duplicate IPs, null values, concurrent requests

- **MeasurementPublisher**
  - WebSocket publishing logic
  - Error handling when publishing fails

- **MeasurementCleanupService**
  - Scheduled cleanup logic
  - Retention period calculations
  - Manual cleanup with custom days

##### B. MapStruct Mappers
- **MeasurementMapper**
  - Entity to DTO conversion accuracy
  - Null field handling
  - List conversions

- **StationMapper**
  - Station ↔ StationDto conversion
  - Complex mapping: StationWithMeasurementsDto
  - Nested object mapping

- **StationGroupMapper**
  - StationGroup ↔ StationGroupDto conversion

##### C. Utility Classes
- **HttpUtils**
  - `getClientIp()` with various header combinations
  - X-Forwarded-For parsing (single/multiple proxies)
  - IPv4/IPv6 handling
  - Fallback to RemoteAddr

##### D. Domain Models
- **Measurement**
  - Absolute humidity calculation accuracy
  - Constructor logic validation
  - Timezone handling (Europe/Vienna)

- **Station**
  - Default status initialization (NEW)
  - Station-to-measurements relationship

##### E. Validators
- Temperature validation (> -100°C)
- Humidity validation (> 0%)
- Voltage range validation

### 2. Integration Tests (20% of tests)

**Purpose:** Test component interactions with real infrastructure (database, WebSocket)

#### Types of Integration Tests:

##### A. Repository Tests (`@DataJpaTest`)
- **MeasurementRepository**
  - `findByStationAndTimestampAfterOrderByTimestampDesc()`
  - `deleteByTimestampBefore()`
  - Cascade operations

- **StationRepository**
  - `findByIpAddress()`
  - `findByStatus()`
  - `findLatestMeasurementByStation()`
  - Custom query correctness

- **StationGroupRepository**
  - Basic CRUD operations
  - Foreign key constraints

##### B. Controller Tests (`@WebMvcTest`)
- **MeasurementController**
  - `GET /measurements` - pagination, filtering
  - `GET /measurements/{id}` - single retrieval, 404 handling
  - `POST /measurements` - creation, validation, 400 errors
  - IP extraction from headers
  - WebSocket publish verification

- **StationController**
  - All CRUD endpoints
  - `/stations/new` - NEW status filtering
  - `/stations/{id}/measurements` - nested resource retrieval
  - `/stations/latestMeasurement` - complex query
  - Status update via PATCH
  - 404 handling for non-existent stations

- **StationGroupController**
  - Full CRUD operations
  - Validation errors

- **VersionController**
  - Version endpoint response

##### C. Service Integration Tests (`@SpringBootTest`)
- **End-to-end measurement flow**
  1. POST measurement from new IP
  2. Verify station auto-creation
  3. Verify measurement saved
  4. Verify WebSocket publish

- **Station lifecycle**
  - Create → Update → Delete
  - Status transitions

- **Scheduled cleanup**
  - Verify old measurements deleted
  - Verify recent measurements retained

##### D. WebSocket Tests
- **Real-time messaging**
  - Connect to `/ws` endpoint
  - Subscribe to `/topic/measurements`
  - Verify broadcast on new measurement
  - Test SockJS fallback

### 3. End-to-End Tests (10% of tests)

**Purpose:** Test complete user workflows

#### Scenarios:

1. **IoT Device Measurement Submission**
   - Device POSTs measurement → Station auto-created → Data saved → WebSocket broadcast

2. **Station Management Workflow**
   - Create station group → Create station → Assign to group → Update status → Retrieve with measurements

3. **Data Retrieval & Filtering**
   - Query measurements by station → Filter by date range → Verify ordering

4. **Cleanup Workflow**
   - Insert old measurements → Run cleanup → Verify deletion → Verify recent data retained

---

## Tooling & Dependencies

### Required Dependencies (add to `pom.xml`)

```xml
<!-- Testing Dependencies -->
<dependencies>
    <!-- Already present: spring-boot-starter-test (JUnit 5, Mockito, AssertJ) -->

    <!-- REST API Testing -->
    <dependency>
        <groupId>io.rest-assured</groupId>
        <artifactId>rest-assured</artifactId>
        <version>5.4.0</version>
        <scope>test</scope>
    </dependency>

    <!-- TestContainers for realistic database testing -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>

    <!-- WebSocket Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Code Coverage -->
    <dependency>
        <groupId>org.jacoco</groupId>
        <artifactId>jacoco-maven-plugin</artifactId>
        <version>0.8.11</version>
        <scope>test</scope>
    </dependency>

    <!-- Awaitility for async testing -->
    <dependency>
        <groupId>org.awaitility</groupId>
        <artifactId>awaitility</artifactId>
        <version>4.2.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Maven Plugins

```xml
<build>
    <plugins>
        <!-- JaCoCo Code Coverage -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.11</version>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
                <execution>
                    <id>check</id>
                    <phase>verify</phase>
                    <goals>
                        <goal>check</goal>
                    </goals>
                    <configuration>
                        <rules>
                            <rule>
                                <element>PACKAGE</element>
                                <limits>
                                    <limit>
                                        <counter>LINE</counter>
                                        <value>COVEREDRATIO</value>
                                        <minimum>0.80</minimum>
                                    </limit>
                                </limits>
                            </rule>
                        </rules>
                    </configuration>
                </execution>
            </executions>
        </plugin>

        <!-- Surefire for test execution -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.5</version>
            <configuration>
                <includes>
                    <include>**/*Test.java</include>
                    <include>**/*Tests.java</include>
                </includes>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1) - PRIORITY

**Goal:** Set up testing infrastructure and critical unit tests

1. **Add Dependencies**
   - ✅ Update `pom.xml` with REST Assured, TestContainers, JaCoCo
   - ✅ Configure JaCoCo plugin for coverage reports

2. **Test Utilities**
   - Create `TestDataBuilder` class for consistent test data
   - Create `TestConstants` for shared values (IPs, temperatures, etc.)

3. **Critical Unit Tests**
   - ✅ `HttpUtilsTest` - IP extraction (CRITICAL for station identification)
   - ✅ `MeasurementTest` - Absolute humidity calculation
   - ✅ `StationServiceImplTest` - Station auto-creation logic

### Phase 2: Repository & Service Layer (Week 2)

**Goal:** Comprehensive service and repository coverage

4. **Repository Tests**
   - ✅ `MeasurementRepositoryTest` with `@DataJpaTest` + H2
   - ✅ `StationRepositoryTest` with custom queries
   - ✅ `StationGroupRepositoryTest` with FK constraints

5. **Service Tests**
   - ✅ `MeasurementCleanupServiceTest` - scheduled task + manual cleanup
   - ✅ `MeasurementPublisherTest` - WebSocket publishing

6. **Mapper Tests**
   - ✅ `MeasurementMapperTest` - entity ↔ DTO conversions
   - ✅ `StationMapperTest` - complex nested mappings
   - ✅ `StationGroupMapperTest`

### Phase 3: Controller Layer (Week 3)

**Goal:** REST API endpoint validation

7. **Controller Tests** (`@WebMvcTest` + MockMvc)
   - ✅ `MeasurementControllerTest` - all endpoints + validation
   - ✅ `StationControllerTest` - CRUD + complex queries
   - ✅ `StationGroupControllerTest` - CRUD operations
   - ✅ `VersionControllerTest` - version endpoint

### Phase 4: Integration Tests (Week 4)

**Goal:** Test component interactions

8. **Service Integration Tests** (`@SpringBootTest`)
   - ✅ End-to-end measurement flow (IoT device → DB → WebSocket)
   - ✅ Station lifecycle (create → update → delete)

9. **WebSocket Integration Tests**
   - ✅ Connect to `/ws` endpoint
   - ✅ Subscribe to `/topic/measurements`
   - ✅ Verify broadcasts

10. **Scheduled Task Tests**
    - ✅ Test `@Scheduled` cleanup execution
    - ✅ Verify retention period logic

### Phase 5: TestContainers & E2E (Week 5)

**Goal:** Realistic testing with PostgreSQL

11. **TestContainers Setup**
    - ✅ PostgreSQL container configuration
    - ✅ Flyway migration validation in tests
    - ✅ Replace H2 tests with PostgreSQL where needed

12. **End-to-End Tests**
    - ✅ Complete IoT workflow
    - ✅ Station management workflow
    - ✅ Data retrieval & filtering

### Phase 6: Polish & CI/CD (Week 6)

**Goal:** Refinement and automation

13. **Code Coverage**
    - ✅ Run JaCoCo report → identify gaps → fill missing tests
    - ✅ Achieve 80%+ coverage target

14. **CI/CD Integration**
    - ✅ GitHub Actions / Jenkins / GitLab CI configuration
    - ✅ Run tests on every PR
    - ✅ Block merge if tests fail or coverage drops

15. **Documentation**
    - ✅ Document test patterns in README
    - ✅ Create contribution guidelines with testing requirements

---

## Layer-Specific Testing Patterns

### 1. Unit Test Pattern: Service Layer

**Example: StationServiceImplTest**

```java
package com.elstner.airqualityapi.service;

import com.elstner.airqualityapi.model.Station;
import com.elstner.airqualityapi.model.StationStatus;
import com.elstner.airqualityapi.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StationService - Unit Tests")
class StationServiceImplTest {

    @Mock
    private StationRepository stationRepository;

    @InjectMocks
    private StationServiceImpl stationService;

    private static final String TEST_IP = "192.168.1.100";

    @Test
    @DisplayName("getOrCreateStation - should return existing station when IP exists")
    void getOrCreateStation_WhenIpExists_ReturnsExistingStation() {
        // Given
        Station existingStation = new Station();
        existingStation.setId(1L);
        existingStation.setIpAddress(TEST_IP);
        existingStation.setStatus(StationStatus.ACTIVE);

        when(stationRepository.findByIpAddress(TEST_IP))
            .thenReturn(Optional.of(existingStation));

        // When
        Station result = stationService.getOrCreateStation(TEST_IP);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getIpAddress()).isEqualTo(TEST_IP);
        assertThat(result.getStatus()).isEqualTo(StationStatus.ACTIVE);

        verify(stationRepository, times(1)).findByIpAddress(TEST_IP);
        verify(stationRepository, never()).save(any());
    }

    @Test
    @DisplayName("getOrCreateStation - should create new station when IP doesn't exist")
    void getOrCreateStation_WhenIpDoesNotExist_CreatesNewStation() {
        // Given
        when(stationRepository.findByIpAddress(TEST_IP))
            .thenReturn(Optional.empty());

        Station newStation = new Station();
        newStation.setId(2L);
        newStation.setIpAddress(TEST_IP);
        newStation.setStatus(StationStatus.NEW);
        newStation.setName("New Station");

        when(stationRepository.save(any(Station.class)))
            .thenReturn(newStation);

        // When
        Station result = stationService.getOrCreateStation(TEST_IP);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getIpAddress()).isEqualTo(TEST_IP);
        assertThat(result.getStatus()).isEqualTo(StationStatus.NEW);
        assertThat(result.getName()).isEqualTo("New Station");

        verify(stationRepository, times(1)).findByIpAddress(TEST_IP);
        verify(stationRepository, times(1)).save(any(Station.class));
    }

    @Test
    @DisplayName("getOrCreateStation - should handle null IP address gracefully")
    void getOrCreateStation_WhenIpIsNull_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> stationService.getOrCreateStation(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("IP address cannot be null");

        verify(stationRepository, never()).findByIpAddress(any());
        verify(stationRepository, never()).save(any());
    }
}
```

---

### 2. Integration Test Pattern: Repository Layer

**Example: MeasurementRepositoryTest**

```java
package com.elstner.airqualityapi.repository;

import com.elstner.airqualityapi.model.Measurement;
import com.elstner.airqualityapi.model.Station;
import com.elstner.airqualityapi.model.StationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("MeasurementRepository - Integration Tests")
class MeasurementRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MeasurementRepository measurementRepository;

    private Station testStation;

    @BeforeEach
    void setUp() {
        // Create test station
        testStation = new Station();
        testStation.setIpAddress("192.168.1.100");
        testStation.setName("Test Station");
        testStation.setStatus(StationStatus.ACTIVE);
        testStation = entityManager.persist(testStation);
        entityManager.flush();
    }

    @Test
    @DisplayName("findByStationAndTimestampAfterOrderByTimestampDesc - should return measurements after date")
    void findByStationAndTimestampAfter_ReturnsFilteredMeasurements() {
        // Given
        ZonedDateTime cutoffTime = ZonedDateTime.now(ZoneId.of("Europe/Vienna")).minusHours(2);

        // Old measurement (before cutoff)
        Measurement oldMeasurement = createMeasurement(testStation, 20.0, 50.0, cutoffTime.minusHours(1));
        entityManager.persist(oldMeasurement);

        // Recent measurements (after cutoff)
        Measurement recentMeasurement1 = createMeasurement(testStation, 22.0, 55.0, cutoffTime.plusHours(1));
        Measurement recentMeasurement2 = createMeasurement(testStation, 23.0, 60.0, cutoffTime.plusHours(2));
        entityManager.persist(recentMeasurement1);
        entityManager.persist(recentMeasurement2);
        entityManager.flush();

        // When
        List<Measurement> results = measurementRepository
            .findByStationAndTimestampAfterOrderByTimestampDesc(testStation, cutoffTime);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getTemperature()).isEqualTo(23.0); // Most recent first
        assertThat(results.get(1).getTemperature()).isEqualTo(22.0);
    }

    @Test
    @DisplayName("deleteByTimestampBefore - should delete old measurements")
    void deleteByTimestampBefore_DeletesOldRecords() {
        // Given
        ZonedDateTime cutoffTime = ZonedDateTime.now(ZoneId.of("Europe/Vienna")).minusDays(30);

        Measurement oldMeasurement = createMeasurement(testStation, 20.0, 50.0, cutoffTime.minusDays(1));
        Measurement recentMeasurement = createMeasurement(testStation, 22.0, 55.0, cutoffTime.plusDays(1));
        entityManager.persist(oldMeasurement);
        entityManager.persist(recentMeasurement);
        entityManager.flush();

        // When
        int deletedCount = measurementRepository.deleteByTimestampBefore(cutoffTime);

        // Then
        assertThat(deletedCount).isEqualTo(1);
        assertThat(measurementRepository.findAll()).hasSize(1);
        assertThat(measurementRepository.findAll().get(0).getTemperature()).isEqualTo(22.0);
    }

    private Measurement createMeasurement(Station station, double temp, double humidity, ZonedDateTime timestamp) {
        Measurement m = new Measurement();
        m.setStation(station);
        m.setTemperature(temp);
        m.setHumidity(humidity);
        m.setVoltage(3.3);
        m.setTimestamp(timestamp);
        return m;
    }
}
```

---

### 3. Controller Test Pattern: WebMvcTest

**Example: MeasurementControllerTest**

```java
package com.elstner.airqualityapi.controller;

import com.elstner.airqualityapi.dto.MeasurementDto;
import com.elstner.airqualityapi.model.Measurement;
import com.elstner.airqualityapi.model.Station;
import com.elstner.airqualityapi.repository.MeasurementRepository;
import com.elstner.airqualityapi.service.MeasurementPublisher;
import com.elstner.airqualityapi.service.StationService;
import com.elstner.airqualityapi.mapper.MeasurementMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MeasurementController.class)
@DisplayName("MeasurementController - Integration Tests")
class MeasurementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MeasurementRepository measurementRepository;

    @MockBean
    private StationService stationService;

    @MockBean
    private MeasurementPublisher measurementPublisher;

    @MockBean
    private MeasurementMapper measurementMapper;

    @Test
    @DisplayName("POST /measurements - should create measurement and auto-register station")
    void createMeasurement_WithNewStation_ReturnsCreated() throws Exception {
        // Given
        String testIp = "192.168.1.100";
        Station mockStation = new Station();
        mockStation.setId(1L);
        mockStation.setIpAddress(testIp);

        Measurement savedMeasurement = new Measurement();
        savedMeasurement.setId(UUID.randomUUID());
        savedMeasurement.setStation(mockStation);
        savedMeasurement.setTemperature(22.5);
        savedMeasurement.setHumidity(55.0);
        savedMeasurement.setVoltage(3.3);

        MeasurementDto responseDto = new MeasurementDto();
        responseDto.setTemperature(22.5);
        responseDto.setHumidity(55.0);

        when(stationService.getOrCreateStation(testIp)).thenReturn(mockStation);
        when(measurementRepository.save(any(Measurement.class))).thenReturn(savedMeasurement);
        when(measurementMapper.toDto(any(Measurement.class))).thenReturn(responseDto);

        // When & Then
        mockMvc.perform(post("/measurements")
                .header("X-Forwarded-For", testIp)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "temperature": 22.5,
                        "humidity": 55.0,
                        "voltage": 3.3
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.temperature").value(22.5))
            .andExpect(jsonPath("$.humidity").value(55.0));

        verify(stationService, times(1)).getOrCreateStation(testIp);
        verify(measurementRepository, times(1)).save(any(Measurement.class));
        verify(measurementPublisher, times(1)).publishMeasurementUpdate(any());
    }

    @Test
    @DisplayName("POST /measurements - should reject invalid temperature")
    void createMeasurement_WithInvalidTemperature_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/measurements")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "temperature": -150.0,
                        "humidity": 55.0,
                        "voltage": 3.3
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Temperature must be greater than -100°C"));

        verify(measurementRepository, never()).save(any());
    }

    @Test
    @DisplayName("GET /measurements/{id} - should return 404 when measurement not found")
    void getMeasurement_WhenNotFound_Returns404() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(measurementRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/measurements/" + nonExistentId))
            .andExpect(status().isNotFound());
    }
}
```

---

### 4. WebSocket Test Pattern

**Example: MeasurementWebSocketTest**

```java
package com.elstner.airqualityapi.websocket;

import com.elstner.airqualityapi.dto.MeasurementWithStationDto;
import com.elstner.airqualityapi.service.MeasurementPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("WebSocket - Integration Tests")
class MeasurementWebSocketTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MeasurementPublisher measurementPublisher;

    private WebSocketStompClient stompClient;
    private String wsUrl;

    @BeforeEach
    void setUp() {
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);

        stompClient = new WebSocketStompClient(sockJsClient);
        wsUrl = "ws://localhost:" + port + "/ws";
    }

    @Test
    @DisplayName("Should receive measurement update via WebSocket")
    void whenMeasurementPublished_ThenSubscriberReceivesUpdate() throws Exception {
        // Given
        CompletableFuture<MeasurementWithStationDto> resultFuture = new CompletableFuture<>();

        StompSession session = stompClient
            .connect(wsUrl, new StompSessionHandlerAdapter() {})
            .get(5, TimeUnit.SECONDS);

        session.subscribe("/topic/measurements", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MeasurementWithStationDto.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                resultFuture.complete((MeasurementWithStationDto) payload);
            }
        });

        // When
        MeasurementWithStationDto testData = new MeasurementWithStationDto();
        testData.setTemperature(22.5);
        testData.setHumidity(55.0);
        measurementPublisher.publishMeasurementUpdate(testData);

        // Then
        MeasurementWithStationDto received = resultFuture.get(5, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.getTemperature()).isEqualTo(22.5);
        assertThat(received.getHumidity()).isEqualTo(55.0);
    }
}
```

---

### 5. Scheduled Task Test Pattern

**Example: MeasurementCleanupServiceTest**

```java
package com.elstner.airqualityapi.service;

import com.elstner.airqualityapi.model.Measurement;
import com.elstner.airqualityapi.repository.MeasurementRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.TestPropertySource;

import java.time.ZonedDateTime;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@EnableScheduling
@TestPropertySource(properties = {
    "measurement.cleanup.enabled=true",
    "measurement.cleanup.retention-days=30",
    "measurement.cleanup.cron=*/5 * * * * *"  // Every 5 seconds for testing
})
@DisplayName("MeasurementCleanupService - Scheduled Task Tests")
class MeasurementCleanupServiceScheduledTest {

    @MockBean
    private MeasurementRepository measurementRepository;

    @Test
    @DisplayName("Scheduled cleanup should execute automatically")
    void scheduledCleanup_ShouldExecute() {
        // Given
        when(measurementRepository.deleteByTimestampBefore(any(ZonedDateTime.class)))
            .thenReturn(10);

        // When
        await()
            .atMost(10, java.util.concurrent.TimeUnit.SECONDS)
            .untilAsserted(() ->
                verify(measurementRepository, atLeastOnce())
                    .deleteByTimestampBefore(any(ZonedDateTime.class))
            );

        // Then - verification happens in await()
    }
}
```

---

### 6. Utility Test Pattern

**Example: HttpUtilsTest**

```java
package com.elstner.airqualityapi.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("HttpUtils - Unit Tests")
class HttpUtilsTest {

    @Test
    @DisplayName("getClientIp - should extract IP from X-Forwarded-For header")
    void getClientIp_WithForwardedHeader_ReturnsFirstIp() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100, 10.0.0.1");

        // When
        String result = HttpUtils.getClientIp(request);

        // Then
        assertThat(result).isEqualTo("192.168.1.100");
    }

    @Test
    @DisplayName("getClientIp - should fallback to RemoteAddr when no header")
    void getClientIp_WithoutForwardedHeader_ReturnsRemoteAddr() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.200");

        // When
        String result = HttpUtils.getClientIp(request);

        // Then
        assertThat(result).isEqualTo("192.168.1.200");
    }

    @ParameterizedTest
    @CsvSource({
        "'192.168.1.100', '192.168.1.100'",
        "'10.0.0.1, 192.168.1.100', '10.0.0.1'",
        "'2001:db8::1', '2001:db8::1'",  // IPv6
        "' 192.168.1.100 ', '192.168.1.100'"  // Trimming
    })
    @DisplayName("getClientIp - should handle various IP formats")
    void getClientIp_VariousFormats(String forwardedHeader, String expectedIp) {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(forwardedHeader);

        // When
        String result = HttpUtils.getClientIp(request);

        // Then
        assertThat(result).isEqualTo(expectedIp);
    }
}
```

---

### 7. Model Test Pattern

**Example: MeasurementTest**

```java
package com.elstner.airqualityapi.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("Measurement - Model Tests")
class MeasurementTest {

    @ParameterizedTest
    @CsvSource({
        "20.0, 50.0, 8.65",   // Normal room conditions
        "25.0, 60.0, 13.8",   // Warmer, more humid
        "0.0, 100.0, 4.85",   // Freezing, max humidity
        "-10.0, 80.0, 1.84",  // Below freezing
        "30.0, 30.0, 9.09"    // Hot, dry
    })
    @DisplayName("Absolute humidity calculation - should match meteorological formula")
    void absoluteHumidityCalculation_VariousConditions(
        double temperature,
        double humidity,
        double expectedAbsoluteHumidity
    ) {
        // Given
        Measurement measurement = new Measurement();
        measurement.setTemperature(temperature);
        measurement.setHumidity(humidity);
        measurement.setVoltage(3.3);

        // When
        // Absolute humidity is calculated in constructor/setter
        double result = measurement.getAbsoluteHumidity();

        // Then
        // Formula: AH = 6.112 * e^((17.67 * T) / (T + 243.5)) * (RH / 100)
        assertThat(result).isCloseTo(expectedAbsoluteHumidity, within(0.1));
    }

    @Test
    @DisplayName("Timestamp - should use Europe/Vienna timezone")
    void timestamp_ShouldUseViennaTimezone() {
        // Given
        Measurement measurement = new Measurement();
        measurement.setTemperature(20.0);
        measurement.setHumidity(50.0);

        // When
        // Timestamp defaults to ZonedDateTime.now() in Europe/Vienna

        // Then
        assertThat(measurement.getTimestamp()).isNotNull();
        assertThat(measurement.getTimestamp().getZone().getId()).isEqualTo("Europe/Vienna");
    }
}
```

---

## Test Configuration

### Test Properties (`src/test/resources/application-test.properties`)

```properties
# Application Metadata
spring.application.name=AirQualityApi
spring.application.version=0.5.1

# H2 In-Memory Database for fast tests
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# H2 Dialect with PostgreSQL compatibility
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

# Flyway (enable for migration tests)
spring.flyway.enabled=false
spring.flyway.locations=classpath:db/migration

# Disable scheduled tasks by default (enable per test)
measurement.cleanup.enabled=false
measurement.cleanup.retention-days=30
measurement.cleanup.cron=-  # Disabled

# WebSocket (for integration tests)
spring.websocket.allowed-origins=*

# Logging
logging.level.com.elstner.airqualityapi=DEBUG
logging.level.org.springframework.test=INFO
```

### TestContainers Configuration (PostgreSQL)

**Base Test Class: `AbstractPostgreSQLTest`**

```java
package com.elstner.airqualityapi.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
public abstract class AbstractPostgreSQLTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("airquality_test")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true);  // Reuse container across tests for performance

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.flyway.enabled", () -> true);  // Enable Flyway for PostgreSQL tests
    }
}
```

**Usage Example:**

```java
@DisplayName("Station Repository - PostgreSQL Integration Tests")
class StationRepositoryPostgreSQLTest extends AbstractPostgreSQLTest {

    @Autowired
    private StationRepository stationRepository;

    @Test
    void testCustomQueryWithPostgreSQL() {
        // Test runs with real PostgreSQL database
    }
}
```

---

## Code Coverage Targets

### Overall Targets

| Component | Target Coverage | Priority |
|-----------|-----------------|----------|
| **Controllers** | 85%+ | High |
| **Services** | 90%+ | Critical |
| **Repositories** | 80%+ | Medium |
| **Mappers** | 85%+ | High |
| **Models** | 70%+ | Medium |
| **Utilities** | 95%+ | Critical |
| **Configuration** | 50%+ | Low |
| **Overall** | 80%+ | - |

### Coverage Exclusions

Exclude from coverage reports:
- Generated MapStruct implementations (`*Impl.java` in mapper package)
- Main application class (`AirQualityApiApplication.java`)
- Configuration classes (WebSocketConfig, CorsConfig)
- DTOs (simple POJOs with no logic)

**JaCoCo Exclusion Configuration:**

```xml
<configuration>
    <excludes>
        <exclude>**/mapper/*Impl.class</exclude>
        <exclude>**/AirQualityApiApplication.class</exclude>
        <exclude>**/dto/*.class</exclude>
        <exclude>**/config/*.class</exclude>
    </excludes>
</configuration>
```

---

## CI/CD Integration

### GitHub Actions Workflow

**File: `.github/workflows/tests.yml`**

```yaml
name: Tests & Coverage

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_DB: airquality_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Run tests with coverage
        run: ./mvnw clean verify

      - name: Generate JaCoCo report
        run: ./mvnw jacoco:report

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./target/site/jacoco/jacoco.xml
          fail_ci_if_error: true

      - name: Check coverage threshold
        run: ./mvnw jacoco:check

      - name: Comment PR with coverage
        if: github.event_name == 'pull_request'
        uses: madrapps/jacoco-report@v1.6.1
        with:
          paths: target/site/jacoco/jacoco.xml
          token: ${{ secrets.GITHUB_TOKEN }}
          min-coverage-overall: 80
          min-coverage-changed-files: 80
```

### Build Failure on Coverage Drop

Configure Maven to fail builds below 80% coverage:

```xml
<execution>
    <id>check</id>
    <goals>
        <goal>check</goal>
    </goals>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</execution>
```

---

## Best Practices

### 1. Test Naming Convention

**Pattern:** `methodName_StateUnderTest_ExpectedBehavior`

```java
// Good
void getOrCreateStation_WhenIpExists_ReturnsExistingStation()
void createMeasurement_WithInvalidTemperature_ReturnsBadRequest()

// Bad
void testGetStation()
void test1()
```

### 2. Use AssertJ for Fluent Assertions

```java
// Good - AssertJ
assertThat(result)
    .isNotNull()
    .extracting(Station::getIpAddress, Station::getStatus)
    .containsExactly("192.168.1.100", StationStatus.NEW);

// Avoid - JUnit assertions
assertTrue(result != null);
assertEquals("192.168.1.100", result.getIpAddress());
```

### 3. Test Data Builders

**Create reusable builders:**

```java
public class TestDataBuilder {
    public static Station.StationBuilder aStation() {
        return Station.builder()
            .ipAddress("192.168.1.100")
            .name("Test Station")
            .status(StationStatus.ACTIVE);
    }

    public static Measurement.MeasurementBuilder aMeasurement() {
        return Measurement.builder()
            .temperature(22.0)
            .humidity(55.0)
            .voltage(3.3)
            .timestamp(ZonedDateTime.now(ZoneId.of("Europe/Vienna")));
    }
}

// Usage
Station station = TestDataBuilder.aStation()
    .ipAddress("192.168.1.200")
    .build();
```

### 4. Test Organization

```
src/test/java/
├── com.elstner.airqualityapi/
│   ├── controller/           # @WebMvcTest
│   ├── service/              # @ExtendWith(MockitoExtension)
│   ├── repository/           # @DataJpaTest
│   ├── mapper/               # Unit tests
│   ├── model/                # Unit tests
│   ├── util/                 # Unit tests
│   ├── integration/          # @SpringBootTest (end-to-end)
│   └── config/               # Test configuration
│       ├── AbstractPostgreSQLTest.java
│       └── TestDataBuilder.java
```

### 5. Mock vs Real Dependencies

**Use mocks for:**
- External services (not in scope)
- Slow operations
- Non-deterministic behavior

**Use real implementations for:**
- Repositories (with H2/TestContainers)
- Mappers (MapStruct generated code)
- Controllers (with MockMvc)

### 6. Test Isolation

```java
@BeforeEach
void setUp() {
    // Reset mocks
    reset(stationRepository, measurementPublisher);

    // Clear in-memory database (if using @DataJpaTest)
    entityManager.clear();
}
```

### 7. Parameterized Tests for Multiple Inputs

```java
@ParameterizedTest
@CsvSource({
    "-150.0, false",  // Too cold
    "-100.0, true",   // Boundary
    "20.0, true",     // Normal
    "60.0, true"      // Hot
})
void validateTemperature(double temperature, boolean expected) {
    assertThat(MeasurementValidator.isValid(temperature)).isEqualTo(expected);
}
```

### 8. Avoid Test Interdependence

```java
// Bad - tests depend on execution order
@Test
@Order(1)
void createStation() { ... }

@Test
@Order(2)
void updateStation() { ... }  // Assumes station from test 1 exists

// Good - each test is independent
@Test
void createStation() {
    Station station = createTestStation();
    // ...
}

@Test
void updateStation() {
    Station station = createTestStation();  // Create own test data
    // ...
}
```

---

## Success Metrics

### Phase Completion Criteria

**Phase 1 (Foundation):**
- ✅ All dependencies added
- ✅ TestDataBuilder created
- ✅ HttpUtils, Measurement, StationService tests passing
- ✅ JaCoCo report generated

**Phase 2 (Repositories & Services):**
- ✅ All repository tests passing with H2
- ✅ All service tests passing with mocks
- ✅ All mapper tests passing
- ✅ Coverage: 50%+

**Phase 3 (Controllers):**
- ✅ All controller tests passing with MockMvc
- ✅ Validation tests for all endpoints
- ✅ Coverage: 65%+

**Phase 4 (Integration):**
- ✅ Service integration tests passing
- ✅ WebSocket tests passing
- ✅ Scheduled task tests passing
- ✅ Coverage: 75%+

**Phase 5 (TestContainers & E2E):**
- ✅ PostgreSQL TestContainers configured
- ✅ Flyway migrations validated in tests
- ✅ End-to-end workflows tested
- ✅ Coverage: 80%+

**Phase 6 (Polish & CI/CD):**
- ✅ Coverage target: 80%+
- ✅ CI/CD pipeline configured
- ✅ Tests run on every PR
- ✅ Coverage report in PR comments
- ✅ Build fails if coverage drops

---

## Appendix: Quick Command Reference

```bash
# Run all tests
./mvnw test

# Run tests with coverage report
./mvnw clean verify

# View coverage report (after running tests)
open target/site/jacoco/index.html

# Run specific test class
./mvnw test -Dtest=StationServiceImplTest

# Run specific test method
./mvnw test -Dtest=StationServiceImplTest#getOrCreateStation_WhenIpExists_ReturnsExistingStation

# Run tests in parallel (faster)
./mvnw -T 1C test  # 1 thread per CPU core

# Run integration tests only
./mvnw verify -Pintegration-tests

# Skip tests (not recommended!)
./mvnw package -DskipTests
```

---

## Conclusion

This comprehensive testing strategy transforms the AirQuality API from **~2% coverage to 80%+ coverage** through a structured, phased approach. By following the test pyramid, using appropriate test types for each layer, and integrating with CI/CD, we ensure:

- **Reliability:** Bugs caught early through automated testing
- **Confidence:** Safe refactoring with comprehensive test coverage
- **Documentation:** Tests serve as living documentation
- **Quality:** Enforced quality gates in CI/CD pipeline

**Next Steps:**
1. Review and approve this strategy
2. Begin Phase 1 implementation (Foundation)
3. Iterate through phases with regular progress reviews
4. Celebrate 80%+ coverage achievement!

---

**Document Owner:** Development Team
**Review Cycle:** Monthly
**Last Review:** 2025-11-05
