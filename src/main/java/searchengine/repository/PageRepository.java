package searchengine.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SitePage;


import java.util.Collection;
import java.util.List;


@Repository
public interface PageRepository extends JpaRepository<Page, Long> {
    @Transactional
    long countBySiteId(SitePage siteId);

    @Transactional
    Page getById(long pageID);

    @Transactional
    Iterable<Page> findBySiteId(SitePage sitePath);

    @Transactional
    @Query(value = "SELECT * FROM Words_index JOIN Page  ON Page.id = Words_index.page_id WHERE Words_index.lemma_id IN :lemmas", nativeQuery = true)
    List<Page> findByLemmaList(@Param("lemmas") Collection<Lemma> lemmas);

}