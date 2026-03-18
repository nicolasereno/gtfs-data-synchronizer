package it.sereno.gtfs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class GTFSStaticDataImportService {

	private final JdbcTemplate baseJdbcTemplate;

	public void importStopTimetableData() {
		log.debug( "Importing stop timetable..." );
		final LocalDate date = LocalDate.now();

		log.debug( "Deleting old timetable data..." );
		baseJdbcTemplate.update( "delete from route_timetable_arrival_times" );
		baseJdbcTemplate.update( "delete from route_timetable" );
		baseJdbcTemplate.update( "delete from stop_time_table" );

		log.debug( "Inserting new timetable data..." );

		baseJdbcTemplate.update( """
				insert into stop_time_table (stop_code)
				select distinct s.stop_code
				from stops s
				join stop_times st on s.stop_id = st.stop_id
				join trips t on st.trip_id = t.trip_id
				join calendar_dates cd on t.service_id = cd.service_id and cd."date" = ?
				where s.stop_code is not null
				""", date );

		baseJdbcTemplate.update( """
				insert into route_timetable (route_identifier, direction_description, stop_code)
				select distinct t.route_id, t.trip_headsign, s.stop_code
				from stops s
				join stop_times st on s.stop_id = st.stop_id
				join trips t on st.trip_id = t.trip_id
				join calendar_dates cd on t.service_id = cd.service_id and cd."date" = ?
				where s.stop_code is not null
				""", date );

		baseJdbcTemplate.update( """
				insert into route_timetable_arrival_times (route_timetable_id, trip_identifier, arrival_time)
				select rt.id, t.trip_id, st.arrival_time
				from route_timetable rt
				join stops s on rt.stop_code = s.stop_code
				join stop_times st on s.stop_id = st.stop_id
				join trips t on st.trip_id = t.trip_id and t.route_id = rt.route_identifier and t.trip_headsign = rt.direction_description
				join calendar_dates cd on t.service_id = cd.service_id and cd."date" = ?
				""", date );

		log.debug( "Timetable synchronization completed!" );
	}

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

	public void importRouteData() {
		log.debug( "Importing routes..." );
		log.debug( "Deleting old route data..." );
		baseJdbcTemplate.update( "delete from route" );
		log.debug( "Inserting new routes..." );

		baseJdbcTemplate.update( """
				insert into route (shape_id, route_id, outbound, short_name, path)
				select t.shape_id, t.route_id, t.direction_id = 1 as outbound, t.trip_headsign, s.shepe_geom
				from (select distinct shape_id as shape, route_id, direction_id, trip_headsign, shape_id from trips a) t
				         join
				     (select shape_id, st_setsrid(st_makeline(st_point(shape_pt_lon, shape_pt_lat) order by shape_pt_sequence), 4326) as shepe_geom
				      from shapes
				      group by shape_id) s
				     on s.shape_id = t.shape
				""" );

		log.debug( "Route synchronization completed!" );
	}
}
