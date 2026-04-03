package it.sereno.gtfs.base.repository;

import it.sereno.gtfs.base.model.Stop;
import it.sereno.gtfs.base.model.StopWithDistance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StopRepository extends JpaRepository<Stop, String> {

	@Query( value = """
			SELECT s.id, s.name, ST_Distance( s.location::geography, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography ) AS distance_meters
			FROM stop s
			WHERE ST_Distance( s.location::geography, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography ) < :maxDistance
			ORDER BY s.location <-> ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)
			LIMIT :maxResults
			""", nativeQuery = true )
	List<StopWithDistance> findNearestStops(
			@Param( "lon" ) double lon,
			@Param( "lat" ) double lat,
			@Param( "maxDistance" ) double maxDistance,
			@Param( "maxResults" ) int maxResults );

}