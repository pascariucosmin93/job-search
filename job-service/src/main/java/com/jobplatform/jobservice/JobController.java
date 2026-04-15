package com.jobplatform.jobservice;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

  private final JobProviderService jobProviderService;

  public JobController(JobProviderService jobProviderService) {
    this.jobProviderService = jobProviderService;
  }

  @GetMapping
  public List<JobListing> listJobs(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String location,
      @RequestParam(required = false) Boolean remote
  ) {
    return jobProviderService.listJobs(q, location, remote);
  }
}
