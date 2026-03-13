package it.sereno.gtfs.base.model;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

import java.time.Instant;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehiclePosition {

	@Id
	private String vehicleId;
	private String tripId;
	private String routeId;
	private boolean outbound;

	private Double bearing;
	private Double speed;

	private Instant timestamp;

	private Point location;
}