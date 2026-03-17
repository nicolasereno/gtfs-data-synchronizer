package it.sereno.gtfs.service;

import it.sereno.gtfs.base.model.RouteTimetable;
import it.sereno.gtfs.base.model.StopTimeTable;
import it.sereno.gtfs.base.repository.StopTimeTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GTFSStaticDataImportService {

	private final JdbcTemplate baseJdbcTemplate;
	private final StopTimeTableRepository stopTimeTableRepository;

	@Transactional
	public void importStopTimetableData() {
		log.debug( "Importing stop timetable..." );
		final SimpleDateFormat sdf = new SimpleDateFormat( "yyyyMMdd" );

		final String query = """
				select s.stop_code, t.route_id, t.trip_headsign, t.trip_id, st.arrival_time
					from stops s
					join stop_times st on s.stop_id = st.stop_id
					join trips t on st.trip_id = t.trip_id
					join calendar_dates cd on t.service_id = cd.service_id and cd.date = '%s'
				""".formatted( sdf.format( System.currentTimeMillis() ) );

		Map<String, StopTimeTable> stopTimeTableMap = new LinkedHashMap<>();

		baseJdbcTemplate.query( query, rs -> {
			String stopCode = rs.getString( "stop_code" );
			String routeId = rs.getString( "route_id" );
			String tripId = rs.getString( "trip_id" );
			String tripHeadsign = rs.getString( "trip_headsign" );
			String arrivalTimeStr = rs.getString( "arrival_time" );

			StopTimeTable stopTimeTable = stopTimeTableMap.computeIfAbsent( stopCode, k -> {
				StopTimeTable stt = new StopTimeTable();
				stt.setStopCode( k );
				stt.setTimetable( new ArrayList<>() );
				return stt;
			} );

			RouteTimetable routeTimetable = stopTimeTable.getTimetable().stream()
					.filter( rt -> rt.getRouteIdentifier().equals( routeId ) && rt.getDirectionDescription().equals( tripHeadsign ) )
					.findFirst()
					.orElseGet( () -> {
						RouteTimetable rt = new RouteTimetable();
						rt.setRouteIdentifier( routeId );
						rt.setDirectionDescription( tripHeadsign );
						rt.setArrivalTimes( new ArrayList<>() );
						stopTimeTable.getTimetable().add( rt );
						return rt;
					} );

			routeTimetable.getArrivalTimes().add( new RouteTimetable.ArrivalTime( tripId, arrivalTimeStr ) );
		} );

		log.debug( "Deleting old timetable data..." );
		stopTimeTableRepository.deleteCollectionTable();
		stopTimeTableRepository.deleteSecondaryTable();
		stopTimeTableRepository.deleteMainTable();
		log.debug( "Inserting new timetable data..." );
		stopTimeTableRepository.saveAll( stopTimeTableMap.values() );
		log.debug( "Timetable synchronization completed!" );
	}

	@Transactional
	public void importStopData() {
		log.debug( "Importing stops..." );
		log.debug( "Deleting old stop data..." );
		baseJdbcTemplate.update( "delete from stop" );
		log.debug( "Inserting new stops..." );
		baseJdbcTemplate.update( """
				insert into stop (id, name, location)
				select s.stop_id, s.stop_name, st_setsrid(st_point(s.stop_lon, s.stop_lat), 4326)
				from stops s
				""" );
		log.debug( "Stop synchronization completed!" );
	}

	@Transactional
	public void importRouteData() {
		log.debug( "Importing routes..." );
		log.debug( "Deleting old route data..." );
		baseJdbcTemplate.update( "delete from route" );
		log.debug( "Inserting new routes..." );

		Integer routeCount = baseJdbcTemplate.queryForObject( "select count(distinct route_id) from routes", Integer.class );
		if ( routeCount == null ) {
			routeCount = 0;
		}

		int blockSize = 100;
		for ( int i = 0; i < routeCount; i += blockSize ) {
			log.info( "Inserting routes block {}/{}...", i, routeCount );
			baseJdbcTemplate.update( """
					insert into route (route_id, short_name, path)
					select t.route_id || '-' || t.direction_id, max(t.trip_headsign), st_setsrid(st_makeline(st_point(s.shape_pt_lon, s.shape_pt_lat) order by s.shape_pt_sequence), 4326)
					from trips t
					         join shapes s on t.shape_id = s.shape_id
					where t.route_id in (select distinct route_id from routes order by route_id offset ? limit ?)
					group by t.route_id, t.direction_id
					""", i, blockSize );
		}

		log.debug( "Route synchronization completed!" );
	}
}
