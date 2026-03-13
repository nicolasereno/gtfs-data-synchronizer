package it.sereno.gtfs.service;

import it.sereno.gtfs.base.model.Route;
import it.sereno.gtfs.base.model.RouteTimetable;
import it.sereno.gtfs.base.model.Stop;
import it.sereno.gtfs.base.model.StopTimeTable;
import it.sereno.gtfs.base.repository.RouteRepository;
import it.sereno.gtfs.base.repository.StopRepository;
import it.sereno.gtfs.base.repository.StopTimeTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GTFSStaticDataImportService {

	private final JdbcTemplate baseJdbcTemplate;
	private final StopTimeTableRepository stopTimeTableRepository;
	private final RouteRepository routeRepository;
	private final StopRepository stopRepository;

	@Transactional
	public void importStopTimetableData() {
		log.info( "Importing stop timetable..." );
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

		log.info( "Deleting old timetable data..." );
		stopTimeTableRepository.deleteCollectionTable();
		stopTimeTableRepository.deleteSecondaryTable();
		stopTimeTableRepository.deleteMainTable();
		log.info( "Inserting new timetable data..." );
		stopTimeTableRepository.saveAll( stopTimeTableMap.values() );
		log.info( "Timetable synchronization completed!" );
	}

	@Transactional
	public void importStopData() {
		log.info( "Importing stops..." );
		final String query = """
				select s.stop_id, s.stop_name, s.stop_lon, s.stop_lat
				from stops s
				""";

		GeometryFactory geometryFactory = new GeometryFactory( new PrecisionModel(), 4326 );

		List<Stop> stops = baseJdbcTemplate.query( query, ( rs, rowNum ) -> {
			String stopId = rs.getString( "stop_id" );
			String stopName = rs.getString( "stop_name" );
			double lon = rs.getDouble( "stop_lon" );
			double lat = rs.getDouble( "stop_lat" );

			Point point = geometryFactory.createPoint( new Coordinate( lon, lat ) );

			return Stop.builder()
					.id( stopId )
					.name( stopName )
					.location( point )
					.build();
		} );

		log.info( "Deleting old stop data..." );
		stopRepository.deleteAll();
		log.info( "Inserting {} new stops...", stops.size() );
		stopRepository.saveAll( stops );
		log.info( "Stop synchronization completed!" );
	}

	@Transactional
	public void importRouteData() {
		log.info( "Importing routes..." );
		final String query = """
				select distinct t.route_id as routeId, t.trip_headsign as shortName, s.shape_pt_lat, s.shape_pt_lon, s.shape_pt_sequence
				from trips t
				         join shapes s on t.shape_id = s.shape_id
				order by t.route_id, s.shape_pt_sequence
				""";

		Map<String, Map<Integer, Coordinate>> routePoints = new LinkedHashMap<>();
		Map<String, String> routeShortNames = new LinkedHashMap<>();

		baseJdbcTemplate.query( query, rs -> {
			String routeId = rs.getString( "routeId" );
			String shortName = rs.getString( "shortName" );
			double lat = rs.getDouble( "shape_pt_lat" );
			double lon = rs.getDouble( "shape_pt_lon" );
			int sequence = rs.getInt( "shape_pt_sequence" );

			routePoints.computeIfAbsent( routeId, k -> new LinkedHashMap<>() ).put( sequence, new Coordinate( lon, lat ) );
			routeShortNames.putIfAbsent( routeId, shortName );
		} );

		GeometryFactory geometryFactory = new GeometryFactory( new PrecisionModel(), 4326 );
		List<Route> routes = routePoints.entrySet().stream()
				.map( entry -> {
					String routeId = entry.getKey();
					List<Coordinate> coords = new ArrayList<>( entry.getValue().values() );
					LineString lineString = geometryFactory.createLineString( coords.toArray( new Coordinate[0] ) );
					return Route.builder()
							.routeId( routeId )
							.shortName( routeShortNames.get( routeId ) )
							.path( lineString )
							.build();
				} )
				.toList();

		log.info( "Deleting old route data..." );
		routeRepository.deleteAll();
		log.info( "Inserting {} new routes...", routes.size() );
		routeRepository.saveAll( routes );
		log.info( "Route synchronization completed!" );
	}
}
