package searchengine.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.dto.response.ResultDTO;
import searchengine.dto.statistics.SearchDto;
import searchengine.exception.CurrentIOException;
import searchengine.lemma.LemmaEngine;
import searchengine.model.IndexSearch;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SitePage;
import searchengine.repository.IndexSearchRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.SearchService;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public record SearchServiceImpl(LemmaEngine lemmaEngine, LemmaRepository lemmaRepository, PageRepository pageRepository,
                                IndexSearchRepository indexRepository, SiteRepository siteRepository,
                                SearchEngineServiceImpl searchEngineServiceImpl) implements SearchService {

    private List<SearchDto> getSearchDtoList(ConcurrentHashMap<Page, Float> pageList,
                                             List<String> textLemmaList) {
        List<SearchDto> searchDtoList = new ArrayList<>();
        StringBuilder titleStringBuilder = new StringBuilder();
        for (Page page : pageList.keySet()) {
            String uri = page.getPath();
            String content = page.getContent();
            SitePage pageSite = page.getSiteId();
            String site = pageSite.getUrl();
            String siteName = pageSite.getName();
            String title = clearCodeFromTag(content, "title");
            String body = clearCodeFromTag(content, "body");
            titleStringBuilder.append(title).append(body);
            float pageValue = pageList.get(page);
            List<Integer> lemmaIndex = new ArrayList<>();
            int i = 0;
            while (i < textLemmaList.size()) {
                String lemma = textLemmaList.get(i);
                try {
                    lemmaIndex.addAll(lemmaEngine.findLemmaIndexInText(titleStringBuilder.toString(), lemma));
                } catch (IOException e) {
                    new CurrentIOException(e.getMessage());
                }
                i++;
            }
            Collections.sort(lemmaIndex);
            StringBuilder snippetBuilder = new StringBuilder();
            List<String> wordList = getWordsFromSiteContent(titleStringBuilder.toString(), lemmaIndex);
            int y = 0;
            while (y < wordList.size()) {
                snippetBuilder.append(wordList.get(y)).append(".");
                if (y > 3) {
                    break;
                }
                y++;
            }
            searchDtoList.add(new SearchDto(site, siteName, uri, title, snippetBuilder.toString(), pageValue));
        }
        return searchDtoList;
    }

    private List<String> getWordsFromSiteContent(String content, List<Integer> lemmaIndex) {
        List<String> result = new ArrayList<>();
        int i = 0;
        while (i < lemmaIndex.size()) {
            int start = lemmaIndex.get(i);
            int end = content.indexOf(" ", start);
            int next = i + 1;
            while (next < lemmaIndex.size() && 0 < lemmaIndex.get(next) - end && lemmaIndex.get(next) - end < 5) {
                end = content.indexOf(" ", lemmaIndex.get(next));
                next += 1;
            }
            i = next - 1;
            String word = content.substring(start, end);
            int startIndex;
            int nextIndex;
            if (content.lastIndexOf(" ", start) != -1) {
                startIndex = content.lastIndexOf(" ", start);
            } else startIndex = start;
            if (content.indexOf(" ", (end + lemmaIndex.size() / (lemmaIndex.size() / 10))) != -1) {
                nextIndex = content.indexOf(" ", end + lemmaIndex.size() / 10);
            } else nextIndex = content.indexOf(" ", end);
            String text = content.substring(startIndex, nextIndex).replaceAll(word, "<b>".concat(word).concat("</b>"));
            result.add(text);
            i++;
        }
        result.sort(Comparator.comparing(String::length).reversed());
        return result;
    }

    private Map<Page, Float> getRelevanceFromPage(List<Page> pageList,
                                                  List<IndexSearch> indexList) {
        Map<Page, Float> relevanceMap = new HashMap<>();

        int i = 0;
        int j = 0;
        while (i < pageList.size()) {
            Page page = pageList.get(i);
            float relevance = 0;
            while (j < indexList.size()) {
                IndexSearch index = indexList.get(j);
                if (index.getPage() == page) {
                    relevance += index.getRank();
                }
                j++;
            }
            relevanceMap.put(page, relevance);
            i++;
        }

        Map<Page, Float> allRelevanceMap = new HashMap<>();

        relevanceMap.keySet().forEach(page -> {
            float relevance = relevanceMap.get(page) / Collections.max(relevanceMap.values());
            allRelevanceMap.put(page, relevance);
        });

        List<Map.Entry<Page, Float>> sortList = new ArrayList<>(allRelevanceMap.entrySet());
        sortList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        Map<Page, Float> map = new ConcurrentHashMap<>();
        Entry<Page, Float> pageModelFloatEntry;
        int y = 0;
        while (y < sortList.size()) {
            pageModelFloatEntry = sortList.get(y);
            map.putIfAbsent(pageModelFloatEntry.getKey(), pageModelFloatEntry.getValue());
            y++;
        }
        return map;
    }

    public List<Lemma> getLemmaModelFromSite(List<String> lemmas, SitePage site) {
        lemmaRepository.flush();
        List<Lemma> lemmaModels = lemmaRepository.findLemmaListBySite(lemmas, site);
        List<Lemma> result = new ArrayList<>(lemmaModels);
        result.sort(Comparator.comparingInt(Lemma::getFrequency));
        return result;
    }

    public List<String> getLemmaFromSearchText(String text) {
        String[] words = text.toLowerCase(Locale.ROOT).split(" ");
        List<String> lemmaList = new ArrayList<>();
        int i = 0;
        List<String> list;
        while (i < words.length) {
            String lemma = words[i];
            try {
                list = lemmaEngine.getLemma(lemma);
                lemmaList.addAll(list);
            } catch (IOException e) {
                new CurrentIOException(e.getMessage());
            }
            i++;
        }
        return lemmaList;
    }

    public List<SearchDto> createSearchDtoList(List<Lemma> lemmaList,
                                               List<String> textLemmaList,
                                               int start, int limit) {
        List<SearchDto> result = new ArrayList<>();
        pageRepository.flush();
        if (lemmaList.size() >= textLemmaList.size()) {
            List<Page> pagesList = pageRepository.findByLemmaList(lemmaList);
            indexRepository.flush();
            List<IndexSearch> indexesList = indexRepository.findByPageAndLemmas(lemmaList, pagesList);
            Map<Page, Float> relevanceMap = getRelevanceFromPage(pagesList, indexesList);
            List<SearchDto> searchDtos = getSearchDtoList((ConcurrentHashMap<Page, Float>) relevanceMap, textLemmaList);
            if (start > searchDtos.size()) {
                return new ArrayList<>();
            }
            if (searchDtos.size() > limit) {
                int i = start;
                while (i < limit) {
                    result.add(searchDtos.get(i));
                    i++;
                }
                return result;
            } else return searchDtos;

        } else return result;
    }

    public String clearCodeFromTag(String text, String element) {
        Document doc = Jsoup.parse(text);
        Elements elements = doc.select(element);
        String html = elements.stream().map(Element::html).collect(Collectors.joining());
        return Jsoup.parse(html).text();
    }

    public ResultDTO search(String query, String site, int offset) {
        List<SearchDto> searchData;
        if (!site.isEmpty()) {
            if (siteRepository.findByUrl(site) == null) {

                return new ResultDTO(false, "Данная страница находится за пределами сайтов,\n" +
                        "указанных в конфигурационном файле", HttpStatus.BAD_REQUEST);
            } else {
                searchData = searchEngineServiceImpl.getSearchFromOneSite(query, site, offset, 30);
            }
        } else {
            searchData = searchEngineServiceImpl.getFullSearch(query, offset, 30);
        }
        return new ResultDTO(true, searchData.size(), searchData, HttpStatus.OK);
    }
}