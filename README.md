# GTFS Data Synchronizer

A Spring Boot application that collects, processes, and publishes public transit data for Rome (Roma Mobilità) using both GTFS static feeds and GTFS Realtime feeds. It exposes REST APIs and GeoJSON layers for integration with mapping and mobility applications.

## What It Does

- Ingests GTFS static data (stops, routes, timetables) from the Roma Mobilità feed
- Synchronizes real-time vehicle positions and trip update corrections via GTFS-RT protobuf feeds
- Publishes GeoJSON layers for vehicle positions, route paths, and bus stops
- Provides wait-time estimates for any stop, combining scheduled timetables with real-time corrections
- Offers a nearest-stops lookup by geographic coordinates

## Architecture

### Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.0.3 |
| Static data store | PostgreSQL + PostGIS |
| Dynamic data store | H2 (in-memory) |
| Spatial library | JTS (Java Topology Suite) |
| GTFS-RT parsing | gtfs-realtime-bindings (protobuf) |
| Deployment | Container image (Paketobuildpacks) on k3d (Kubernetes) |

### Dual-Datasource Design

The application maintains two separate datasources:

- **PostgreSQL + PostGIS** — persistent store for static GTFS data, processed stops, routes, and timetable tables. All raw GTFS tables are prefixed with `GTFS_`. Processed application tables (`stop`, `route`, `stop_time_table`, `route_timetable`, `vehicle_position`) live alongside them.
- **H2 in-memory** — ephemeral store for real-time trip corrections. Cleared and repopulated every 30 seconds with fresh GTFS-RT data.

### Data Collection

#### Static Data (GTFS)

A scheduled job runs every hour (at `:30`) to check for a new static feed:

1. Fetches the MD5 hash from the remote `.md5` URL
2. Compares it with the hash stored in the `GTFS_HASH` table
3. If different: downloads the ZIP, bulk-imports all GTFS text files into `GTFS_*` tables using the PostgreSQL `COPY` command, then triggers a full timetable rebuild
4. A separate job runs daily at midnight to refresh the timetable for the current service date

The timetable build derives stop arrival times from `GTFS_TRIPS`, `GTFS_STOP_TIMES`, and `GTFS_CALENDAR_DATES`, grouping them into `stop_time_table` and `route_timetable` records with pre-computed arrival time arrays.

#### Realtime Data (GTFS-RT)

Two jobs run on sub-minute schedules:

| Job | Cron | Action |
|---|---|---|
| Vehicle positions | `0 * * * * *` | Fetch protobuf feed → parse `VehiclePosition` entities → replace all rows in `vehicle_position` table |
| Trip corrections | `30 * * * * *` | Fetch protobuf feed → parse `TripUpdate` stop time updates → replace all rows in H2 `trip_correction` table |

Vehicle positions are stored as PostGIS `Point` geometries (WGS84 / SRID 4326) with bearing, speed, route ID, and timestamp.

### Data Publishing

#### REST Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/wait-times/{stopId}` | Scheduled and corrected arrival times for the next 60 minutes at a stop |
| `GET` | `/api/gis/vehicles` | GeoJSON `FeatureCollection` of current vehicle positions (optional `?routeIds=` filter) |
| `GET` | `/api/gis/routes` | GeoJSON `FeatureCollection` of route paths as LineStrings (optional `?routeIds=` filter) |
| `GET` | `/api/gis/stops` | GeoJSON `FeatureCollection` of all stops as Points (response cached, 10-day TTL) |
| `GET` | `/api/gis/nearest-stops` | Up to 10 stops within 2 km of `?lon=&lat=`, ordered by distance |

CORS is enabled for `https://smart-mobility.homelinuxserver.org` and `http://localhost:4200`.

#### Wait-Time Calculation

When `/api/wait-times/{stopId}` is called, `WaitTimeService`:

1. Queries the pre-built `stop_time_table` for all routes serving that stop
2. Filters arrivals to the current service date and the next 60 minutes
3. Checks the H2 `trip_correction` table for real-time adjustments to any of those trips
4. Returns a merged, sorted list of `WaitTime` records — each flagged `corrected=true` when a real-time override was applied

### Key Architectural Patterns

- **Lazy hash checking** — avoids downloading the full GTFS ZIP unless the MD5 changes
- **PostgreSQL COPY for bulk import** — significantly faster than row-by-row inserts for large GTFS files
- **Spatial indexing via PostGIS** — stop and vehicle geometries stored as native PostGIS types; nearest-stop queries use the KNN (`<->`) operator
- **Timetable date tracking** — `GTFS_DAY` records the last timetable generation date so the midnight job can skip work when the date has not changed
- **Real-time overlay at read time** — corrections are applied when wait times are requested, keeping the two stores independent

### Project Structure

```
src/main/java/it/sereno/gtfs/
├── GTFSDataSynchronizerApplication.java   # Entry point; triggers initial sync on startup
├── base/model/                            # JPA entities (Stop, Route, VehiclePosition, …)
├── updates/model/                         # H2 entity (TripCorrection)
├── service/
│   ├── GTFSStaticDataIngestService        # Download & bulk-import static GTFS ZIP
│   ├── GTFSStaticDataImportService        # Build processed tables from raw GTFS_* tables
│   ├── GTFSStaticDataSyncScheduler        # Hourly hash check + midnight timetable refresh
│   ├── GTFSDynamicDataImportService       # Parse GTFS-RT protobuf feeds
│   ├── GTFSDynamicDataSyncScheduler       # Sub-minute realtime sync jobs
│   ├── WaitTimeService                    # Scheduled + corrected arrival times
│   └── GISDataService                     # GeoJSON layer generation
├── controller/
│   ├── StopTimesController                # /api/wait-times
│   └── GISDataController                  # /api/gis/*
└── config/                                # Datasource and transaction manager configuration
src/main/resources/
├── application.yaml                       # Production configuration
├── application-dev.yaml                   # Local development overrides (port 8080, localhost DB)
└── schema.sql                             # DDL for GTFS_* and application tables
```

### Deployment

The application is packaged as an OCI container image (`192.168.1.51:5000/gtfs-synchronizer:latest`) using Paketobuildpacks and deployed to a k3d (lightweight Kubernetes) cluster. It connects to an external PostGIS instance at hostname `postgis` and listens on port 80. The H2 console is enabled for inspecting in-memory dynamic data.

For local development, activate the `dev` profile to point at `localhost:5433` and run on port 8080.
