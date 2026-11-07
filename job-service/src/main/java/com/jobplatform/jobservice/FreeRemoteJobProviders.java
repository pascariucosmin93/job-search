package com.jobplatform.jobservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Free remote-job providers that need no API key: Jobicy, Himalayas, RemoteOK, Working Nomads.
 * Every fetch method is fail-safe: on any error it logs a warning and returns an empty list.
 */
@Component
public class FreeRemoteJobProviders {

  private static final Logger LOG = LoggerFactory.getLogger(FreeRemoteJobProviders.class);
  private static final String USER_AGENT = "job-platform-job-service/1.0";
  private static final int HIMALAYAS_PAGE_SIZE = 20;

  static final String SOURCE_JOBICY = "Jobicy";
  static final String SOURCE_HIMALAYAS = "Himalayas";
  static final String SOURCE_REMOTEOK = "RemoteOK";
  static final String SOURCE_WORKING_NOMADS = "Working Nomads";

  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  private final boolean jobicyEnabled;
  private final String jobicyBaseUrl;
  private final int jobicyCount;

  private final boolean himalayasEnabled;
  private final String himalayasBaseUrl;
  private final int himalayasPages;

  private final boolean remoteOkEnabled;
  private final String remoteOkBaseUrl;

  private final boolean workingNomadsEnabled;
  private final String workingNomadsBaseUrl;

  public FreeRemoteJobProviders(
      RestClient.Builder restClientBuilder,
      ObjectMapper objectMapper,
      @Value("${jobs.api.timeout-ms:5000}") int timeoutMs,
      @Value("${jobs.api.jobicy.enabled:true}") boolean jobicyEnabled,
      @Value("${jobs.api.jobicy.base-url:https://jobicy.com}") String jobicyBaseUrl,
      @Value("${jobs.api.jobicy.count:100}") int jobicyCount,
      @Value("${jobs.api.himalayas.enabled:true}") boolean himalayasEnabled,
      @Value("${jobs.api.himalayas.base-url:https://himalayas.app}") String himalayasBaseUrl,
      @Value("${jobs.api.himalayas.pages:3}") int himalayasPages,
      @Value("${jobs.api.remoteok.enabled:true}") boolean remoteOkEnabled,
      @Value("${jobs.api.remoteok.base-url:https://remoteok.com}") String remoteOkBaseUrl,
      @Value("${jobs.api.workingnomads.enabled:true}") boolean workingNomadsEnabled,
      @Value("${jobs.api.workingnomads.base-url:https://www.workingnomads.com}") String workingNomadsBaseUrl
  ) {
    var requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
    requestFactory.setReadTimeout(Duration.ofMillis(Math.max(timeoutMs, 10_000)));
    this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    this.objectMapper = objectMapper;

    this.jobicyEnabled = jobicyEnabled;
    this.jobicyBaseUrl = trimSlash(jobicyBaseUrl);
    this.jobicyCount = Math.min(100, Math.max(1, jobicyCount));

    this.himalayasEnabled = himalayasEnabled;
    this.himalayasBaseUrl = trimSlash(himalayasBaseUrl);
    this.himalayasPages = Math.min(10, Math.max(1, himalayasPages));

    this.remoteOkEnabled = remoteOkEnabled;
    this.remoteOkBaseUrl = trimSlash(remoteOkBaseUrl);

    this.workingNomadsEnabled = workingNomadsEnabled;
    this.workingNomadsBaseUrl = trimSlash(workingNomadsBaseUrl);
  }

  public List<JobListing> fetchJobicy() {
    if (!jobicyEnabled) {
      return List.of();
    }
    try {
      JsonNode jobs = fetchJson(jobicyBaseUrl + "/api/v2/remote-jobs?count=" + jobicyCount).path("jobs");
      List<JobListing> result = new ArrayList<>();
      for (JsonNode j : jobs) {
        toJobicy(j).ifPresent(result::add);
      }
      return result;
    } catch (Exception ex) {
      LOG.warn("Jobicy provider unavailable: {}", ex.getMessage());
      return List.of();
    }
  }

  public List<JobListing> fetchHimalayas() {
    if (!himalayasEnabled) {
      return List.of();
    }
    List<JobListing> result = new ArrayList<>();
    for (int page = 0; page < himalayasPages; page++) {
      try {
        String url = himalayasBaseUrl + "/jobs/api?limit=" + HIMALAYAS_PAGE_SIZE
            + "&offset=" + (page * HIMALAYAS_PAGE_SIZE);
        JsonNode jobs = fetchJson(url).path("jobs");
        if (!jobs.isArray() || jobs.isEmpty()) {
          break;
        }
        for (JsonNode j : jobs) {
          toHimalayas(j).ifPresent(result::add);
        }
      } catch (Exception ex) {
        LOG.warn("Himalayas provider unavailable (page {}): {}", page, ex.getMessage());
        break;
      }
    }
    return result;
  }

  public List<JobListing> fetchRemoteOk() {
    if (!remoteOkEnabled) {
      return List.of();
    }
    try {
      JsonNode root = fetchJson(remoteOkBaseUrl + "/api");
      List<JobListing> result = new ArrayList<>();
      // The first element of the array is a legal notice, not a job; toRemoteOk skips it.
      for (JsonNode j : root) {
        toRemoteOk(j).ifPresent(result::add);
      }
      return result;
    } catch (Exception ex) {
      LOG.warn("RemoteOK provider unavailable: {}", ex.getMessage());
      return List.of();
    }
  }

