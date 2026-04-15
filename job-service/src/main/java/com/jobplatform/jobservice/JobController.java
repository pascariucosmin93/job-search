package com.jobplatform.jobservice;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobController {
  @GetMapping
  public List<Map<String, String>> listJobs() {
    return List.of(
        Map.of("id", "1", "title", "Senior Platform Engineer", "location", "Remote"),
        Map.of("id", "2", "title", "Staff Backend Developer", "location", "Berlin"));
  }
}

