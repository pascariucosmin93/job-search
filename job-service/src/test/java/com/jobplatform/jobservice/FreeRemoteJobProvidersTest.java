package com.jobplatform.jobservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class FreeRemoteJobProvidersTest {

  private final ObjectMapper mapper = new ObjectMapper();
  private final FreeRemoteJobProviders providers = new FreeRemoteJobProviders(
      RestClient.builder(), mapper, 1000,
      true, "https://jobicy.com", 100,
      true, "https://himalayas.app", 3,
      true, "https://remoteok.com",
      true, "https://www.workingnomads.com");

  private JsonNode json(String s) throws Exception {
    return mapper.readTree(s);
  }

  @Test
  void mapsJobicy() throws Exception {
    var job = providers.toJobicy(json("""
        {"id":159539,"url":"https://jobicy.com/jobs/159539-x","jobTitle":"QA Rater","companyName":"Welo",
         "jobIndustry":["QA & Testing"],"jobType":["Contract"],"jobGeo":"Australia","jobLevel":"Any",
         "pubDate":"2026-11-07T11:13:52+00:00"}""")).orElseThrow();
    assertEquals("QA Rater", job.title());
    assertEquals("Welo", job.company());
    assertEquals("Australia", job.location());
    assertEquals("Contract", job.type());
    assertEquals("Jobicy", job.source());
    assertTrue(job.remote());
    assertEquals(java.util.List.of("QA & Testing"), job.tags());
  }

  @Test
  void mapsHimalayasWithEpochDateAndRestrictions() throws Exception {
    var job = providers.toHimalayas(json("""
        {"title":"Grants Manager","companyName":"Precision","employmentType":"Full Time",
         "seniority":["Senior"],"locationRestrictions":["Germany","Romania"],"categories":["A","B"],
         "pubDate":1794063327,"applicationLink":"https://himalayas.app/x","guid":"https://himalayas.app/x"}""")).orElseThrow();
    assertEquals("Germany, Romania", job.location());
    assertEquals("Full Time", job.type());
    assertEquals(java.util.List.of("Senior", "A", "B"), job.tags());
    assertTrue(job.publishedAt().startsWith("2026-"));
  }

  @Test
  void mapsRemoteOkAndSkipsLegalNotice() throws Exception {
    assertTrue(providers.toRemoteOk(json("{\"last_updated\":1,\"legal\":\"notice\"}")).isEmpty());
    var job = providers.toRemoteOk(json("""
        {"id":"1","position":"Analyst","company":"MEYA","location":"","tags":["finance","remote"],
         "url":"https://remoteOK.com/remote-jobs/x","date":"2026-11-06T13:47:40+00:00"}""")).orElseThrow();
    assertEquals("Remote", job.location());
    assertEquals("RemoteOK", job.source());
  }

  @Test
  void mapsWorkingNomadsCsvTags() throws Exception {
    var job = providers.toWorkingNomads(json("""
        {"url":"https://www.workingnomads.com/job/go/1/","title":"Reviewer","company_name":"iMerit",
         "category_name":"Administration","tags":"data, annotation,english","location":"Austria, Belgium",
         "pub_date":"2026-11-05T03:10:45-05:00"}""")).orElseThrow();
    assertEquals(java.util.List.of("Administration", "data", "annotation", "english"), job.tags());
    assertEquals("Austria, Belgium", job.location());
  }

  @Test
  void rejectsIncompleteEntries() throws Exception {
    assertTrue(providers.toJobicy(json("{\"jobTitle\":\"x\"}")).isEmpty());
    assertTrue(providers.toHimalayas(json("{\"title\":\"x\",\"companyName\":\"y\"}")).isEmpty());
    assertFalse(providers.toWorkingNomads(json("{\"title\":\"x\",\"company_name\":\"y\",\"url\":\"u\"}")).isEmpty());
  }

  @Test
  void disabledProviderReturnsNothing() {
    var off = new FreeRemoteJobProviders(RestClient.builder(), mapper, 1000,
        false, "x", 1, false, "x", 1, false, "x", false, "x");
    assertTrue(off.fetchJobicy().isEmpty());
    assertTrue(off.fetchHimalayas().isEmpty());
    assertTrue(off.fetchRemoteOk().isEmpty());
    assertTrue(off.fetchWorkingNomads().isEmpty());
  }
}
