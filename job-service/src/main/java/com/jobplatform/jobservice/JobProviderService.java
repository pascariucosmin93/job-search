package com.jobplatform.jobservice;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class JobProviderService {

  private static final Logger LOG = LoggerFactory.getLogger(JobProviderService.class);

  private static final List<JobListing> FALLBACK_JOBS = List.of(
      new JobListing("fallback-1", "Senior Platform Engineer", "CloudScale Labs", "Remote", "Full-time", true, List.of("Kubernetes", "Terraform", "GitOps"), "https://example.com/jobs/senior-platform-engineer", "Fallback", "2026-04-01"),
      new JobListing("fallback-2", "Java Backend Engineer", "Nimbus Systems", "Berlin", "Hybrid", false, List.of("Java", "Spring Boot", "PostgreSQL"), "https://example.com/jobs/java-backend-engineer", "Fallback", "2026-03-30"),
      new JobListing("fallback-3", "DevOps Engineer", "OrbitStack", "Remote", "Contract", true, List.of("CI/CD", "Docker", "AWS"), "https://example.com/jobs/devops-engineer", "Fallback", "2026-03-28"),
      new JobListing("fallback-4", "Frontend Engineer", "Pulse Apps", "Bucharest", "Full-time", false, List.of("React", "Next.js", "TypeScript"), "https://example.com/jobs/frontend-engineer", "Fallback", "2026-03-25")
  );

  private final RestClient restClient;

  public JobProviderService(
      RestClient.Builder restClientBuilder,
      @Value("${jobs.api.base-url:https://www.arbeitnow.com}") String baseUrl,
      @Value("${jobs.api.timeout-ms:5000}") int timeoutMs
  ) {
    var requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
    requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));

    this.restClient = restClientBuilder
        .baseUrl(baseUrl)
        .requestFactory(requestFactory)
        .build();
  }

  public List<JobListing> listJobs(String query, String location, Boolean remote) {
    List<JobListing> jobs = fetchLiveJobs();

    String normalizedQuery = normalize(query);
    String normalizedLocation = normalize(location);

    return jobs.stream()
        .filter(job -> matchesQuery(job, normalizedQuery))
        .filter(job -> matchesLocation(job, normalizedLocation))
        .filter(job -> matchesRemote(job, remote))
        .toList();
  }

  private List<JobListing> fetchLiveJobs() {
    try {
      Map<String, Object> payload = restClient.get()
          .uri(uriBuilder -> uriBuilder.path("/api/job-board-api").build())
          .retrieve()
          .body(new ParameterizedTypeReference<>() {});

      if (payload == null || !(payload.get("data") instanceof List<?> data)) {
        return FALLBACK_JOBS;
      }

      List<JobListing> mapped = data.stream()
          .filter(Map.class::isInstance)
          .map(Map.class::cast)
          .map(this::toJob)
          .filter(Objects::nonNull)
          .toList();

      return mapped.isEmpty() ? FALLBACK_JOBS : mapped;
    } catch (Exception ex) {
      LOG.warn("Failed to fetch jobs from external API, serving fallback list: {}", ex.getMessage());
      return FALLBACK_JOBS;
    }
  }

  private JobListing toJob(Map<?, ?> source) {
    String title = asString(source.get("title"));
    String company = asString(source.get("company_name"));
    String jobUrl = asString(source.get("url"));

    if (title.isBlank() || company.isBlank()) {
      return null;
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

    return new JobListing(
        hashId(idSeed),
        title,
        company,
        location.isBlank() ? "Location not specified" : location,
        type,
        remote,
        tags,
        jobUrl,
        "Arbeitnow",
        asString(source.get("created_at"))
    );
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

  private static String asString(Object value) {
    return value == null ? "" : value.toString().trim();
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
