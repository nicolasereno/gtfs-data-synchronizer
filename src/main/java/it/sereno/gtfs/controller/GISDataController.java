package it.sereno.gtfs.controller;

import it.sereno.gtfs.service.GISDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GISDataController {

	private final GISDataService service;

	@GetMapping( "/api/vehicles/geojson" )
	public GISDataService.GeoJsonFeatureCollection getVehiclesLayer( @RequestParam( required = false ) final List<String> routeIds ) {
		return service.getVehiclesLayer( routeIds );
	}

	@GetMapping( "/api/routes/geojson" )
	public GISDataService.GeoJsonFeatureCollection getRoutesLayer( @RequestParam( required = false ) final List<String> routeIds ) {
		return service.getRoutesLayer( routeIds );
	}

	@GetMapping( "/api/stops/geojson" )
	public GISDataService.GeoJsonFeatureCollection getStopsLayer() {
		return service.getStopsLayer();
	}


}