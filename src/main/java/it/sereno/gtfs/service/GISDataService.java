package it.sereno.gtfs.service;

import it.sereno.gtfs.base.model.StopWithDistance;
import it.sereno.gtfs.base.repository.RouteRepository;
import it.sereno.gtfs.base.repository.StopRepository;
import it.sereno.gtfs.base.repository.VehiclePositionRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GISDataService {

	private final VehiclePositionRepository repository;
	private final RouteRepository routeRepository;
	private final StopRepository stopRepository;

	public List<StopWithDistance> getNearestStops( final double lon, final double lat ) {
		return stopRepository.findNearestStops( lon, lat, 2000, 10 );
	}

	public GeoJsonFeatureCollection getVehiclesLayer( final List<String> routeIds ) {

		List<GeoJsonFeature> features = ((routeIds == null || routeIds.isEmpty())
				? repository.findAll()
				: repository.findByRouteIdIn( routeIds ))
				.stream()
				.map( doc -> GeoJsonFeature.builder()
						.geometry( toGeoJson( doc.getLocation() ) )
						.properties( Map.of(
								"tripIdentifier", doc.getTripId(),
								"routeId", doc.getRouteId(),
								"outbound", doc.isOutbound(),
								"timestamp", doc.getTimestamp()
						) )
						.build()
				)
				.toList();

		return GeoJsonFeatureCollection.builder()
				.features( features )
				.build();
	}

	public GeoJsonFeatureCollection getRoutesLayer( final List<String> routeIds ) {
		List<GeoJsonFeature> features = ((routeIds == null || routeIds.isEmpty())
				? routeRepository.findAll()
				: routeRepository.findByRouteIdIn( routeIds ))
				.stream()
				.filter( doc -> doc.getPath() != null )
				.map( doc ->
						GeoJsonFeature.builder()
								.geometry( toGeoJson( doc.getPath() ) )
								.properties( Map.of(
										"routeId", doc.getRouteId(),
										"shortName", doc.getShortName() != null
												? doc.getShortName()
												: ""
								) )
								.build()
				)
				.toList();

		return GeoJsonFeatureCollection.builder()
				.features( features )
				.build();
	}

	public GeoJsonFeatureCollection getStopsLayer() {
		List<GeoJsonFeature> features = stopRepository.findAll()
				.stream()
				.map( doc -> GeoJsonFeature.builder()
						.geometry( toGeoJson( doc.getLocation() ) )
						.properties( Map.of(
								"stopId", doc.getId(),
								"name", doc.getName() != null
										? doc.getName()
										: ""
						) )
						.build()
				)
				.toList();

		return GeoJsonFeatureCollection.builder()
				.features( features )
				.build();
	}

	private static final GeoJsonWriter WRITER = new GeoJsonWriter();
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@SneakyThrows
	public static JsonNode toGeoJson( Geometry geometry ) {
		if ( geometry == null ) {
			return null;
		}

		String json = WRITER.write( geometry );
		return MAPPER.readTree( json );
	}

	@Data
	@Builder
	public static class GeoJsonFeatureCollection {

		@Builder.Default
		private String type = "FeatureCollection";
		private List<GeoJsonFeature> features;
	}

	@Data
	@Builder
	public static class GeoJsonFeature {

		@Builder.Default
		private String type = "Feature";
		private JsonNode geometry;
		private Map<String, Object> properties;
	}
}