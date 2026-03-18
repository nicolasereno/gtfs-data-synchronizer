package it.sereno.gtfs.base.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.LineString;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route {

	@Id
	private String shapeId;
	private String routeId;
	private boolean outbound;
	private String shortName;
	private LineString path;
}
