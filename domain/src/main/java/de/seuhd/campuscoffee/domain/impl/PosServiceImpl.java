package de.seuhd.campuscoffee.domain.impl;

import de.seuhd.campuscoffee.domain.exceptions.DuplicatePosNameException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeMissingFieldsException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.model.CampusType;
import de.seuhd.campuscoffee.domain.model.OsmAmenity;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.model.Pos;
import de.seuhd.campuscoffee.domain.exceptions.PosNotFoundException;
import de.seuhd.campuscoffee.domain.model.PosType;
import de.seuhd.campuscoffee.domain.ports.OsmDataService;
import de.seuhd.campuscoffee.domain.ports.PosDataService;
import de.seuhd.campuscoffee.domain.ports.PosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Implementation of the POS service that handles business logic related to POS entities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PosServiceImpl implements PosService {
    private final PosDataService posDataService;
    private final OsmDataService osmDataService;

    @Override
    public void clear() {
        log.warn("Clearing all POS data");
        posDataService.clear();
    }

    @Override
    public @NonNull List<Pos> getAll() {
        log.debug("Retrieving all POS");
        return posDataService.getAll();
    }

    @Override
    public @NonNull Pos getById(@NonNull Long id) throws PosNotFoundException {
        log.debug("Retrieving POS with ID: {}", id);
        return posDataService.getById(id);
    }

    // TODO: Implement getByName after adding it to the PosService interface. Note that the PosDataService already supports filtering by name.
    @Override
    public @NonNull Pos getByName(@NonNull String name) throws PosNotFoundException {
        log.debug("Retrieving POS with name: {}", name);
        return posDataService.getByName(name);
    }

    @Override
    public @NonNull Pos upsert(@NonNull Pos pos) throws PosNotFoundException {
        if (pos.id() == null) {
            // create a new POS
            log.info("Creating new POS: {}", pos.name());
        } else {
            // update an existing POS
            log.info("Updating POS with ID: {}", pos.id());
            // POS ID must be set
            Objects.requireNonNull(pos.id());
            // POS must exist in the database before the update
            posDataService.getById(pos.id());
        }
        return performUpsert(pos);
    }

    @Override
    public @NonNull Pos importFromOsmNode(@NonNull Long nodeId, @NonNull CampusType campusType)
            throws OsmNodeNotFoundException {
        log.info("Importing POS from OpenStreetMap node {}...", nodeId);

        // Fetch the OSM node data using the port
        OsmNode osmNode = osmDataService.fetchNode(nodeId);

        // Convert OSM node to POS domain object and upsert it
        Pos savedPos = upsert(convertOsmNodeToPos(osmNode, campusType));
        log.info("Successfully imported POS '{}' from OSM node {}", savedPos.name(), nodeId);

        return savedPos;
    }

    /**
     * Converts an OSM node to a POS domain object.
     * Maps OSM amenity types to POS types and validates required fields.
     *
     * @param osmNode the OSM node data
     * @param campusType the campus where the POS is located
     * @return a new Pos object with data from the OSM node
     * @throws OsmNodeMissingFieldsException if required fields are missing or invalid
     */
    private @NonNull Pos convertOsmNodeToPos(@NonNull OsmNode osmNode, @NonNull CampusType campusType) {
        // map OSM amenity to POS type
        PosType posType = mapAmenityToPosType(osmNode.amenity());

        // parse postal code from string to integer
        int postalCode;
        try {
            postalCode = Integer.parseInt(osmNode.postcode());
        } catch (NumberFormatException e) {
            log.error("Could not parse postcode {} of OSM node {}", osmNode.postcode(), osmNode.nodeId());
            throw new OsmNodeMissingFieldsException(osmNode.nodeId(), "postcode");
        }

        // build and return POS object
        return Pos.builder()
                .name(osmNode.name())
                .description(osmNode.description())
                .type(posType)
                .campus(campusType)
                .street(osmNode.street())
                .houseNumber(osmNode.houseNumber())
                .postalCode(postalCode)
                .city(osmNode.city())
                .build();
    }

    /**
     * Maps an OpenStreetMap amenity type to a POS type.
     *
     * @param amenity the OSM amenity type
     * @return the corresponding POS type
     */
    private PosType mapAmenityToPosType(OsmAmenity amenity) {
        return switch (amenity) {
            case CAFE, ICE_CREAM -> PosType.CAFE;
            case VENDING_MACHINE -> PosType.VENDING_MACHINE;
            case FOOD_COURT -> PosType.CAFETERIA;
            case BAR, BIERGARTEN, PUB, RESTAURANT, FAST_FOOD -> PosType.OTHER;
        };
    }

    /**
     * Performs the actual upsert operation with consistent error handling and logging.
     * Database constraint enforces name uniqueness - data layer will throw DuplicatePosNameException if violated.
     * JPA lifecycle callbacks (@PrePersist/@PreUpdate) set timestamps automatically.
     *
     * @param pos the POS to upsert
     * @return the persisted POS with updated ID and timestamps
     * @throws DuplicatePosNameException if a POS with the same name already exists
     */
    private @NonNull Pos performUpsert(@NonNull Pos pos) throws DuplicatePosNameException {
        try {
            Pos upsertedPos = posDataService.upsert(pos);
            log.info("Successfully upserted POS with ID: {}", upsertedPos.id());
            return upsertedPos;
        } catch (DuplicatePosNameException e) {
            log.error("Error upserting POS '{}': {}", pos.name(), e.getMessage());
            throw e;
        }
    }
}
