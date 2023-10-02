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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class NewsService {

  private final String NEWS_URL = "https://pro.hammerstone.us/api/v2/feed?key=STftCRyKz4Y2CScF663aLxHk";

  private  FinrefService finref;

  public NewsResponse fetchNews(String lastId) {
    RestTemplate restTemplate = new RestTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

    String queryParams = lastId != null ? "&last_id=" + lastId : "";
    HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
    ResponseEntity<NewsResponse> result =
        restTemplate.exchange(NEWS_URL + queryParams, HttpMethod.GET, entity, NewsResponse.class);
    return (NewsResponse) result.getBody();
  }

  public List<AtomEntry> getNews(String lastId) {
    List entries = new ArrayList();
    NewsResponse resp = fetchNews(lastId);
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
