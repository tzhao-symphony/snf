package com.symphony.snf.stores;

import com.symphony.snf.model.stats.NewsStats;
import com.symphony.snf.model.atom.AtomEntry;
import com.symphony.snf.model.atom.AtomFeed;
import com.symphony.snf.model.atom.AtomLink;
import com.symphony.snf.services.AuthenticationService;
import com.symphony.snf.services.NewsService;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NewsStore {

  public static final int PAGE_SIZE = 50;

  public static final int STARTING_NEWS_COUNT = 100;

  Map<String, String> news = new HashMap<>();

  NewsService newsService;

  AuthenticationService authenticationService;

  String firstPageString;

  AtomFeed firstPage;

  XmlMapper xmlMapper;

  Instant latestNewsTime = Instant.EPOCH;

  String lastNewsId;

  @Getter
  boolean isInitialized = false;

  NewsStore(NewsService newsService, AuthenticationService authenticationService) {
    this.newsService = newsService;
    this.authenticationService = authenticationService;
    xmlMapper = new XmlMapper();
    xmlMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
  }

  public boolean isCurrent(String id) {
    if (id == null || firstPage == null) {
      return true;
    }
    return firstPage.getId().equals(id);
  }

  public String getNewsPage(String id) {
    if (StringUtils.isBlank(id)) {
      return firstPageString;
    }

    return news.get(id);
  }

  public NewsStats getStats() {
    NewsStats.NewsStatsBuilder builder = NewsStats.builder();
    int pageCount = news.size();
    int newsCount = firstPage == null ? 0 : (news.size() - 1) * PAGE_SIZE + firstPage.getEntries().size();
    builder.lastUpdate(latestNewsTime.toString()).pageCount(pageCount).newsCount(newsCount);
    return builder.build();
  }

  @Scheduled(fixedDelay = 10000)
  private void pollNews() {
    if (!authenticationService.areCredentialsSet()) {
      System.out.println("Postponing polling until credentials are set");
      return;
    }

    System.out.println("Storing news");
    LinkedList<AtomEntry> news = new LinkedList<>();

    if (lastNewsId == null) {
      int newsCount = 0;
      String oldestNewsId = null;
      while(newsCount < STARTING_NEWS_COUNT) {
        List<AtomEntry> newsFrom = newsService.getNewsFrom(oldestNewsId);
        if (newsFrom.isEmpty()) {
          break;
        }
        oldestNewsId = newsFrom.get(0).getId();
        news.addAll(0, newsFrom);
        newsCount = news.size();
      }
    } else {
      String lastId = lastNewsId;

      while(true) {
        List<AtomEntry> newsSince = newsService.getNewsSince(lastId);
        if (newsSince.isEmpty()) {
          break;
        }
        Collections.reverse(newsSince);
        news.addAll(newsSince);
        lastId = news.get(0).getId();
      }
    }

    if (news.size() == 0) {
      return;
    }

    lastNewsId = news.getLast().getId();

    if (firstPage == null) {
      initNewPageAndArchiveCurrent();
    }

    boolean hasChanged = false;
    for (AtomEntry entry: news) {
          if (firstPage.getEntries().size() >= PAGE_SIZE) {
            initNewPageAndArchiveCurrent();
          }
          firstPage.getEntries().addFirst(entry);
          latestNewsTime = Instant.parse(entry.getPublished());
          hasChanged =true;
    }

    if (hasChanged) {
      try {
        firstPageString = getPageAsString(firstPage);
        this.news.put(firstPage.getId(), firstPageString);
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
    }
  };

  private void initNewPageAndArchiveCurrent() {
    String newId = UUID.randomUUID().toString();

    if (firstPage != null) {
      firstPage.setPrevious(AtomLink.builder().rel("previous").type("application/atom+xml").href("/getFeeds?page=" + newId).build());
      String archiveId = firstPage.getId();
      String archive = null;
      try {
        archive = getPageAsString(firstPage);
        news.put(archiveId, archive);
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
    }


    AtomFeed.AtomFeedBuilder newPageBuilder = AtomFeed.builder()
        .id(newId)
        .entries(new LinkedList<>())
        .current(AtomLink.builder().rel("current").type("application/atom+xml").href("/getFeeds?page=" + newId).build());

    if (firstPage!= null) {
      newPageBuilder.next(AtomLink.builder().rel("next").type("application/atom+xml").href(firstPage.getCurrent().getHref()).build());
    }
    AtomFeed newPage = newPageBuilder.build();

    try {
      String newPageString = getPageAsString(newPage);
      firstPageString = newPageString;
      firstPage = newPage;
      news.put(newId, firstPageString);
      isInitialized = true;
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }

  private String getPageAsString(AtomFeed page) throws JsonProcessingException {
    return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + xmlMapper.writeValueAsString(page);
  }
}
