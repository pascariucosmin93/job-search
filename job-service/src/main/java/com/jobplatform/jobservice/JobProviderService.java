package com.jobplatform.jobservice;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class JobProviderService {

  private static final Logger LOG = LoggerFactory.getLogger(JobProviderService.class);

  private static final List<JobListing> FALLBACK_JOBS = List.of(
      new JobListing("fallback-1", "Senior Platform Engineer", "CloudScale Labs", "Remote", "Full-time", true, List.of("Kubernetes", "Terraform", "GitOps"), "https://example.com/jobs/senior-platform-engineer", "Fallback", "2026-04-01"),
      new JobListing("fallback-2", "Java Backend Engineer", "Nimbus Systems", "Berlin", "Hybrid", false, List.of("Java", "Spring Boot", "PostgreSQL"), "https://example.com/jobs/java-backend-engineer", "Fallback", "2026-03-30"),
      new JobListing("fallback-3", "DevOps Engineer", "OrbitStack", "Remote", "Contract", true, List.of("CI/CD", "Docker", "AWS"), "https://example.com/jobs/devops-engineer", "Fallback", "2026-03-28"),
      new JobListing("fallback-4", "Frontend Engineer", "Pulse Apps", "Bucharest", "Full-time", false, List.of("React", "Next.js", "TypeScript"), "https://example.com/jobs/frontend-engineer", "Fallback", "2026-03-25")
  );

  private static final String SOURCE_ARBEITNOW = "Arbeitnow";
  private static final String SOURCE_REMOTIVE = "Remotive";
  private static final String SOURCE_ADZUNA = "Adzuna";
  private static final String SOURCE_GREENHOUSE = "Greenhouse";
  private static final String SOURCE_LEVER = "Lever";
  private static final String SOURCE_ASHBY = "Ashby";

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final Duration cacheTtl;
  private final boolean excludeUs;

  private final boolean arbeitnowEnabled;
  private final String arbeitnowBaseUrl;

  private final boolean remotiveEnabled;
  private final String remotiveBaseUrl;

  private final boolean adzunaEnabled;
  private final String adzunaBaseUrl;
  private final String adzunaCountry;
  private final String adzunaAppId;
  private final String adzunaAppKey;
  private final String adzunaDefaultQuery;
  private final int adzunaPage;

  private final boolean greenhouseEnabled;
  private final List<String> greenhouseBoardTokens;

  private final boolean leverEnabled;
  private final String leverBaseUrl;
  private final List<String> leverSites;

  private final boolean ashbyEnabled;
  private final String ashbyBaseUrl;
  private final List<String> ashbyBoardNames;

  private volatile List<JobListing> cachedJobs = FALLBACK_JOBS;
  private volatile Instant cacheUpdatedAt = Instant.EPOCH;
  private final Object refreshLock = new Object();

  public JobProviderService(
      RestClient.Builder restClientBuilder,
      ObjectMapper objectMapper,
      @Value("${jobs.api.timeout-ms:5000}") int timeoutMs,
      @Value("${jobs.api.cache-ttl-seconds:900}") long cacheTtlSeconds,
      @Value("${jobs.api.filters.exclude-us:true}") boolean excludeUs,
      @Value("${jobs.api.arbeitnow.enabled:true}") boolean arbeitnowEnabled,
      @Value("${jobs.api.arbeitnow.base-url:https://www.arbeitnow.com}") String arbeitnowBaseUrl,
      @Value("${jobs.api.remotive.enabled:true}") boolean remotiveEnabled,
      @Value("${jobs.api.remotive.base-url:https://remotive.com}") String remotiveBaseUrl,
      @Value("${jobs.api.adzuna.enabled:true}") boolean adzunaEnabled,
      @Value("${jobs.api.adzuna.base-url:https://api.adzuna.com}") String adzunaBaseUrl,
      @Value("${jobs.api.adzuna.country:us}") String adzunaCountry,
      @Value("${jobs.api.adzuna.page:1}") int adzunaPage,
      @Value("${jobs.api.adzuna.default-query:software engineer}") String adzunaDefaultQuery,
      @Value("${jobs.api.adzuna.app-id:}") String adzunaAppId,
      @Value("${jobs.api.adzuna.app-key:}") String adzunaAppKey,
      @Value("${jobs.api.greenhouse.enabled:true}") boolean greenhouseEnabled,
      @Value("${jobs.api.greenhouse.board-tokens:}") String greenhouseBoardTokens,
      @Value("${jobs.api.lever.enabled:true}") boolean leverEnabled,
      @Value("${jobs.api.lever.base-url:https://api.lever.co}") String leverBaseUrl,
      @Value("${jobs.api.lever.sites:}") String leverSites,
      @Value("${jobs.api.ashby.enabled:true}") boolean ashbyEnabled,
      @Value("${jobs.api.ashby.base-url:https://api.ashbyhq.com}") String ashbyBaseUrl,
      @Value("${jobs.api.ashby.board-names:}") String ashbyBoardNames
  ) {
    var requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
    requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));

    this.restClient = restClientBuilder
        .requestFactory(requestFactory)
        .build();
    this.objectMapper = objectMapper;

    this.cacheTtl = Duration.ofSeconds(Math.max(60, cacheTtlSeconds));
    this.excludeUs = excludeUs;

    this.arbeitnowEnabled = arbeitnowEnabled;
    this.arbeitnowBaseUrl = sanitizeBaseUrl(arbeitnowBaseUrl);

    this.remotiveEnabled = remotiveEnabled;
    this.remotiveBaseUrl = sanitizeBaseUrl(remotiveBaseUrl);

    this.adzunaEnabled = adzunaEnabled;
    this.adzunaBaseUrl = sanitizeBaseUrl(adzunaBaseUrl);
    this.adzunaCountry = defaultIfBlank(adzunaCountry, "us").toLowerCase();
    this.adzunaPage = Math.max(1, adzunaPage);
    this.adzunaDefaultQuery = defaultIfBlank(adzunaDefaultQuery, "software engineer");
    this.adzunaAppId = adzunaAppId == null ? "" : adzunaAppId.strip();
    this.adzunaAppKey = adzunaAppKey == null ? "" : adzunaAppKey.strip();

    this.greenhouseEnabled = greenhouseEnabled;
    this.greenhouseBoardTokens = splitCsv(greenhouseBoardTokens);

    this.leverEnabled = leverEnabled;
    this.leverBaseUrl = sanitizeBaseUrl(leverBaseUrl);
    this.leverSites = splitCsv(leverSites);

    this.ashbyEnabled = ashbyEnabled;
    this.ashbyBaseUrl = sanitizeBaseUrl(ashbyBaseUrl);
    this.ashbyBoardNames = splitCsv(ashbyBoardNames);
  }

  public List<JobListing> listJobs(String query, String location, Boolean remote) {
    List<JobListing> jobs = getCachedOrRefresh();

    String normalizedQuery = normalize(query);
    String normalizedLocation = normalize(location);

    return jobs.stream()
        .filter(job -> !this.excludeUs || !isUnitedStatesLocation(job.location()))
        .filter(job -> matchesQuery(job, normalizedQuery))
        .filter(job -> matchesLocation(job, normalizedLocation))
        .filter(job -> matchesRemote(job, remote))
        .toList();
  }

  private List<JobListing> getCachedOrRefresh() {
    if (isCacheFresh()) {
      return cachedJobs;
    }

    synchronized (refreshLock) {
      if (isCacheFresh()) {
        return cachedJobs;
      }

      List<JobListing> refreshed = refreshAllProviders();
      cachedJobs = refreshed.isEmpty() ? FALLBACK_JOBS : refreshed;
      cacheUpdatedAt = Instant.now();
      return cachedJobs;
    }
  }

  private boolean isCacheFresh() {
    return Duration.between(cacheUpdatedAt, Instant.now()).compareTo(cacheTtl) < 0;
  }

  private List<JobListing> refreshAllProviders() {
    LinkedHashMap<String, JobListing> deduped = new LinkedHashMap<>();

    List<JobListing> arbeitnow = fetchArbeitnowJobs();
    List<JobListing> remotive = fetchRemotiveJobs();
    List<JobListing> adzuna = fetchAdzunaJobs();
    List<JobListing> greenhouse = fetchGreenhouseJobs();
    List<JobListing> lever = fetchLeverJobs();
    List<JobListing> ashby = fetchAshbyJobs();

    addAll(deduped, arbeitnow);
    addAll(deduped, remotive);
    addAll(deduped, adzuna);
    addAll(deduped, greenhouse);
    addAll(deduped, lever);
    addAll(deduped, ashby);

    LOG.info(
        "Job refresh completed: Arbeitnow={}, Remotive={}, Adzuna={}, Greenhouse={}, Lever={}, Ashby={}, DedupedTotal={}",
        arbeitnow.size(),
        remotive.size(),
        adzuna.size(),
        greenhouse.size(),
        lever.size(),
        ashby.size(),
        deduped.size()
    );

    return List.copyOf(deduped.values());
  }

  private static void addAll(Map<String, JobListing> target, List<JobListing> jobs) {
    for (JobListing job : jobs) {
      String key = dedupeKey(job);
      target.putIfAbsent(key, job);
    }
  }

  private static String dedupeKey(JobListing job) {
    return normalize(job.title()) + "|" + normalize(job.company()) + "|" + normalize(job.location());
  }

  private List<JobListing> fetchArbeitnowJobs() {
    if (!arbeitnowEnabled) {
      return List.of();
    }

    String url = arbeitnowBaseUrl + "/api/job-board-api";
    try {
      Map<String, Object> payload = fetchAsMap(url);
      if (!(payload.get("data") instanceof List<?> data)) {
        return List.of();
      }
      return data.stream()
          .filter(Map.class::isInstance)
          .map(Map.class::cast)
          .map(this::toArbeitnowJob)
          .flatMap(Optional::stream)
          .toList();
    } catch (Exception ex) {
      LOG.warn("Arbeitnow provider unavailable: {}", ex.getMessage());
      return List.of();
    }
  }

  private List<JobListing> fetchRemotiveJobs() {
    if (!remotiveEnabled) {
      return List.of();
    }

    String url = remotiveBaseUrl + "/api/remote-jobs";
    try {
      Map<String, Object> payload = fetchAsMap(url);
      if (!(payload.get("jobs") instanceof List<?> jobs)) {
        return List.of();
      }
      return jobs.stream()
          .filter(Map.class::isInstance)
          .map(Map.class::cast)
          .map(this::toRemotiveJob)
          .flatMap(Optional::stream)
          .toList();
    } catch (Exception ex) {
      LOG.warn("Remotive provider unavailable: {}", ex.getMessage());
      return List.of();
    }
  }

  private List<JobListing> fetchAdzunaJobs() {
    if (!adzunaEnabled) {
      return List.of();
    }
    if (adzunaAppId.isBlank() || adzunaAppKey.isBlank()) {
      LOG.info("Adzuna provider skipped: missing app id/app key");
      return List.of();
    }

    String url = UriComponentsBuilder.fromHttpUrl(adzunaBaseUrl + "/v1/api/jobs/" + adzunaCountry + "/search/" + adzunaPage)
        .queryParam("app_id", adzunaAppId)
        .queryParam("app_key", adzunaAppKey)
        .queryParam("what", adzunaDefaultQuery)
        .queryParam("results_per_page", 50)
        .toUriString();
    try {
      Map<String, Object> payload = fetchAsMapFromTextCompatibleEndpoint(url, SOURCE_ADZUNA);
      if (!(payload.get("results") instanceof List<?> results)) {
        LOG.warn("Adzuna response does not contain 'results' array");
        return List.of();
      }
      List<JobListing> mapped = results.stream()
          .filter(Map.class::isInstance)
          .map(Map.class::cast)
          .map(this::toAdzunaJob)
          .flatMap(Optional::stream)
          .toList();
      LOG.info("Adzuna fetch succeeded: {} jobs mapped", mapped.size());
      return mapped;
    } catch (Exception ex) {
      LOG.warn("Adzuna provider unavailable: {}", ex.getMessage());
      return List.of();
    }
  }

  private List<JobListing> fetchGreenhouseJobs() {
    if (!greenhouseEnabled || greenhouseBoardTokens.isEmpty()) {
      return List.of();
    }

    return greenhouseBoardTokens.stream()
        .flatMap(boardToken -> {
          try {
            String url = "https://boards-api.greenhouse.io/v1/boards/" + boardToken + "/jobs?content=true";
            Map<String, Object> payload = fetchAsMap(url);
            if (!(payload.get("jobs") instanceof List<?> jobs)) {
              return List.<JobListing>of().stream();
            }
            return jobs.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(job -> toGreenhouseJob(job, boardToken))
                .flatMap(Optional::stream);
          } catch (Exception ex) {
            LOG.warn("Greenhouse provider unavailable for board {}: {}", boardToken, ex.getMessage());
            return List.<JobListing>of().stream();
          }
        })
        .toList();
  }

  private List<JobListing> fetchLeverJobs() {
    if (!leverEnabled || leverSites.isEmpty()) {
      return List.of();
    }

    return leverSites.stream()
        .flatMap(site -> {
          try {
            String url = leverBaseUrl + "/v0/postings/" + site + "?mode=json&limit=100";
            List<Map<String, Object>> payload = fetchAsListOfMaps(url);
            return payload.stream()
                .map(job -> toLeverJob(job, site))
                .flatMap(Optional::stream);
          } catch (Exception ex) {
            LOG.warn("Lever provider unavailable for site {}: {}", site, ex.getMessage());
            return List.<JobListing>of().stream();
          }
        })
        .toList();
  }

  private List<JobListing> fetchAshbyJobs() {
    if (!ashbyEnabled || ashbyBoardNames.isEmpty()) {
      return List.of();
    }

    return ashbyBoardNames.stream()
        .flatMap(board -> {
          try {
            String url = ashbyBaseUrl + "/posting-api/job-board/" + board;
            Map<String, Object> payload = fetchAsMap(url);
            if (!(payload.get("jobs") instanceof List<?> jobs)) {
              return List.<JobListing>of().stream();
            }
            return jobs.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(job -> toAshbyJob(job, board))
                .flatMap(Optional::stream);
          } catch (Exception ex) {
            LOG.warn("Ashby provider unavailable for board {}: {}", board, ex.getMessage());
            return List.<JobListing>of().stream();
          }
        })
        .toList();
  }

  private Optional<JobListing> toArbeitnowJob(Map<?, ?> source) {
    String title = asString(source.get("title"));
    String company = asString(source.get("company_name"));
    String jobUrl = asString(source.get("url"));

    if (title.isBlank() || company.isBlank()) {
      return Optional.empty();
    }

    String location = formatLocation(source.get("location"));
    List<String> tags = toStringList(source.get("tags"));
    List<String> jobTypes = toStringList(source.get("job_types"));

    String type = jobTypes.isEmpty() ? "Not specified" : String.join(", ", jobTypes);
    boolean remote = asBoolean(source.get("remote"));

    String idSeed = asString(source.get("slug"));
    if (idSeed.isBlank()) {
      idSeed = (title + "|" + company + "|" + jobUrl).toLowerCase();
    }

    return Optional.of(new JobListing(
        hashId(idSeed),
        title,
        company,
        location.isBlank() ? "Location not specified" : location,
        type,
        remote,
        tags,
        jobUrl,
        SOURCE_ARBEITNOW,
        asString(source.get("created_at"))
    ));
  }

  private Optional<JobListing> toRemotiveJob(Map<?, ?> source) {
    String title = asString(source.get("title"));
    String company = asString(source.get("company_name"));
    String jobUrl = asString(source.get("url"));
    if (title.isBlank() || company.isBlank() || jobUrl.isBlank()) {
      return Optional.empty();
    }

    String location = asString(source.get("candidate_required_location"));
    String type = asString(source.get("job_type")).replace('_', ' ');
    type = type.isBlank() ? "Not specified" : type;

    return Optional.of(new JobListing(
        hashId(SOURCE_REMOTIVE + "|" + asString(source.get("id")) + "|" + jobUrl),
        title,
        company,
        location.isBlank() ? "Remote" : location,
        type,
        true,
        toStringList(source.get("tags")),
        jobUrl,
        SOURCE_REMOTIVE,
        asString(source.get("publication_date"))
    ));
  }

  private Optional<JobListing> toAdzunaJob(Map<?, ?> source) {
    String title = asString(source.get("title"));
    String jobUrl = asString(source.get("redirect_url"));
    String company = nestedValue(source, "company", "display_name");
    String location = nestedValue(source, "location", "display_name");

    if (title.isBlank() || company.isBlank() || jobUrl.isBlank()) {
      return Optional.empty();
    }

    String type = List.of(asString(source.get("contract_type")), asString(source.get("contract_time"))).stream()
        .filter(v -> !v.isBlank())
        .collect(Collectors.joining(", "));
    if (type.isBlank()) {
      type = "Not specified";
    }

    String category = nestedValue(source, "category", "label");
    List<String> tags = category.isBlank() ? List.of() : List.of(category);
    boolean remote = location.toLowerCase().contains("remote")
        || asString(source.get("description")).toLowerCase().contains("remote");

    return Optional.of(new JobListing(
        hashId(SOURCE_ADZUNA + "|" + asString(source.get("id")) + "|" + jobUrl),
        title,
        company,
        location.isBlank() ? "Location not specified" : location,
        type,
        remote,
        tags,
        jobUrl,
        SOURCE_ADZUNA,
        asString(source.get("created"))
    ));
  }

  private Optional<JobListing> toGreenhouseJob(Map<?, ?> source, String boardToken) {
    String title = asString(source.get("title"));
    String jobUrl = asString(source.get("absolute_url"));
    String location = nestedValue(source, "location", "name");
    if (title.isBlank() || jobUrl.isBlank()) {
      return Optional.empty();
    }

    List<String> tags = List.of(
        nestedValue(source, "departments", "0", "name"),
        nestedValue(source, "offices", "0", "name")
    ).stream().filter(v -> !v.isBlank()).toList();

    boolean remote = location.toLowerCase().contains("remote");

    return Optional.of(new JobListing(
        hashId(SOURCE_GREENHOUSE + "|" + asString(source.get("id")) + "|" + jobUrl),
        title,
        boardToken,
        location.isBlank() ? "Location not specified" : location,
        "Not specified",
        remote,
        tags,
        jobUrl,
        SOURCE_GREENHOUSE,
        asString(source.get("updated_at"))
    ));
  }

  private Optional<JobListing> toLeverJob(Map<?, ?> source, String site) {
    String title = asString(source.get("text"));
    String jobUrl = asString(source.get("hostedUrl"));
    if (title.isBlank() || jobUrl.isBlank()) {
      return Optional.empty();
    }

    String location = nestedValue(source, "categories", "location");
    String commitment = nestedValue(source, "categories", "commitment");
    String team = nestedValue(source, "categories", "team");
    boolean remote = location.toLowerCase().contains("remote");

    List<String> tags = List.of(team).stream().filter(v -> !v.isBlank()).toList();

    return Optional.of(new JobListing(
        hashId(SOURCE_LEVER + "|" + asString(source.get("id")) + "|" + jobUrl),
        title,
        site,
        location.isBlank() ? "Location not specified" : location,
        commitment.isBlank() ? "Not specified" : commitment,
        remote,
        tags,
        jobUrl,
        SOURCE_LEVER,
        asString(source.get("createdAt"))
    ));
  }

  private Optional<JobListing> toAshbyJob(Map<?, ?> source, String board) {
    String title = asString(source.get("title"));
    String applyUrl = asString(source.get("applyUrl"));
    String jobUrl = asString(source.get("jobUrl"));
    String location = asString(source.get("location"));
    String employmentType = asString(source.get("employmentType"));
    String workplaceType = asString(source.get("workplaceType"));
    boolean remote = asBoolean(source.get("isRemote")) || "Remote".equalsIgnoreCase(workplaceType);

    if (title.isBlank() || (applyUrl.isBlank() && jobUrl.isBlank())) {
      return Optional.empty();
    }

    List<String> tags = List.of(asString(source.get("department")), asString(source.get("team")), workplaceType).stream()
        .filter(v -> !v.isBlank())
        .toList();

    return Optional.of(new JobListing(
        hashId(SOURCE_ASHBY + "|" + title + "|" + board + "|" + applyUrl + "|" + jobUrl),
        title,
        board,
        location.isBlank() ? "Location not specified" : location,
        employmentType.isBlank() ? "Not specified" : employmentType,
        remote,
        tags,
        applyUrl.isBlank() ? jobUrl : applyUrl,
        SOURCE_ASHBY,
        asString(source.get("publishedAt"))
    ));
  }

  private Map<String, Object> fetchAsMap(String url) {
    Map<String, Object> payload = restClient.get()
        .uri(url)
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
    return payload == null ? Map.of() : payload;
  }

  private List<Map<String, Object>> fetchAsListOfMaps(String url) {
    List<Map<String, Object>> payload = restClient.get()
        .uri(url)
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
    return payload == null ? List.of() : payload;
  }

  private Map<String, Object> fetchAsMapFromTextCompatibleEndpoint(String url, String providerName) {
    String body = restClient.get()
        .uri(url)
        .accept(MediaType.APPLICATION_JSON)
        .header("User-Agent", "job-platform-job-service/1.0")
        .retrieve()
        .body(String.class);

    if (body == null || body.isBlank()) {
      return Map.of();
    }

    try {
      return objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
    } catch (Exception ex) {
      String preview = body.length() > 200 ? body.substring(0, 200) + "..." : body;
      LOG.warn("{} provider returned non-JSON body preview: {}", providerName, preview);
      throw new IllegalStateException(providerName + " response is not valid JSON", ex);
    }
  }

  private static String nestedValue(Map<?, ?> source, String... path) {
    Object current = source;
    for (String key : path) {
      if (current == null) {
        return "";
      }
      if (current instanceof List<?> list) {
        try {
          int index = Integer.parseInt(key);
          if (index < 0 || index >= list.size()) {
            return "";
          }
          current = list.get(index);
          continue;
        } catch (NumberFormatException ex) {
          return "";
        }
      }
      if (!(current instanceof Map<?, ?> map)) {
        return "";
      }
      current = map.get(key);
    }
    return asString(current);
  }

  private static String normalize(String value) {
    return value == null ? "" : value.strip().toLowerCase();
  }

  private static boolean matchesQuery(JobListing job, String query) {
    if (query.isBlank()) {
      return true;
    }

    return job.title().toLowerCase().contains(query)
        || job.company().toLowerCase().contains(query)
        || job.location().toLowerCase().contains(query)
        || job.type().toLowerCase().contains(query)
        || job.tags().stream().anyMatch(tag -> tag.toLowerCase().contains(query));
  }

  private static boolean matchesLocation(JobListing job, String location) {
    return location.isBlank() || job.location().toLowerCase().contains(location);
  }

  private static boolean matchesRemote(JobListing job, Boolean remote) {
    return remote == null || job.remote() == remote;
  }

  private static boolean isUnitedStatesLocation(String rawLocation) {
    String location = normalize(rawLocation);
    if (location.isBlank()) {
      return false;
    }

    String compact = " " + location.replaceAll("[^a-z0-9]+", " ").trim() + " ";
    if (compact.contains(" united states ")
        || compact.contains(" united states of america ")
        || compact.contains(" usa ")
        || compact.contains(" us only ")
        || compact.contains(" us based ")
        || compact.contains(" u s a ")) {
      return true;
    }

    // Common provider-style labels like "Remote (US)" / "US, Canada"
    if (compact.matches(".*\\bus\\b.*")) {
      return true;
    }

    return compact.contains(" new york ")
        || compact.contains(" san francisco ")
        || compact.contains(" los angeles ")
        || compact.contains(" chicago ")
        || compact.contains(" austin ")
        || compact.contains(" seattle ")
        || compact.contains(" boston ")
        || compact.contains(" united states remote ");
  }

  private static String asString(Object value) {
    return value == null ? "" : value.toString().trim();
  }

  private static String sanitizeBaseUrl(String url) {
    String normalized = defaultIfBlank(url, "");
    while (normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }

  private static String defaultIfBlank(String value, String fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    return value.strip();
  }

  private static boolean asBoolean(Object value) {
    if (value instanceof Boolean b) {
      return b;
    }
    if (value instanceof String str) {
      return Boolean.parseBoolean(str);
    }
    return false;
  }

  private static List<String> splitCsv(String value) {
    if (value == null || value.isBlank()) {
      return List.of();
    }
    return List.of(value.split(",")).stream()
        .map(String::strip)
        .filter(s -> !s.isBlank())
        .distinct()
        .toList();
  }

  private static List<String> toStringList(Object value) {
    if (!(value instanceof List<?> list)) {
      return List.of();
    }

    return list.stream()
        .map(JobProviderService::asString)
        .filter(s -> !s.isBlank())
        .collect(Collectors.toList());
  }

  private static String formatLocation(Object value) {
    if (value instanceof List<?> list) {
      return list.stream()
          .map(JobProviderService::asString)
          .filter(s -> !s.isBlank())
          .collect(Collectors.joining(", "));
    }
    return asString(value);
  }

  private static String hashId(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash).substring(0, 16);
    } catch (NoSuchAlgorithmException e) {
      return Integer.toHexString(value.hashCode());
    }
  }
}
