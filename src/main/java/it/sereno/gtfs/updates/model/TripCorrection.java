package it.sereno.gtfs.updates.model;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class TripCorrection {

    @Id
    private String tripIdentifier;
    // stopIdentifier -> correction
    @ElementCollection
    private Map<String, String> corrections;
}
