package com.symphony.snf.services;

import com.symphony.snf.model.ConsolidatedNewsItem;
import com.symphony.snf.model.Finref;
import com.symphony.snf.model.NewsItem;
import com.symphony.snf.model.NewsResponse;
import com.symphony.snf.model.atom.AtomAuthor;
import com.symphony.snf.model.atom.AtomContent;
import com.symphony.snf.model.atom.AtomEntry;
import com.symphony.snf.model.atom.AtomTitle;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Service
@AllArgsConstructor
public class NewsService {

  private final String NEWS_URL = "https://pro.hammerstone.us/api/v2/feed";

  private final String HAMMERSTONE_KEY = "?key=STftCRyKz4Y2CScF663aLxH";

  private final String FIRST_ID = "first_id";

  private final String LAST_ID = "last_id";

  private  FinrefService finref;

  public NewsResponse fetchNewsSince(String lastId) {
    Map<String, String> params = new HashMap<>();
    if (StringUtils.isNotBlank(lastId)) {
      params.put(LAST_ID, lastId);
    }
    return fetchNews(params);
  }

  public NewsResponse fetchNewsFrom(String fromId) {
    Map<String, String> params = new HashMap<>();
    if (StringUtils.isNotBlank(fromId)) {
      params.put(FIRST_ID, fromId);
    }
    return fetchNews(params);
  }

  private NewsResponse fetchNews(Map<String, String> params) {
    RestTemplate restTemplate = new RestTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

    StringBuilder queryParamsBuilder = new StringBuilder(HAMMERSTONE_KEY);
    if (params != null) {
      for (Entry<String, String> entry: params.entrySet()) {
        queryParamsBuilder.append("&");
        queryParamsBuilder.append(entry.getKey());
        queryParamsBuilder.append("=");
        queryParamsBuilder.append(entry.getValue());
      }
    }
    String url = NEWS_URL + queryParamsBuilder.toString();
    System.out.println("fetching:" + url);
    HttpEntity<String> entity = new HttpEntity<>(headers);
    try {
      ResponseEntity<NewsResponse> result =
          restTemplate.exchange(url, HttpMethod.GET, entity, NewsResponse.class);
      return result.getBody();
    } catch (RestClientException e) {
      e.printStackTrace();
      return new NewsResponse();
    }

  }

  public List<AtomEntry> getNewsSince(String lastId) {
    return getConsolidatedItems(fetchNewsSince(lastId));
  }

  public List<AtomEntry> getNewsFrom(String fromId) {
    return getConsolidatedItems(fetchNewsFrom(fromId));
  }

  private List<AtomEntry> getConsolidatedItems(NewsResponse resp) {
    List entries = new ArrayList();
    List<ConsolidatedNewsItem> consolidatedNewsItems = consolidateFinrefs(resp);

    for (ConsolidatedNewsItem newsItem: consolidatedNewsItems) {
      AtomAuthor author = AtomAuthor.builder().name(newsItem.getAuthor()).build();
      AtomContent content = AtomContent.builder().type("html").value(newsItem.getHtmlMessage()).build();
      AtomTitle title = AtomTitle.builder().type("text").value(newsItem.getMessage()).build();
      AtomEntry entry = AtomEntry.builder()
          .author(author)
          .content(content)
          .published(newsItem.getPostedAt())
          .id(newsItem.getMsgId())
          .title(title)
          .stocks(newsItem.getStocks())
          .build();
      entries.add(entry);
    }
    return entries;
  }

  private List<ConsolidatedNewsItem> consolidateFinrefs(NewsResponse response) {
    List<ConsolidatedNewsItem> list = new ArrayList<>();

    for (NewsItem<String> item: response) {
      List<Finref> instruments = item.getStocks().stream().map(stock -> finref.getFinref(stock)).toList();
      ConsolidatedNewsItem newsItem = ConsolidatedNewsItem.builder()
          .author(item.getAuthor())
          .htmlMessage(item.getHtmlMessage())
          .msgId(item.getMsgId())
          .postedAt(item.getPostedAt())
          .message(item.getMessage())
          .stocks(instruments).build();
      list.add(newsItem);
    }
    return list;
  }
}
