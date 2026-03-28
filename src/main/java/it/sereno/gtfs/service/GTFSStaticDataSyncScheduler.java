package it.sereno.gtfs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GTFSStaticDataSyncScheduler {

    private final GTFSStaticDataIngestService staticDataIngestService;
    private final GTFSStaticDataImportService staticDataImportService;

    public GTFSStaticDataSyncScheduler(
            final GTFSStaticDataIngestService staticDataIngestService,
            final GTFSStaticDataImportService staticDataImportService) {
        this.staticDataIngestService = staticDataIngestService;
        this.staticDataImportService = staticDataImportService;
    }

    @Scheduled(cron = "0 30 * * * *")
    public void syncGTFSStaticData() {
        log.debug("Starting GTFS static data synchronization check");
        final String newHash = staticDataIngestService.dataImportNeeded();
        if (newHash != null) {
            log.debug("New GTFS static data found (hash: {}), starting import", newHash);
            staticDataIngestService.importStaticData(newHash);
            staticDataImportService.importStopData();
            staticDataImportService.importRouteData();
            staticDataImportService.importStopTimetableData();
            log.debug("GTFS static data import completed successfully");
        } else {
            log.debug("No GTFS static data update needed");
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void syncTimetableData() {
        log.debug("Starting GTFS timetable data synchronization check");
        staticDataImportService.importStopTimetableData();
        log.debug("GTFS timetable data import completed successfully");
    }

}
