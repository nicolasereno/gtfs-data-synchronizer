package it.sereno.gtfs.service;


import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
@Slf4j
public class GTFSStaticDataIngestService {

	private final DataSource dataSource;
	private final JdbcTemplate jdbcTemplate;
	private final String gtfsStaticDataUrl;
	private final String gtfsStaticDataHashUrl;

	public GTFSStaticDataIngestService(
			final @Qualifier( "baseDataSource" ) DataSource baseDataSource,
			final JdbcTemplate baseJdbcTemplate,
			final @Value( "${gtfs.static-data-url}" ) String gtfsStaticDataUrl,
			final @Value( "${gtfs.static-data-md5-url}" ) String gtfsStaticDataHashUrl ) {
		this.dataSource = baseDataSource;
		this.jdbcTemplate = baseJdbcTemplate;
		this.gtfsStaticDataUrl = gtfsStaticDataUrl;
		this.gtfsStaticDataHashUrl = gtfsStaticDataHashUrl;
	}

	public String dataImportNeeded() {

		final String hash = readHashFromRemoteUrl();
		if ( hash == null ) {
			log.warn( "Remote hash is null, data import cannot be performed" );
			return null;
		}
		final String localHash = readHashFromDatabase();
		if ( localHash == null || !localHash.equals( hash ) ) {
			return hash;
		}
		return null;
	}

	@SneakyThrows
	private String readHashFromDatabase() {
		jdbcTemplate.execute( "CREATE TABLE IF NOT EXISTS gtfs_hash (hash TEXT)" );
		final List<String> hashes = jdbcTemplate.queryForList( "SELECT hash FROM gtfs_hash", String.class );
		return hashes.isEmpty()
				? null
				: hashes.get( 0 );
	}

	@SneakyThrows
	private String readHashFromRemoteUrl() {
		try ( final InputStream is = new URI( gtfsStaticDataHashUrl ).toURL().openStream();
				final BufferedReader br = new BufferedReader( new InputStreamReader( is, StandardCharsets.UTF_8 ) ) ) {
			return br.lines().collect( Collectors.joining( "" ) ).split( "\\s+" )[0];
		} catch ( Exception e ) {
			log.error( "Error reading remote hash from {}: {}", gtfsStaticDataHashUrl, e.getMessage() );
			return null;
		}
	}

	private void writeHashToDatabase( final String hash ) {
		jdbcTemplate.execute( "CREATE TABLE IF NOT EXISTS gtfs_hash (hash TEXT)" );
		jdbcTemplate.execute( "DELETE FROM gtfs_hash" );
		jdbcTemplate.update( "INSERT INTO gtfs_hash (hash) VALUES (?)", hash );
	}

	@SneakyThrows
	public void importStaticData( final String newHash ) {

		log.info( "Importing GTFS static data from {}", gtfsStaticDataUrl );

		Path tempFile = Files.createTempFile( "remote-", ".zip" );

		try ( final InputStream remoteStream = new URI( gtfsStaticDataUrl ).toURL().openStream();
				final Connection connection = dataSource.getConnection();
				final Statement statement = connection.createStatement();
		) {
			Files.copy( remoteStream, tempFile, StandardCopyOption.REPLACE_EXISTING );

			try ( final ZipFile zipFile = new ZipFile( tempFile.toFile() ) ) {

				final List<String> tables = List.of( "trips", "stop_times", "agency", "stops", "routes", "shapes", "calendar_dates" );

				CopyManager copyManager = new CopyManager( connection.unwrap( BaseConnection.class ) );

				for ( String table : tables ) {
					statement.executeUpdate( "TRUNCATE TABLE " + table );

					try (
							final InputStream fileStream = open( zipFile, table + ".txt" );
							final BufferedReader br = new BufferedReader( new InputStreamReader( fileStream ) );
					) {
						String header = br.readLine();

						String sql = """
								COPY %s (%s)
								FROM STDIN
								WITH ( FORMAT csv, HEADER true )
								""".formatted( table, header );

						copyManager.copyIn( sql, br );
					}
				}

				writeHashToDatabase( newHash );
				log.info( "GTFS static data import completed!" );
			} finally {
				Files.deleteIfExists( tempFile );
			}
		}
	}

	@SneakyThrows
	private InputStream open( final ZipFile zipFile, final String internalPath ) {
		ZipEntry entry = zipFile.getEntry( internalPath );
		return zipFile.getInputStream( entry );
	}
}