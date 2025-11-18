package de.seuhd.campuscoffee.api.controller;

import de.seuhd.campuscoffee.api.dtos.PosDto;
import de.seuhd.campuscoffee.api.mapper.PosDtoMapper;
import de.seuhd.campuscoffee.domain.model.CampusType;
import de.seuhd.campuscoffee.domain.ports.PosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controller for handling POS-related API requests.
 */
@Controller
@RequestMapping("/api/pos")
@Slf4j
@RequiredArgsConstructor
public class PosController {
    private final PosService posService;
    private final PosDtoMapper posDtoMapper;

    @GetMapping("")
    public ResponseEntity<List<PosDto>> getAll() {
        return ResponseEntity.ok(
                posService.getAll().stream()
                        .map(posDtoMapper::fromDomain)
                        .toList()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<PosDto> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                posDtoMapper.fromDomain(posService.getById(id))
        );
    }

    // TODO: Implement a new GET endpoint that supports filtering POS by name, e.g., /filter?name=Schmelzpunkt
    @GetMapping("/filter")
    public ResponseEntity<PosDto> getByName(
            @RequestParam String name) {
        return ResponseEntity.ok(
                posDtoMapper.fromDomain(posService.getByName(name)));
    }
    @PostMapping("")
    public ResponseEntity<PosDto> create(
            @RequestBody PosDto posDto) {
        PosDto created = upsert(posDto);
        return ResponseEntity
                .created(getLocation(created.id()))
                .body(created);
    }

    @PostMapping("/import/osm/{nodeId}")
    public ResponseEntity<PosDto> create(
            @PathVariable Long nodeId,
            @RequestBody CampusType campusType) {
        PosDto created = posDtoMapper.fromDomain(
                posService.importFromOsmNode(nodeId, campusType)
        );
        return ResponseEntity
                .created(getLocation(created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PosDto> update(
            @PathVariable Long id,
            @RequestBody PosDto posDto) {
        if (!id.equals(posDto.id())) {
            throw new IllegalArgumentException("POS ID in path and body do not match.");
        }
        return ResponseEntity.ok(upsert(posDto));
    }

    /**
     * Common upsert logic for create and update.
     *
     * @param posDto the POS DTO to map and upsert
     * @return the upserted POS mapped back to the DTO format.
     */
    private PosDto upsert(PosDto posDto) {
        return posDtoMapper.fromDomain(
                posService.upsert(
                        posDtoMapper.toDomain(posDto)
                )
        );
    }

    /**
     * Builds the location URI for a newly created resource.
     * @param resourceId the ID of the created resource
     * @return the location URI
     */
    private URI getLocation(Long resourceId) {
        return ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resourceId)
                .toUri();
    }
}
