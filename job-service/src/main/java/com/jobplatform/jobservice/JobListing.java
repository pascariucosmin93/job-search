package com.jobplatform.jobservice;

import java.util.List;

public record JobListing(
    String id,
    String title,
    String company,
    String location,
    String type,
    boolean remote,
    List<String> tags,
    String url,
    String source,
    String publishedAt
) {}
