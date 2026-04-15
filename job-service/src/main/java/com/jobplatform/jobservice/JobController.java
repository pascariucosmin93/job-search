package com.jobplatform.jobservice;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

  private static final List<Map<String, Object>> JOBS = List.of(
      Map.of("id", "1", "title", "Senior Platform Engineer",     "location", "Remote",    "type", "Full-time", "tags", List.of("Kubernetes", "GitOps", "Terraform")),
      Map.of("id", "2", "title", "Staff Backend Developer",      "location", "Berlin",    "type", "Hybrid",    "tags", List.of("Java", "Spring Boot", "PostgreSQL")),
      Map.of("id", "3", "title", "DevOps Engineer",              "location", "Remote",    "type", "Full-time", "tags", List.of("CI/CD", "Argo CD", "Docker")),
      Map.of("id", "4", "title", "Cloud Infrastructure Lead",    "location", "Bucharest", "type", "On-site",   "tags", List.of("AWS", "Kubernetes", "Terraform")),
      Map.of("id", "5", "title", "Site Reliability Engineer",    "location", "Amsterdam", "type", "Hybrid",    "tags", List.of("Prometheus", "Grafana", "Kubernetes")),
      Map.of("id", "6", "title", "Full Stack Developer",         "location", "Remote",    "type", "Full-time", "tags", List.of("React", "Node.js", "PostgreSQL")),
      Map.of("id", "7", "title", "Security Engineer",            "location", "Remote",    "type", "Full-time", "tags", List.of("DevSecOps", "Vault", "Kubernetes"))
  );

  @GetMapping
  public List<Map<String, Object>> listJobs(@RequestParam(required = false) String q) {
    if (q == null || q.isBlank()) return JOBS;

    String query = q.toLowerCase();
    return JOBS.stream()
        .filter(job ->
            job.get("title").toString().toLowerCase().contains(query) ||
            job.get("location").toString().toLowerCase().contains(query) ||
            job.get("type").toString().toLowerCase().contains(query) ||
            ((List<?>) job.get("tags")).stream().anyMatch(t -> t.toString().toLowerCase().contains(query))
        )
        .toList();
  }
}
