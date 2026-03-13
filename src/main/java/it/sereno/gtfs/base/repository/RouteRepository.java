package it.sereno.gtfs.base.repository;

import it.sereno.gtfs.base.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, String> {

	List<Route> findByRouteIdIn( List<String> routeIds );
}