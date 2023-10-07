package com.symphony.snf.controllers;

import com.symphony.snf.stores.NewsStore;

import lombok.AllArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@CrossOrigin
@RestController
@AllArgsConstructor
public class NewsFeedController {


  private NewsStore newsStore;


  @GetMapping("/getFeeds")
  public ResponseEntity<String> getFeeds(@RequestParam(value = "page", required = false) String pageId) {
    String response = newsStore.getNewsPageXml(pageId);
    if (response == null) {
      return ResponseEntity.notFound().build();
    }

    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.setContentType(MediaType.APPLICATION_XML);
    if (!newsStore.isCurrent(pageId)) {
      responseHeaders.setCacheControl(CacheControl.maxAge(Duration.ofDays(60)));
    }
    return ResponseEntity.ok().headers(responseHeaders).body(response);
  }

  @GetMapping("/v1/getFeeds")
  public ResponseEntity<String> getJsonFeeds(@RequestParam(value = "page", required = false) String pageId) {
    String response = newsStore.getNewsPageJson(pageId);
    if (response == null) {
      return ResponseEntity.notFound().build();
    }

    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.setContentType(MediaType.APPLICATION_JSON);
    if (!newsStore.isCurrent(pageId)) {
      responseHeaders.setCacheControl(CacheControl.maxAge(Duration.ofDays(60)));
    }
    return ResponseEntity.ok().headers(responseHeaders).body(response);
  }


}
