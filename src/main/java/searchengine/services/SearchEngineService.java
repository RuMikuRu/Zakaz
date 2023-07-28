package searchengine.services;

import searchengine.dto.statistics.SearchDto;

import java.util.List;

public interface SearchEngineService {
    List<SearchDto> getSearchFromOneSite(String text,
                                         String url,
                                         int start,
                                         int limit);

    List<SearchDto> getFullSearch(String text,
                                  int start,
                                  int limit);
}
