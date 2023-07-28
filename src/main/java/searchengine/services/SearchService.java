package searchengine.services;

import searchengine.dto.response.ResultDTO;
import searchengine.dto.statistics.SearchDto;
import searchengine.model.Lemma;
import searchengine.model.SitePage;

import java.util.List;

public interface SearchService {
    List<Lemma> getLemmaModelFromSite(List<String> lemmas, SitePage site);

    List<String> getLemmaFromSearchText(String text);

    List<SearchDto> createSearchDtoList(List<Lemma> lemmaList,
                                        List<String> textLemmaList,
                                        int start, int limit);

    String clearCodeFromTag(String text, String element);

    ResultDTO search(String query, String site, int offset);
}
