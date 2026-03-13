package it.sereno.gtfs.base.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stop {

	@Id
	private String id;
	private String name;
	private Point location;
}
