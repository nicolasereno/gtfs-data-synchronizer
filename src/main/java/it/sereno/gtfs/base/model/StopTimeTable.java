package it.sereno.gtfs.base.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class StopTimeTable {

	@Id
	private String stopCode;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "stop_code")
	private List<RouteTimetable> timetable;
}
