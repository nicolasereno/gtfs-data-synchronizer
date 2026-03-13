package it.sereno.gtfs.base.repository;

import it.sereno.gtfs.base.model.Stop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StopRepository extends JpaRepository<Stop, String> {

}