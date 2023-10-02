package com.symphony.snf.stores;

import com.symphony.snf.model.atom.AtomEntry;
import com.symphony.snf.model.atom.AtomFeed;
import com.symphony.snf.model.atom.AtomLink;
import com.symphony.snf.services.NewsService;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NewsStore {

  public static final int PAGE_SIZE = 13;

  Map<String, String> news = new HashMap<>();

  @Autowired
  NewsService newsService;

  String firstPageString;

  AtomFeed firstPage;

  XmlMapper xmlMapper;

  Instant latestNews = Instant.EPOCH;

  NewsStore() {
    xmlMapper = new XmlMapper();
    xmlMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
  }

  public String getNewsPage(String id) {
    if (StringUtils.isBlank(id)) {
      return firstPageString;
    }

    String page = news.get(id);
    if (page == null) {
      throw new RuntimeException(String.format("No page with id %s was found", id));
    }
    return page;
  }


  @Scheduled(fixedDelay = 10000)
  private void storeNews() {
    System.out.println("Storing news");
    List<AtomEntry> news = newsService.getNews(null);

    if (firstPage == null) {
      initNewPageAndArchiveCurrent();
    }

    boolean hasChanged = false;
    for (AtomEntry entry: news) {
        if (Instant.parse(entry.getPublished()).isAfter(latestNews)) {
          hasChanged =true;
          firstPage.getEntries().add(entry);
          latestNews = Instant.parse(entry.getPublished());
          if (firstPage.getEntries().size() >= PAGE_SIZE) {
            initNewPageAndArchiveCurrent();
          }
        }
    }
    if (hasChanged) {
      try {
        firstPageString = xmlMapper.writeValueAsString(firstPage);
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
        archive = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + xmlMapper.writeValueAsString(firstPage);
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
      news.put(archiveId, archive);
    }


    AtomFeed.AtomFeedBuilder newPageBuilder = AtomFeed.builder()
        .id(newId)
        .entries(new ArrayList<>())
        .current(AtomLink.builder().rel("current").type("application/atom+xml").href("/getFeeds?page=" + newId).build());

    if (firstPage!= null) {
      newPageBuilder.next(AtomLink.builder().rel("next").type("application/atom+xml").href(firstPage.getCurrent().getHref()).build());
    }
    AtomFeed newPage = newPageBuilder.build();

    try {
      String newPageString = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + xmlMapper.writeValueAsString(newPage);
      firstPageString = newPageString;
      firstPage = newPage;
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }

  private String getFirstPageAsString() {
    try {
      return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + xmlMapper.writeValueAsString(firstPage);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return "Oups";
  }
}
