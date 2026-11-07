package com.jobplatform.jobservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Careerjet partner API v4 (https://www.careerjet.com/partners/api). Requires an API key and
 * the calling server's IP to be whitelisted in the partner account. The API returns 20 jobs per
 * page, so we query several keywords and pages. Disabled automatically when no key is set.
 */
@Component
public class CareerjetProvider {

  private static final Logger LOG = LoggerFactory.getLogger(CareerjetProvider.class);
  static final String SOURCE_CAREERJET = "Careerjet";

  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  private final boolean enabled;
  private final String baseUrl;
  private final String apiKey;
  private final String localeCode;
  private final String location;
  private final List<String> keywords;
  private final int pages;
  private final String userIp;
  private final String userAgent;
  private final String referer;

  public CareerjetProvider(
      RestClient.Builder restClientBuilder,
      ObjectMapper objectMapper,
      @Value("${jobs.api.timeout-ms:5000}") int timeoutMs,
      @Value("${jobs.api.careerjet.enabled:true}") boolean enabled,
      @Value("${jobs.api.careerjet.base-url:https://search.api.careerjet.net}") String baseUrl,
      @Value("${jobs.api.careerjet.api-key:}") String apiKey,
      @Value("${jobs.api.careerjet.locale-code:ro_RO}") String localeCode,
      @Value("${jobs.api.careerjet.location:Romania}") String location,
      @Value("${jobs.api.careerjet.keywords:software developer,devops,java,python,frontend,data engineer}") String keywords,
      @Value("${jobs.api.careerjet.pages:3}") int pages,
      @Value("${jobs.api.careerjet.user-ip:}") String userIp,
      @Value("${jobs.api.careerjet.user-agent:job-platform-job-service/1.0}") String userAgent,
      @Value("${jobs.api.careerjet.referer:}") String referer
  ) {
    var requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
    requestFactory.setReadTimeout(Duration.ofMillis(Math.max(timeoutMs, 10_000)));
    this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    this.objectMapper = objectMapper;

    this.enabled = enabled;
    this.baseUrl = trimSlash(baseUrl);
    this.apiKey = apiKey == null ? "" : apiKey.strip();
    this.localeCode = localeCode == null || localeCode.isBlank() ? "ro_RO" : localeCode.strip();
    this.location = location == null ? "" : location.strip();
    this.keywords = splitCsv(keywords);
    this.pages = Math.min(10, Math.max(1, pages));
    this.userIp = userIp == null ? "" : userIp.strip();
    this.userAgent = userAgent == null || userAgent.isBlank() ? "job-platform-job-service/1.0" : userAgent.strip();
    this.referer = referer == null ? "" : referer.strip();
  }

  public List<JobListing> fetchJobs() {
    if (!enabled) {
      return List.of();
    }
    if (apiKey.isBlank() || userIp.isBlank() || referer.isBlank()) {
      LOG.info("Careerjet provider skipped: missing api key, user-ip or referer");
      return List.of();
    }

    List<JobListing> result = new ArrayList<>();
    for (String keyword : keywords) {
      for (int page = 1; page <= pages; page++) {
        try {
          JsonNode root = query(keyword, page);
          if ("ERROR".equals(root.path("type").asText())) {
            // e.g. unauthorized IP or invalid parameters: abort the whole provider, not just this page.
            LOG.warn("Careerjet returned an error: {}", root.path("error").asText());
            return result;
          }
          JsonNode jobs = root.path("jobs");
          if (!jobs.isArray() || jobs.isEmpty()) {
            break;
          }
          for (JsonNode job : jobs) {
            toJob(job, keyword).ifPresent(result::add);
          }
          if (page >= root.path("pages").asInt(page)) {
            break;
          }
        } catch (Exception ex) {
          LOG.warn("Careerjet provider unavailable (keywords='{}', page {}): {}", keyword, page, ex.getMessage());
          break;
        }
      }
    }
    return result;
  }

  private JsonNode query(String keyword, int page) throws Exception {
    var uri = UriComponentsBuilder.fromUriString(baseUrl + "/v4/query")
        .queryParam("locale_code", localeCode)
        .queryParam("keywords", "{keywords}")
        .queryParam("location", "{location}")
        .queryParam("user_ip", userIp)
        .queryParam("user_agent", "{userAgent}")
        .queryParam("page", page)
        .build(java.util.Map.of("keywords", keyword, "location", location, "userAgent", userAgent));

    var request = restClient.get()
        .uri(uri)
        .accept(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.REFERER, referer)
        .headers(h -> h.setBasicAuth(apiKey, ""));

    // Careerjet returns HTTP 403 with a JSON body on errors; read the body ourselves.
    String body = request.exchange((req, res) -> new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8));
    if (body == null || body.isBlank()) {
      throw new IllegalStateException("empty response");
    }
    return objectMapper.readTree(body);
  }

  Optional<JobListing> toJob(JsonNode j, String keyword) {
    String title = text(j, "title");
    String url = text(j, "url");
    if (title.isBlank() || url.isBlank()) {
      return Optional.empty();
    }
    String company = text(j, "company");
    String jobLocation = text(j, "locations");
    String description = text(j, "description");
    String haystack = (title + " " + jobLocation + " " + description).toLowerCase();
    boolean remote = haystack.contains("remote") || haystack.contains("la distan") || haystack.contains("work from home");

    return Optional.of(new JobListing(
        hashId(SOURCE_CAREERJET + "|" + url),
        title,
        company.isBlank() ? "Company not specified" : company,
        jobLocation.isBlank() ? "Romania" : jobLocation,
        "Not specified",
        remote,
        List.of(keyword),
        url,
        SOURCE_CAREERJET,
        parseDate(text(j, "date"))
    ));
  }

  private static String parseDate(String raw) {
    if (raw.isBlank()) {
      return "";
    }
    try {
      return ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toString();
    } catch (Exception ex) {
      return raw;
    }
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node.path(field);
    return value.isMissingNode() || value.isNull() ? "" : value.asText("").strip();
  }

  private static List<String> splitCsv(String value) {
    if (value == null || value.isBlank()) {
      return List.of();
    }
    return List.of(value.split(",")).stream().map(String::strip).filter(s -> !s.isBlank()).distinct().toList();
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
