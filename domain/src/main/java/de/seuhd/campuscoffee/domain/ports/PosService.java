package de.seuhd.campuscoffee.domain.ports;


import de.seuhd.campuscoffee.domain.exceptions.DuplicatePosNameException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeMissingFieldsException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.exceptions.PosNotFoundException;
import de.seuhd.campuscoffee.domain.model.CampusType;
import de.seuhd.campuscoffee.domain.model.Pos;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Service interface for POS (Point of Sale) operations.
 * This interface defines the core business logic operations for managing Points of Sale.
 * This is a port in the hexagonal architecture pattern, implemented by the domain layer
 * and consumed by the API layer. It encapsulates business rules and orchestrates
 * data operations through the {@link PosDataService} port.
 */
public interface PosService {
    /**
     * Clears all POS data.
     * This operation removes all Points of Sale from the system.
     * Warning: This is a destructive operation typically used only for testing
     * or administrative purposes. Use with caution in production environments.
     */
    void clear();

    /**
     * Retrieves all Points of Sale in the system.
     *
     * @return a list of all POS entities; never null, but may be empty if no POSs exist
     */
    @NonNull List<Pos> getAll();

    /**
     * Retrieves a specific Point of Sale by its unique identifier.
     *
     * @param id the unique identifier of the POS to retrieve; must not be null
     * @return the POS entity with the specified ID; never null
     * @throws PosNotFoundException if no POS exists with the given ID
     */
    @NonNull Pos getById(@NonNull Long id) throws PosNotFoundException;

    // TODO: Add a new getByName method to enable fetching POS by name.
    @NonNull Pos getByName(@NonNull String name) throws PosNotFoundException;
    /**
     * Creates a new POS or updates an existing one.
     * This method performs an "upsert" operation:
     * <ul>
     *   <li>If the POS has no ID (null), a new POS is created</li>
     *   <li>If the POS has an ID, and it exists, the existing POS is updated</li>
     * </ul>
     * <p>
     * Business rules enforced:
     * <ul>
     *   <li>POS names must be unique (enforced by database constraint)</li>
     *   <li>All required fields must be present and valid</li>
     *   <li>Timestamps (createdAt, updatedAt) are managed by the {@link PosDataService}.</li>
     * </ul>
     *
     * @param pos the POS entity to create or update; must not be null
     * @return the persisted POS entity with populated ID and timestamps; never null
     * @throws PosNotFoundException if attempting to update a POS that does not exist
     * @throws DuplicatePosNameException if a POS with the same name already exists
     */
    @NonNull Pos upsert(@NonNull Pos pos) throws PosNotFoundException, DuplicatePosNameException;

    /**
     * Imports a Point of Sale from an OpenStreetMap node.
     * Fetches POS data from OpenStreetMap using the {@link OsmDataService}, converts it to a POS entity,
     * and saves it to the system. If a POS with the same name already exists, it will be updated.
     * <p>
     * The import process:
     * <ol>
     *   <li>Fetches the OSM node data using the provided node ID</li>
     *   <li>Extracts relevant tags (name, address, etc.)</li>
     *   <li>Maps OSM data to the POS domain model </li>
     *   <li>Persists the POS entity using the upsert method</li>
     * </ol>
     *
     * @param nodeId the OpenStreetMap node ID to import; must not be null
     * @param campusType the campus type to assign to the imported POS; must not be null
     * @return the created or updated POS entity; never null
     * @throws OsmNodeNotFoundException if the OSM node with the given ID doesn't exist or cannot be fetched
     * @throws OsmNodeMissingFieldsException if the OSM node lacks required fields for creating a valid POS
     * @throws DuplicatePosNameException if a POS with the same name already exists
     */
    @NonNull Pos importFromOsmNode(@NonNull Long nodeId, @NonNull CampusType campusType) throws OsmNodeNotFoundException, OsmNodeMissingFieldsException, DuplicatePosNameException;
}
