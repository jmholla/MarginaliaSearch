package nu.marginalia.loading.domains;

import com.google.common.collect.Lists;
import nu.marginalia.io.processed.DomainLinkRecordParquetFileWriter;
import nu.marginalia.io.processed.DomainRecordParquetFileWriter;
import nu.marginalia.io.processed.ProcessedDataFileNames;
import nu.marginalia.loader.DbTestUtil;
import nu.marginalia.loading.LoaderInputData;
import nu.marginalia.model.processed.DomainLinkRecord;
import nu.marginalia.model.processed.DomainRecord;
import nu.marginalia.process.control.ProcessAdHocTaskHeartbeat;
import nu.marginalia.process.control.ProcessHeartbeat;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Tag("slow")
@Testcontainers
class DomainLoaderServiceTest {
    List<Path> toDelete = new ArrayList<>();
    ProcessHeartbeat heartbeat;

    @Container
    static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>("mariadb")
            .withDatabaseName("WMSA_prod")
            .withUsername("wmsa")
            .withPassword("wmsa")
            .withInitScript("db/migration/V23_06_0_000__base.sql")
            .withNetworkAliases("mariadb");

    @BeforeEach
    public void setUp() {
        heartbeat = Mockito.mock(ProcessHeartbeat.class);

        Mockito.when(heartbeat.createAdHocTaskHeartbeat(Mockito.anyString())).thenReturn(
                Mockito.mock(ProcessAdHocTaskHeartbeat.class)
        );
    }

    @AfterEach
    public void tearDown() throws IOException {
        for (var path : Lists.reverse(toDelete)) {
            Files.deleteIfExists(path);
        }

        toDelete.clear();
    }
    @Test
    void readDomainNames() throws IOException {
        Path workDir = Files.createTempDirectory(getClass().getSimpleName());
        Path parquetFile1 = ProcessedDataFileNames.domainFileName(workDir, 0);
        Path parquetFile2 = ProcessedDataFileNames.domainFileName(workDir, 1);
        Path parquetFile3 = ProcessedDataFileNames.domainLinkFileName(workDir, 0);

        toDelete.add(workDir);
        toDelete.add(parquetFile1);
        toDelete.add(parquetFile2);
        toDelete.add(parquetFile3);

        // Prep by creating two parquet files with domains
        // and one with domain links

        List<String> domains1 = List.of("www.marginalia.nu", "memex.marginalia.nu", "search.marginalia.nu");
        List<String> domains2 = List.of("wiby.me", "www.mojeek.com", "www.altavista.com");
        List<String> linkDomains = List.of("maya.land", "xkcd.com", "aaronsw.com");

        try (var pw = new DomainRecordParquetFileWriter(parquetFile1)) {
            for (var domain : domains1) {
                pw.write(dr(domain));
            }
        }
        try (var pw = new DomainRecordParquetFileWriter(parquetFile2)) {
            for (var domain : domains2) {
                pw.write(dr(domain));
            }
        }
        try (var pw = new DomainLinkRecordParquetFileWriter(parquetFile3)) {
            for (var domain : linkDomains) {
                pw.write(dl(domain));
            }
        }
        // Read them
        var domainService = new DomainLoaderService(null);
        var domainNames = domainService.readDomainNames(new LoaderInputData(workDir, 2));

        // Verify
        Set<String> expectedDomains = Stream.of(domains1, domains2, linkDomains)
                .flatMap(List::stream)
                .collect(Collectors.toSet());
        assertEquals(expectedDomains, domainNames);
    }

    @Test
    void getDatabaseIds() {
        try (var dataSource = DbTestUtil.getConnection(mariaDBContainer.getJdbcUrl())) {
            var domainService = new DomainLoaderService(dataSource);

            for (int i = 0; i < 2; i++) {
                // run the test case twice to cover both the insert and query cases
                System.out.println("Case " + i);

                var domains = List.of("memex.marginalia.nu", "www.marginalia.nu", "search.marginalia.nu", "wiby.me");
                var data = domainService.getDatabaseIds(domains);

                Map<String, Integer> ids = new HashMap<>();

                for (String domain : domains) {
                    ids.put(domain, data.getDomainId(domain));
                }

                // Verify we got 4 domain IDs for the provided inputs
                var entries = new HashSet<>(ids.values());
                assertEquals(4, entries.size());
                assertEquals(Set.of(1,2,3,4), entries); // this may be fragile?
            }

        } catch (SQLException e) {
            Assertions.fail(e);
        }
    }

    private DomainRecord dr(String domainName) {
        return new DomainRecord(domainName, 0, 0, 0, null, null, null, null);
    }

    private DomainLinkRecord dl(String destDomainName) {
        return new DomainLinkRecord("www.marginalia.nu", destDomainName);
    }
}