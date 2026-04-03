package it.sereno.gtfs.controller;

import it.sereno.gtfs.base.model.StopWithDistance;
import it.sereno.gtfs.service.GISDataService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gis")
@CrossOrigin(origins = {"https://smart-mobility.homelinuxserver.org", "http://localhost:4200"})
@RequiredArgsConstructor
public class GISDataController {

    private final GISDataService service;

    @GetMapping("/vehicles")
    public GISDataService.GeoJsonFeatureCollection getVehiclesLayer(@RequestParam(required = false) final List<String> routeIds) {
        return service.getVehiclesLayer(routeIds);
    }

    @GetMapping("/routes")
    public GISDataService.GeoJsonFeatureCollection getRoutesLayer(@RequestParam(required = false) final List<String> routeIds) {
        return service.getRoutesLayer(routeIds);
    }

    @GetMapping("/stops")
    public GISDataService.GeoJsonFeatureCollection getStopsLayer(HttpServletResponse response) {
        response.setHeader("Cache-Control", "public, max-age=864000");
        return service.getStopsLayer();
    }

    @GetMapping("/nearest-stops")
    public List<StopWithDistance> getNearestStops(@RequestParam final double lon, @RequestParam final double lat) {
        return service.getNearestStops(lon, lat);
    }
}