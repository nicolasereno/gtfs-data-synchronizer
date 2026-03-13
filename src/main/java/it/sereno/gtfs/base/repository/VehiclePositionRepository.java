package it.sereno.gtfs.base.repository;

import it.sereno.gtfs.base.model.VehiclePosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehiclePositionRepository extends JpaRepository<VehiclePosition, String> {

	List<VehiclePosition> findByRouteIdIn( List<String> routeIds );
}