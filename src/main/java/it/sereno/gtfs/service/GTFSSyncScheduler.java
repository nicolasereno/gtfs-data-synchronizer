package it.sereno.gtfs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GTFSSyncScheduler {

    private final GTFSStaticDataIngestService staticDataIngestService;
    private final GTFSStaticDataImportService staticDataImportService;
    private final GTFSDynamicDataImportService dynamicDataImportService;

    public GTFSSyncScheduler(
            final GTFSStaticDataIngestService staticDataIngestService,
            final GTFSStaticDataImportService staticDataImportService,
            final GTFSDynamicDataImportService dynamicDataImportService) {
        this.staticDataIngestService = staticDataIngestService;
        this.staticDataImportService = staticDataImportService;
        this.dynamicDataImportService = dynamicDataImportService;
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

    @Scheduled(cron = "30 * * * * *")
    public void syncCorrectionData() {
        log.debug("Starting GTFS dynamic data synchronization check");
        dynamicDataImportService.importGtfsUpdates();
        log.debug("GTFS dynamic data import completed successfully");
    }

    @Scheduled(cron = "0 * * * * *")
    public void syncVehiclePositions() {
        log.debug("Starting GTFS dynamic gis data synchronization check");
        dynamicDataImportService.fetchAndStoreVehiclePositions();
        log.debug("GTFS dynamic gis data import completed successfully");
    }
}
