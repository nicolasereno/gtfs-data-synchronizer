package it.sereno.gtfs.updates.repository;

import it.sereno.gtfs.updates.model.TripCorrection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TripCorrectionsRepository extends JpaRepository<TripCorrection, String> {

	@Query( "select t from TripCorrection t left join fetch t.corrections where t.tripIdentifier = :tripIdentifier" )
	Optional<TripCorrection> findCorrectionsById( @Param( "tripIdentifier" ) String tripIdentifier );
}
