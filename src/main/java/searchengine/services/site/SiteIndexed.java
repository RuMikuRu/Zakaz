package searchengine.services.site;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import searchengine.model.Site;
import searchengine.model.SitesList;
import searchengine.dto.statistics.IndexDto;
import searchengine.dto.statistics.LemmaDto;
import searchengine.dto.statistics.PageDto;
import searchengine.exception.CurrentInterruptedException;
import searchengine.model.IndexSearch;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SitePage;
import searchengine.model.enums.Status;
import searchengine.repository.IndexSearchRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.index.WebParser;
import searchengine.services.lemma.LemmaIndexer;
import searchengine.services.pageconvertor.PageIndexer;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
@Slf4j
public class SiteIndexed implements Callable<Boolean> {

    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexSearchRepository indexRepository;
    private final LemmaIndexer lemmaIndexer;
    private final WebParser webParser;
    private final String url;
    private final SitesList sitesListConfiguration;

    @Override
    public Boolean call() {
        if (siteRepository.findByUrl(url) != null) {
            log.info("start site date delete from ".concat(url));
            SitePage site = siteRepository.findByUrl(url);
            site.setStatus(Status.INDEXING);
            site.setName(getSiteName());
            site.setStatusTime(new Date());
            siteRepository.saveAndFlush(site);
            siteRepository.delete(site);
        }
        log.info("Site indexing start ".concat(url).concat(" ").concat(getSiteName()) );
        SiteModelIndexing siteModelIndexing = new SiteModelIndexing();
        SitePage site = siteModelIndexing.getSiteModelRecord();
        try {
            if (!Thread.interrupted()) {
                List<PageDto> pageDtoList;
                if (!Thread.interrupted()) {
                    String urls = url.concat("/");
                    List<PageDto> pageDtosList = new CopyOnWriteArrayList<>();
                    List<String> urlList = new CopyOnWriteArrayList<>();
                    ForkJoinPool forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
                    List<PageDto> pages = forkJoinPool.invoke(new PageIndexer(urls,urlList, pageDtosList, sitesListConfiguration));
                    pageDtoList = new CopyOnWriteArrayList<>(pages);
                } else throw new CurrentInterruptedException("Fork join exception!");
                List<Page> pageList = new CopyOnWriteArrayList<>();
                int start;
                String pagePath;
                for (PageDto page : pageDtoList) {
                    start = page.url().indexOf(url) + url.length();
                    pagePath = page.url().substring(start);
                    pageList.add(new Page(site, pagePath, page.code(), page.content()));
                }
                pageRepository.saveAllAndFlush(pageList);
            } else {
                throw new CurrentInterruptedException("Local interrupted exception.");
            }
            new LemmaIndexing().saveLemmasInLemmaDTO();
            new AllSiteIndexing().getSiteAllIndexing(site);
        } catch (CurrentInterruptedException e) {
            log.error("WebParser stopped from ".concat(url).concat(". ").concat(e.getMessage()));
            new SiteModelIndexing().getErrorSiteModelRecord(site);
            new CurrentInterruptedException("Interrupted exception");
        }
        return true;
    }

    private String getSiteName() {
        return sitesListConfiguration.getSites().stream()
                .filter(site -> site.getUrl().equals(url))
                .findFirst()
                .map(Site::getName)
                .orElse("");
    }

    private class SiteModelIndexing {
        protected SitePage getSiteModelRecord() {
            SitePage site = new SitePage();
            site.setUrl(url);
            site.setName(getSiteName());
            site.setStatus(Status.INDEXING);
            site.setStatusTime(new Date());
            siteRepository.saveAndFlush(site);
            return site;
        }

        protected void getErrorSiteModelRecord(SitePage site) {
            SitePage sites = new SitePage();
            sites.setLastError("WebParser stopped");
            sites.setStatus(Status.FAILED);
            sites.setStatusTime(new Date());
            siteRepository.saveAndFlush(site);
        }
    }
    private class LemmaIndexing {

        protected void saveLemmasInLemmaDTO() throws CurrentInterruptedException {
            if (!Thread.interrupted()) {
                SitePage siteModel = siteRepository.findByUrl(url);
                siteModel.setStatusTime(new Date());
                lemmaIndexer.startLemmaIndexer();
                List<LemmaDto> lemmaDtoList = lemmaIndexer.getLemmaDtoList();
                List<Lemma> lemmaList = new CopyOnWriteArrayList<>();

                for (LemmaDto lemmaDto : lemmaDtoList) {
                    lemmaList.add(new Lemma(lemmaDto.lemma(), lemmaDto.frequency(), siteModel));
                }
                lemmaRepository.saveAllAndFlush(lemmaList);
            } else {
                throw new CurrentInterruptedException("Invalid saveLemmasInLemmaDTO");
            }
        }
    }

    private class AllSiteIndexing {
        protected void getSiteAllIndexing(SitePage site) throws CurrentInterruptedException {
            if (!Thread.interrupted()) {
                webParser.startWebParser(site);
                List<IndexDto> indexDtoList = new CopyOnWriteArrayList<>(webParser.getConfig());
                List<IndexSearch> indexModels = new CopyOnWriteArrayList<>();
                site.setStatusTime(new Date());
                Page page;
                Lemma lemma;
                for (IndexDto indexDto : indexDtoList) {
                    page = pageRepository.getById(indexDto.pageID());
                    lemma = lemmaRepository.getById(indexDto.lemmaID());
                    indexModels.add(new IndexSearch(page, lemma, indexDto.rank()));
                }
                indexRepository.saveAllAndFlush(indexModels);
                log.info("WebParser stopping ".concat(url));
                site.setStatusTime(new Date());
                site.setStatus(Status.INDEXED);
                siteRepository.saveAndFlush(site);

            } else {
                throw new CurrentInterruptedException("Invalid getSiteAllIndexing");
            }
        }
    }
}