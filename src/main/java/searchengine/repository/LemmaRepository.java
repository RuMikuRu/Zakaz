package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.SitePage;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {

    @Transactional
    Lemma getById(long lemmaID);

    @Transactional
    long countBySiteModelId(SitePage site);

    @Transactional
    List<Lemma> findBySiteModelId(SitePage siteId);

    @Transactional
    @Query(value = "select * from Lemma where Lemma.lemma in :lemmas AND Lemma.site_id = :site", nativeQuery = true)
    List<Lemma> findLemmaListBySite(List<String> lemmas, SitePage site);

}