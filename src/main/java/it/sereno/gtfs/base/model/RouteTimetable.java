package it.sereno.gtfs.base.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class RouteTimetable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

    private String routeIdentifier;
    private String directionDescription;

    @ElementCollection
    private List<ArrivalTime> arrivalTimes;

	@Embeddable
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ArrivalTime {
		private String tripIdentifier;
		private String arrivalTime;
	}

}

