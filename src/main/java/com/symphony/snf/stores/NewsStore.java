package com.symphony.snf.stores;

import com.symphony.snf.model.ConsolidatedNewsItem;
import com.symphony.snf.model.Finref;
import com.symphony.snf.model.atom.AtomAuthor;
import com.symphony.snf.model.atom.AtomContent;
import com.symphony.snf.model.atom.AtomTitle;
import com.symphony.snf.model.json.JsonEntry;
import com.symphony.snf.model.json.JsonFeed;
import com.symphony.snf.model.stats.NewsStats;
import com.symphony.snf.model.atom.AtomEntry;
import com.symphony.snf.model.atom.AtomFeed;
import com.symphony.snf.model.atom.AtomLink;
import com.symphony.snf.services.AuthenticationService;
import com.symphony.snf.services.NewsService;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class NewsStore {

  public static final int PAGE_SIZE = 30;

  public static final int STARTING_NEWS_COUNT = 1000;

  public static final long CACHE_DURATION_IN_DAYS = 2;

  Map<String, String> newsPageXml = new HashMap<>();

  Map<String, String> newsPageJson = new HashMap<>();

  Map<String, Instant> newsPageCreationDate =  new HashMap<>();

  NewsService newsService;

  AuthenticationService authenticationService;

  AtomFeed firstPageXml;

  JsonFeed firstPageJson;

  XmlMapper xmlMapper;

  ObjectMapper jsonMapper;

  Instant latestNewsTime = Instant.EPOCH;

  String lastNewsId;

  String lastPageId;

  boolean isPolling = false;

  @Getter
  boolean isInitialized = false;

  NewsStore(NewsService newsService, AuthenticationService authenticationService) {
    this.newsService = newsService;
    this.authenticationService = authenticationService;
    xmlMapper = new XmlMapper();
    xmlMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    jsonMapper = new ObjectMapper();
    jsonMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
  }

  public boolean isCurrent(String id) {
    if (id == null || firstPageXml == null) {
      return true;
    }
    return firstPageXml.getId().equals(id);
  }

  public String getNewsPageXml(String id) {
    if (StringUtils.isBlank(id)) {
      return lastPageId == null ? "" : newsPageXml.get(lastPageId);
    }

    return newsPageXml.get(id);
  }

  public String getNewsPageJson(String id) {
    if (StringUtils.isBlank(id)) {
      return lastPageId == null ? "" : newsPageJson.get(lastPageId);
    }

    return newsPageJson.get(id);
  }

  public NewsStats getStats() {
    NewsStats.NewsStatsBuilder builder = NewsStats.builder();
    int pageCount = newsPageXml.size();
    int newsCount = firstPageXml == null ? 0 : (newsPageXml.size() - 1) * PAGE_SIZE + firstPageXml.getEntries().size();
    builder.lastUpdate(latestNewsTime.toString()).pageCount(pageCount).newsCount(newsCount);
    return builder.build();
  }

  @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
  private void pollNews() {
    if (!authenticationService.areCredentialsSet()) {
      System.out.println("Postponing polling until credentials are set");
      return;
    }

    if (isPolling) {
      return;
    }

    isPolling = true;

    try {
      LinkedList<ConsolidatedNewsItem> news = new LinkedList<>();

      String now = Instant.now().toString();

      if (lastNewsId == null) {
        int newsCount = 0;
        String oldestNewsId = null;
        int count = 0;
        while (newsCount < STARTING_NEWS_COUNT) {
          List<ConsolidatedNewsItem> newsFrom = newsService.getNewsFrom(oldestNewsId);
          if (newsFrom.isEmpty()) {
            break;
          }
          count++;
          System.out.println("[" + now + "] Fetched " + count + " times");
          oldestNewsId = newsFrom.get(0).getMsgId();
          news.addAll(0, newsFrom);
          newsCount = news.size();
        }
      } else {
        String lastId = lastNewsId;

        int count = 0;
        while (true) {
          List<ConsolidatedNewsItem> newsSince = newsService.getNewsSince(lastId);
          if (newsSince.isEmpty()) {
            break;
          }
          count++;
          System.out.println("[" + now + "] Fetched " + count + " times");
          news.addAll(newsSince);
          lastId = news.getLast().getMsgId();
        }
      }

      if (news.size() == 0) {
        isPolling = false;
        return;
      }

      lastNewsId = news.getLast().getMsgId();

      if (firstPageXml == null) {
        initNewPageAndArchiveCurrent();
      }

      boolean hasChanged = false;
      for (ConsolidatedNewsItem entry : news) {
        Map<String, Finref> finrefs = new HashMap<>();
        for (Finref finref : entry.getStocks()) {
          finrefs.put(finref.getLocalCode(), finref);
        }

        if (firstPageJson.getEntries().size() >= PAGE_SIZE) {
          initNewPageAndArchiveCurrent();
        }

        firstPageXml.getEntries().addFirst(convertToXmlEntry(entry));

        firstPageJson.getEntries().addFirst(convertToJsonEntry(entry));
        firstPageJson.getFintagDict().putAll(finrefs);
        latestNewsTime = Instant.parse(entry.getPostedAt());
        hasChanged = true;
      }

      if (hasChanged) {
        try {
          String firstPageXmlString = getPageAsString(firstPageXml);
          this.newsPageXml.put(firstPageXml.getId(), firstPageXmlString);
          String firstPageJsonString = getPageAsString(firstPageJson);
          newsPageJson.put(firstPageJson.getId(), firstPageJsonString);
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }
      }
    } finally {
      isPolling = false;
    }
  }

  @Scheduled(fixedDelay = 4, timeUnit = TimeUnit.HOURS)
  private void clearOutdatedNews() {
    Instant now = Instant.now();
    List<Entry<String, Instant>> entries = newsPageCreationDate.entrySet().stream().collect(Collectors.toCollection(ArrayList::new));
    entries.sort(Comparator.comparing(Entry::getValue));
    for (Entry<String, Instant> entry: entries) {
      if (newsPageJson.size() * PAGE_SIZE < STARTING_NEWS_COUNT) {
        break;
      }
      if (Duration.between(entry.getValue(), now).toDays() > CACHE_DURATION_IN_DAYS) {
        String key = entry.getKey();
        newsPageCreationDate.remove(key);
        newsPageJson.remove(key);
        newsPageXml.remove(key);
      }
    }
  }

  private AtomEntry convertToXmlEntry(ConsolidatedNewsItem newsItem) {
      AtomAuthor author = AtomAuthor.builder().name(newsItem.getAuthor()).build();
      AtomContent content = AtomContent.builder().type("html").value(newsItem.getHtmlMessage()).build();
      AtomTitle title = AtomTitle.builder().type("text").value(newsItem.getMessage()).build();
      return AtomEntry.builder()
          .author(author)
          .content(content)
          .published(newsItem.getPostedAt())
          .id(newsItem.getMsgId())
          .title(title)
          .stocks(newsItem.getStocks())
          .build();
  }

  private JsonEntry convertToJsonEntry(ConsolidatedNewsItem newsItem) {
    AtomAuthor author = AtomAuthor.builder().name(newsItem.getAuthor()).build();

    return JsonEntry.builder()
        .author(author)
        .content(newsItem.getHtmlMessage())
        .title(newsItem.getMessage())
        .published((newsItem.getPostedAt()))
        .id(newsItem.getMsgId())
        .stocks(newsItem.getStocks().stream().map(f -> f.getLocalCode()).collect(Collectors.toList()))
        .build();
  }

  private void initNewPageAndArchiveCurrent() {
    String newId = UUID.randomUUID().toString();
    String newPageJsonHref = "/v1/getFeeds?page=" + newId;
    String newPageHref = "/getFeeds?page=" + newId;

    /* XML specific processing START */
    if (firstPageXml != null) {
      String archiveId = firstPageXml.getId();

      firstPageXml.setPrevious(AtomLink.builder().rel("previous").type("application/atom+xml").href(newPageHref).build());
      try {
        newsPageXml.put(archiveId, getPageAsString(firstPageXml));
        newsPageCreationDate.put(newId, Instant.now());
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
    }

    AtomFeed.AtomFeedBuilder newPageBuilder = AtomFeed.builder()
        .id(newId)
        .entries(new LinkedList<>())
        .current(AtomLink.builder().rel("current").type("application/atom+xml").href(newPageHref).build());

    if (firstPageXml != null) {
      newPageBuilder.next(AtomLink.builder().rel("next").type("application/atom+xml").href(firstPageXml.getCurrent().getHref()).build());
    }
    AtomFeed newPage = newPageBuilder.build();
    /* XML specific processing END */

    /* JSON specific processing START */
    if (firstPageJson != null) {
      String archiveId = firstPageJson.getId();

      firstPageJson.setPrevious(newPageJsonHref);
      try {
        newsPageJson.put(archiveId, getPageAsString(firstPageJson));
        newsPageCreationDate.put(newId, Instant.now());
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
    }

    JsonFeed.JsonFeedBuilder newJsonPageBuilder = JsonFeed.builder()
        .id(newId)
        .entries(new LinkedList<>())
        .fintagDict(new HashMap<>())
        .current(newPageJsonHref);

    if (firstPageJson != null) {
      newJsonPageBuilder.next(firstPageJson.getCurrent());
    }
    JsonFeed newPageJson = newJsonPageBuilder.build();
    /* JSON specific processing END */

    try {
      String newPageStringXml = getPageAsString(newPage);
      firstPageXml = newPage;
      newsPageXml.put(newId, newPageStringXml);

      String newPageStringJson = getPageAsString(newPageJson);
      firstPageJson = newPageJson;
      newsPageJson.put(newId, newPageStringJson);

      lastPageId = newId;

      isInitialized = true;
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }

  private String getPageAsString(AtomFeed page) throws JsonProcessingException {
    return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + xmlMapper.writeValueAsString(page);
  }

  private String getPageAsString(JsonFeed page) throws JsonProcessingException {
    return jsonMapper.writeValueAsString(page);
  }
}
