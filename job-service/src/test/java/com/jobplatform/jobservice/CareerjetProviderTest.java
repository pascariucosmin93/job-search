package com.jobplatform.jobservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class CareerjetProviderTest {

  private final ObjectMapper mapper = new ObjectMapper();

  private CareerjetProvider provider(boolean enabled, String key, String ip) {
    return new CareerjetProvider(RestClient.builder(), mapper, 1000, enabled,
        "https://search.api.careerjet.net", key, "ro_RO", "Romania", "java", 1, ip, "ua", "http://jobs.kube.cosmin.home/");
  }

  private JsonNode json(String s) throws Exception {
    return mapper.readTree(s);
  }

  @Test
  void mapsRealResponseShape() throws Exception {
    var job = provider(true, "k", "1.2.3.4").toJob(json("""
        {"company":"PureQuad","date":"Wed, 04 Nov 2026 23:39:18 GMT","description":"rol <b>Software</b> Engineer",
         "locations":"București","salary":"","site":"","title":"AI Assisted Software and Tool Engineer",
         "url":"https://jobviewtrack.com/v2/abc"}"""), "java").orElseThrow();
    assertEquals("AI Assisted Software and Tool Engineer", job.title());
    assertEquals("PureQuad", job.company());
    assertEquals("București", job.location());
    assertEquals("Careerjet", job.source());
    assertEquals("2026-11-04T23:39:18Z", job.publishedAt());
    assertEquals(java.util.List.of("java"), job.tags());
    assertFalse(job.remote());
  }

  @Test
  void detectsRemoteAndFillsMissingCompany() throws Exception {
    var job = provider(true, "k", "1.2.3.4").toJob(json("""
        {"title":"Java Developer (Remote)","url":"https://jobviewtrack.com/v2/x","locations":"","company":""}"""), "java").orElseThrow();
    assertTrue(job.remote());
    assertEquals("Company not specified", job.company());
    assertEquals("Romania", job.location());
  }

  @Test
  void rejectsEntriesWithoutTitleOrUrl() throws Exception {
    assertTrue(provider(true, "k", "1.2.3.4").toJob(json("{\"title\":\"x\"}"), "java").isEmpty());
    assertTrue(provider(true, "k", "1.2.3.4").toJob(json("{\"url\":\"u\"}"), "java").isEmpty());
  }

  @Test
  void skipsWithoutKeyIpOrWhenDisabled() {
    assertTrue(provider(true, "", "1.2.3.4").fetchJobs().isEmpty());
    assertTrue(provider(true, "k", "").fetchJobs().isEmpty());
    assertTrue(provider(false, "k", "1.2.3.4").fetchJobs().isEmpty());
    var noReferer = new CareerjetProvider(RestClient.builder(), mapper, 1000, true,
        "https://search.api.careerjet.net", "k", "ro_RO", "Romania", "java", 1, "1.2.3.4", "ua", "");
    assertTrue(noReferer.fetchJobs().isEmpty());
  }
}
