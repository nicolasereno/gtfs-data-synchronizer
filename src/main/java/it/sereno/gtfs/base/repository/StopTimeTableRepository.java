package it.sereno.gtfs.base.repository;

import it.sereno.gtfs.base.model.StopTimeTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface StopTimeTableRepository extends JpaRepository<StopTimeTable, String> {

	@Modifying( clearAutomatically = true, flushAutomatically = true )
	@Transactional
	@Query( value = "DELETE FROM stop_time_table", nativeQuery = true )
	void deleteMainTable();

	@Modifying( clearAutomatically = true, flushAutomatically = true )
	@Transactional
	@Query( value = "DELETE FROM route_timetable_arrival_times", nativeQuery = true )
	void deleteCollectionTable();

	@Modifying( clearAutomatically = true, flushAutomatically = true )
	@Transactional
	@Query( value = "DELETE FROM route_timetable", nativeQuery = true )
	void deleteSecondaryTable();
}
