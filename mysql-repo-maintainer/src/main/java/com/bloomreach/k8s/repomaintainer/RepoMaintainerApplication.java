package com.bloomreach.k8s.repomaintainer;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1PodList;
import io.kubernetes.client.util.Config;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootApplication
public class RepoMaintainerApplication implements CommandLineRunner {

//    @Value("${spring.datasource.url}")
//    private String dataSourceUrl;
//
//    @Value("${spring.datasource.username}")
//    private String userName;
//
//    @Value("${spring.datasource.password}")
//    private String password;

    @Value("#{environment.APP_NAMESPACE}")
    private String namespace;

    @Value("#{environment.BRXM_SELECTOR}")
    private String brxmSelector;

    private static final Logger log = LoggerFactory.getLogger(RepoMaintainerApplication.class);

    final JdbcTemplate jdbcTemplate;

    private static final String REPOSITORY_LOCAL_REVISIONS_CLEANUP = "DELETE FROM REPOSITORY_LOCAL_REVISIONS WHERE JOURNAL_ID NOT IN (%s) AND JOURNAL_ID NOT LIKE '_HIPPO_EXTERNAL%%'";
    private static final String REPOSITORY_JOURNAL_CLEANUP = "DELETE FROM REPOSITORY_JOURNAL WHERE REVISION_ID < ANY (SELECT min(REVISION_ID) FROM REPOSITORY_LOCAL_REVISIONS)";
    private static final String REPOSITORY_JOURNAL_OPTIMIZE = "OPTIMIZE TABLE REPOSITORY_JOURNAL";

    public RepoMaintainerApplication(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public static void main(String[] args) {
        SpringApplication.run(RepoMaintainerApplication.class, args);
    }


    @Override
    public void run(String... args) throws IOException, ApiException {
//        log.info(dataSourceUrl + userName + password);
        validateConfiguration();
        CoreV1Api api = getK8sApiClient();
        final List<String> activeBrxmPods = getActiveBrxmPods(api);
        runCleanupQueries(activeBrxmPods);
    }

    private void runCleanupQueries(final List<String> activeBrxmPods) {
        if (!activeBrxmPods.isEmpty()) {
            String activeIdsJoined = String.join(",", activeBrxmPods.stream().map(id -> "'" + id + "'").toArray(String[]::new));
            String formattedLocalRevisionCleanupQuery = String.format(REPOSITORY_LOCAL_REVISIONS_CLEANUP, activeIdsJoined);
            log.info("Executing local revisions table query...");
            log.info(formattedLocalRevisionCleanupQuery);
            jdbcTemplate.execute(formattedLocalRevisionCleanupQuery);
            log.info("Executing repository journal cleanup...");
            log.info(REPOSITORY_JOURNAL_CLEANUP);
            jdbcTemplate.execute(REPOSITORY_JOURNAL_CLEANUP);
            log.info("Executing repository journal vacuum query...");
            jdbcTemplate.execute(REPOSITORY_JOURNAL_OPTIMIZE);
        } else {
            log.warn("List of brxm pod names is empty. Skipping running cleanup queries.");
        }
    }


    private CoreV1Api getK8sApiClient() throws IOException {
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        return new CoreV1Api();
    }


    private List<String> getActiveBrxmPods(final CoreV1Api api) throws ApiException {
        V1PodList list = api.listNamespacedPod(namespace, null, null, null, null, brxmSelector, null, null, null, null);
        if (list == null || list.getItems().isEmpty()) {
            log.error("Couldn't find any brxm pods with selector: " + brxmSelector + " Skipping running db cleanup queries");
            return Collections.emptyList();
        }
        return list.getItems().stream()
                .map(item -> item.getMetadata().getName())
                .collect(Collectors.toList());
    }

    private void validateConfiguration() {
        if (StringUtils.isBlank(namespace)) {
            throw new RuntimeException("APP_NAMESPACE env variable was blank");
        }
        if (StringUtils.isBlank(brxmSelector)) {
            throw new RuntimeException("BRXM_SELECTOR env variable was blank");
        }
    }
}
