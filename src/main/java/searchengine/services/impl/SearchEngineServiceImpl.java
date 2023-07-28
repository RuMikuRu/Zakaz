package searchengine.services.impl;

import org.springframework.stereotype.Service;
import searchengine.dto.statistics.SearchDto;
import searchengine.model.Lemma;
import searchengine.model.SitePage;
import searchengine.repository.SiteRepository;
import searchengine.services.SearchEngineService;
import searchengine.services.SearchService;

import java.util.ArrayList;
import java.util.List;

@Service
public record SearchEngineServiceImpl(SiteRepository siteRepository,
                                      SearchService searchService) implements SearchEngineService {

    public List<SearchDto> getSearchFromOneSite(String text,
                                                String url,
                                                int start,
                                                int limit) {
        SitePage site = siteRepository.findByUrl(url);
        List<String> textLemmaList = searchService.getLemmaFromSearchText(text);
        List<Lemma> foundLemmaList = searchService.getLemmaModelFromSite(textLemmaList, site);
        return searchService.createSearchDtoList(foundLemmaList, textLemmaList, start, limit);
    }

    public List<SearchDto> getFullSearch(String text,
                                         int start,
                                         int limit) {
        List<SitePage> siteList = siteRepository.findAll();
        List<SearchDto> result = new ArrayList<>();
        List<Lemma> foundLemmaList = new ArrayList<>();
        List<String> textLemmaList = searchService.getLemmaFromSearchText(text);

        int i = 0;
        while (i < siteList.size()) {
            SitePage site = siteList.get(i);
            foundLemmaList.addAll(searchService.getLemmaModelFromSite(textLemmaList, site));
            i++;
        }

        List<SearchDto> searchData = new ArrayList<>();
        for (Lemma l : foundLemmaList) {
            if (l.getLemma().equals(text)) {
                searchData = (searchService.createSearchDtoList(foundLemmaList, textLemmaList, start, limit));
                searchData.sort((o1, o2) -> Float.compare(o2.relevance(), o1.relevance()));
                if (searchData.size() > limit) {
                    var y = start;
                    while (y < limit) {
                        result.add(searchData.get(y));
                        y++;
                    }
                    return result;
                }
            }
        }
        return searchData;
    }
}