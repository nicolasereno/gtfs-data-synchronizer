package it.sereno.gtfs.service;

import com.google.transit.realtime.GtfsRealtime;
import it.sereno.gtfs.base.model.VehiclePosition;
import it.sereno.gtfs.base.repository.VehiclePositionRepository;
import it.sereno.gtfs.updates.model.TripCorrection;
import it.sereno.gtfs.updates.repository.TripCorrectionsRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GTFSDynamicDataImportService {

	private final RestTemplate restTemplate;
	private final VehiclePositionRepository vehiclePositionRepository;
	private final TripCorrectionsRepository tripCorrectionsRepository;
	private final String gtfsVehiclePositionsUrl;
	private final String gtfsUpdatesUrl;

	private static final GeometryFactory geometryFactory =
			new GeometryFactory( new PrecisionModel(), 4326 ); // WGS84

	public GTFSDynamicDataImportService(
			final RestTemplate restTemplate,
			final VehiclePositionRepository vehiclePositionRepository,
			final TripCorrectionsRepository tripCorrectionsRepository,
			final @Value( "${gtfs.vehicle-positions-url}" ) String gtfsVehiclePositionsUrl,
			final @Value( "${gtfs.updates-url}" ) String gtfsUpdatesUrl ) {
		this.restTemplate = restTemplate;
		this.vehiclePositionRepository = vehiclePositionRepository;
		this.tripCorrectionsRepository = tripCorrectionsRepository;
		this.gtfsVehiclePositionsUrl = gtfsVehiclePositionsUrl;
		this.gtfsUpdatesUrl = gtfsUpdatesUrl;
	}

	@SneakyThrows
	public void importGtfsUpdates() {
		try ( InputStream inputStream = new URI( gtfsUpdatesUrl ).toURL().openStream() ) {
			GtfsRealtime.FeedMessage feed = GtfsRealtime.FeedMessage.parseFrom( inputStream );
			final Map<String, TripCorrection> tripCorrections = new HashMap<>();
			for ( GtfsRealtime.FeedEntity entity : feed.getEntityList() ) {
				if ( !entity.hasTripUpdate() ) {
					continue;
				}
				GtfsRealtime.TripUpdate tripUpdate = entity.getTripUpdate();
				for ( GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate : tripUpdate.getStopTimeUpdateList() ) {
					LocalTime time = Instant.ofEpochSecond( stopTimeUpdate.getArrival().getTime() )
							.atZone( ZoneId.systemDefault() )
							.toLocalTime();

					String tripIdentifier = tripUpdate.getTrip().getTripId();
					if ( !tripCorrections.containsKey( tripIdentifier ) ) {
						tripCorrections.put( tripIdentifier, new TripCorrection( tripIdentifier, new HashMap<>() ) );
					}
					String stopCode = stopTimeUpdate.getStopId();
					String routeIdentifier = tripUpdate.getTrip().getRouteId();
					String arrivalTime = time.format( DateTimeFormatter.ISO_LOCAL_TIME );
					tripCorrections.get( tripIdentifier ).getCorrections().put( stopCode, arrivalTime );
				}
			}
			log.debug( "Saving [{}] trip corrections", tripCorrections.size() );
			tripCorrectionsRepository.deleteAll();
			tripCorrectionsRepository.saveAll( tripCorrections.values() );
			log.debug( "Saved [{}] trip corrections", tripCorrections.size() );
		}
	}

	public void fetchAndStoreVehiclePositions() {

		byte[] feedBytes = restTemplate.getForObject( gtfsVehiclePositionsUrl, byte[].class );
		if ( feedBytes == null ) {
			return;
		}
		try {
			GtfsRealtime.FeedMessage feed = GtfsRealtime.FeedMessage.parseFrom( new ByteArrayInputStream( feedBytes ) );
			List<VehiclePosition> documents = new ArrayList<>();

			for ( GtfsRealtime.FeedEntity entity : feed.getEntityList() ) {

				if ( !entity.hasVehicle() ) {
					continue;
				}

				GtfsRealtime.VehiclePosition vehicle = entity.getVehicle();

				if ( !vehicle.hasPosition() ) {
					continue;
				}

				GtfsRealtime.Position position = vehicle.getPosition();


				if ( !position.hasLatitude() || !position.hasLongitude() ) {
					continue;
				}


				final Point location = geometryFactory.createPoint(new Coordinate( position.getLongitude(), position.getLatitude() ));

				VehiclePosition doc = VehiclePosition.builder()
						.vehicleId( vehicle.getVehicle().getId() )
						.tripId( vehicle.getTrip().getTripId() )
						.routeId( vehicle.getTrip().getRouteId() )
						.outbound( vehicle.getTrip().getDirectionId() == 0 )
						.location( location )
						.bearing( position.hasBearing()
								? (double) position.getBearing()
								: null )
						.speed( position.hasSpeed()
								? (double) position.getSpeed()
								: null )
						.timestamp( vehicle.hasTimestamp()
								? Instant.ofEpochSecond( vehicle.getTimestamp() )
								: null )
						.build();

				documents.add( doc );
			}

			log.debug( "Saving [{}] vehicle positions", documents.size() );
			vehiclePositionRepository.deleteAll();
			vehiclePositionRepository.saveAll( documents );

		} catch ( Exception e ) {
			log.error( "Failed to parse GTFS feed: [{}]", e.getMessage() );
		}
	}

}