  public List<JobListing> fetchWorkingNomads() {
    if (!workingNomadsEnabled) {
      return List.of();
    }
    try {
      JsonNode root = fetchJson(workingNomadsBaseUrl + "/api/exposed_jobs/");
      List<JobListing> result = new ArrayList<>();
      for (JsonNode j : root) {
        toWorkingNomads(j).ifPresent(result::add);
      }
      return result;
    } catch (Exception ex) {
      LOG.warn("Working Nomads provider unavailable: {}", ex.getMessage());
      return List.of();
    }
  }

  // --- mapping ---------------------------------------------------------------------------

  Optional<JobListing> toJobicy(JsonNode j) {
    String title = text(j, "jobTitle");
    String company = text(j, "companyName");
    String url = text(j, "url");
    if (title.isBlank() || company.isBlank() || url.isBlank()) {
      return Optional.empty();
    }
    String geo = text(j, "jobGeo");
    List<String> types = strings(j.path("jobType"));
    List<String> tags = new ArrayList<>(strings(j.path("jobIndustry")));
    String level = text(j, "jobLevel");
    if (!level.isBlank() && !"Any".equalsIgnoreCase(level)) {
      tags.add(level);
    }
    return Optional.of(new JobListing(
        hashId(SOURCE_JOBICY + "|" + text(j, "id") + "|" + url),
        title,
        company,
        geo.isBlank() ? "Remote" : geo,
        types.isEmpty() ? "Not specified" : String.join(", ", types),
        true,
        tags,
        url,
        SOURCE_JOBICY,
        text(j, "pubDate")
    ));
  }

  Optional<JobListing> toHimalayas(JsonNode j) {
    String title = text(j, "title");
    String company = text(j, "companyName");
    String url = text(j, "applicationLink");
    if (url.isBlank()) {
      url = text(j, "guid");
    }
    if (title.isBlank() || company.isBlank() || url.isBlank()) {
      return Optional.empty();
    }
    List<String> restrictions = strings(j.path("locationRestrictions"));
    String employment = text(j, "employmentType");
    List<String> tags = new ArrayList<>(strings(j.path("seniority")));
    List<String> categories = strings(j.path("categories"));
    tags.addAll(categories.subList(0, Math.min(5, categories.size())));
    String published = j.path("pubDate").isNumber()
        ? Instant.ofEpochSecond(j.path("pubDate").asLong()).toString()
        : text(j, "pubDate");
    return Optional.of(new JobListing(
        hashId(SOURCE_HIMALAYAS + "|" + text(j, "guid") + "|" + url),
        title,
        company,
        restrictions.isEmpty() ? "Remote" : String.join(", ", restrictions),
        employment.isBlank() ? "Not specified" : employment,
        true,
        tags,
        url,
        SOURCE_HIMALAYAS,
        published
    ));
  }

  Optional<JobListing> toRemoteOk(JsonNode j) {
    String title = text(j, "position");
    String company = text(j, "company");
    String url = text(j, "url");
    if (title.isBlank() || company.isBlank() || url.isBlank()) {
      return Optional.empty();
    }
    String location = text(j, "location");
    return Optional.of(new JobListing(
        hashId(SOURCE_REMOTEOK + "|" + text(j, "id") + "|" + url),
        title,
        company,
        location.isBlank() ? "Remote" : location,
        "Not specified",
        true,
        strings(j.path("tags")),
        url,
        SOURCE_REMOTEOK,
        text(j, "date")
    ));
  }

  Optional<JobListing> toWorkingNomads(JsonNode j) {
    String title = text(j, "title");
    String company = text(j, "company_name");
    String url = text(j, "url");
    if (title.isBlank() || company.isBlank() || url.isBlank()) {
      return Optional.empty();
    }
    List<String> tags = new ArrayList<>();
    String category = text(j, "category_name");
    if (!category.isBlank()) {
      tags.add(category);
    }
    for (String tag : text(j, "tags").split(",")) {
      if (!tag.isBlank()) {
        tags.add(tag.strip());
      }
    }
    String location = text(j, "location");
    return Optional.of(new JobListing(
        hashId(SOURCE_WORKING_NOMADS + "|" + url),
        title,
        company,
        location.isBlank() ? "Remote" : location,
        "Not specified",
        true,
        tags,
        url,
        SOURCE_WORKING_NOMADS,
        text(j, "pub_date")
    ));
  }

  // --- helpers ---------------------------------------------------------------------------

  /** Fetches as text and parses ourselves, so a wrong Content-Type never breaks a provider. */
  private JsonNode fetchJson(String url) throws Exception {
    String body = restClient.get()
        .uri(url)
        .accept(MediaType.APPLICATION_JSON)
        .header("User-Agent", USER_AGENT)
        .retrieve()
        .body(String.class);
    if (body == null || body.isBlank()) {
      throw new IllegalStateException("empty response from " + url);
    }
    return objectMapper.readTree(body);
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node.path(field);
    return value.isMissingNode() || value.isNull() ? "" : value.asText("").strip();
  }

  private static List<String> strings(JsonNode array) {
    if (!array.isArray()) {
      return List.of();
    }
    List<String> out = new ArrayList<>();
    for (JsonNode item : array) {
      String s = item.asText("").strip();
      if (!s.isBlank()) {
        out.add(s);
      }
    }
    return out;
  }

  private static String trimSlash(String url) {
    String s = url == null ? "" : url.strip();
    while (s.endsWith("/")) {
      s = s.substring(0, s.length() - 1);
    }
    return s;
  }

  private static String hashId(String value) {
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash).substring(0, 16);
    } catch (NoSuchAlgorithmException e) {
      return Integer.toHexString(value.hashCode());
    }
  }
}
