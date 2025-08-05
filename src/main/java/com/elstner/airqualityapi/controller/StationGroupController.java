package com.elstner.airqualityapi.controller;

import com.elstner.airqualityapi.dto.StationGroupDto;
import com.elstner.airqualityapi.mapper.StationGroupMapper;
import com.elstner.airqualityapi.model.StationGroup;
import com.elstner.airqualityapi.repository.StationGroupRepository;
import com.elstner.airqualityapi.repository.StationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stationgroups")
public class StationGroupController {

    private final StationGroupMapper stationGroupMapper;
    private final StationGroupRepository stationGroupRepository;

    public StationGroupController(StationGroupMapper stationGroupMapper,
                                  StationGroupRepository stationGroupRepository) {
        this.stationGroupMapper = stationGroupMapper;
        this.stationGroupRepository = stationGroupRepository;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(stationGroupMapper.toStationGroupDtoList(stationGroupRepository.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id) {
        var stationGroup = stationGroupRepository.findById(id)
                .map(stationGroupMapper::toStationGroupDto)
                .orElse(null);

        if(stationGroup == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(stationGroup);
    }

    @PostMapping
    public ResponseEntity<?> createStationGroup(@RequestBody StationGroup stationGroup) {
        stationGroupRepository.save(stationGroup);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(stationGroupMapper.toStationGroupDto(stationGroup));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStationGroup(@PathVariable Long id, @RequestBody StationGroupDto stationGroupDto) {
        var updatedStationGroup = stationGroupRepository.findById(id)
                .map(stationGroup -> {
                    // update each property, if needed
                    if (stationGroupDto.getDisplayName() != null) stationGroup.setDisplayName(stationGroupDto.getDisplayName());
                    if (stationGroupDto.getDisplayOrder() != null) stationGroup.setDisplayOrder(stationGroupDto.getDisplayOrder());
                    if (stationGroupDto.isActive() != null) stationGroup.setActive(stationGroupDto.isActive());

                    return stationGroupRepository.save(stationGroup);
                }).orElse(null);


        if (updatedStationGroup == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .header("Content-Type", "application/problem+json")
                    .body(null);
        } else {
            return ResponseEntity.ok(stationGroupMapper.toStationGroupDto(updatedStationGroup));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStationGroup(@PathVariable Long id) {
        var stationGroup = stationGroupRepository.findById(id)
                .orElse(null);
        if (stationGroup == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .header("Content-Type", "application/problem+json")
                    .body("station group not found");
        } else {
            stationGroupRepository.delete(stationGroup);
        }


        return ResponseEntity.noContent().build();
    }
}
