package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import searchengine.model.Site;
import searchengine.model.SitesList;
import searchengine.dto.response.ResultDTO;
import searchengine.model.SitePage;
import searchengine.repository.IndexSearchRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.index.WebParser;
import searchengine.services.lemma.LemmaIndexer;
import searchengine.services.site.SiteIndexed;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static searchengine.model.enums.Status.INDEXING;

@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {
    private ExecutorService executorService;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexSearchRepository indexRepository;
    private final LemmaIndexer lemmaIndexer;
    private final WebParser webParser;
    private final SitesList config;

    public IndexingServiceImpl() {
    }

    public ResultDTO startIndexing() {
        if (isIndexingActive()) {
            log.debug("Indexing is already running.");
            new ResultDTO(false, "Индексация уже запущена").getError();

        } else {

            List<Site> siteList = config.getSites();
            executorService = Executors.
                    newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            for (Site site : siteList) {
                String url = String.valueOf(site.getUrl());
                SitePage siteModel = new SitePage();
                siteModel.setName(site.getName());
                log.info("Indexing web site ".concat(site.getName()));
                executorService.submit(new SiteIndexed(pageRepository, siteRepository, lemmaRepository,
                        indexRepository, lemmaIndexer, webParser, url, config));
            }
            executorService.shutdown();
        }
        return new ResultDTO(true);
    }

    public ResultDTO stopIndexing() {
        if (!isIndexingActive()) {
            log.info("Site indexing is already running!");
            return new ResultDTO(false, "Индексация не запущена");
        } else {
            log.info("Index stopping.");
            executorService.shutdown();
            return new ResultDTO(true);
        }
    }

    private boolean isIndexingActive() {
        siteRepository.flush();
        Iterable<SitePage> siteList = siteRepository.findAll();
        for (SitePage site : siteList) {
            if (site.getStatus() == INDEXING) {
                return true;
            }
        }
        return false;
    }

    public ResultDTO indexPage(String url) {
        if (url.isEmpty()) {
            log.info("Страница не указана");
            return new ResultDTO(false, "Страница не указана", HttpStatus.BAD_REQUEST);
        } else {
            if (reindexSite(url)) {
                log.info("Страница - " + url + " - добавлена на переиндексацию");
                return new ResultDTO(true, HttpStatus.OK);
            } else {
                log.info("Указанная страница" + "за пределами конфигурационного файла");
                return new ResultDTO(false, "Указанная страница" + "за пределами конфигурационного файла", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private boolean reindexSite(String urlPage){
        if (isUrlSiteEquals(urlPage)) {
            log.info("Начата переиндексация сайта - " + urlPage);
            executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            executorService.submit(new SiteIndexed(pageRepository, siteRepository, lemmaRepository, indexRepository, lemmaIndexer, webParser, urlPage, config));
            executorService.shutdown();
            return true;
        } else {
            return false;
        }
    }

    private boolean isUrlSiteEquals(String url) {
        return config.getSites().stream().anyMatch(site -> site.getUrl().equals(url));
    }

}