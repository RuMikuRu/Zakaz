package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexSearch;
import searchengine.model.Lemma;;
import searchengine.model.Page;


import java.util.List;

@Repository
public interface IndexSearchRepository extends JpaRepository<IndexSearch, Long> {

    @Transactional
    @Query(value = "select * from Words_index where Words_index.lemma_id in :lemmas and Words_index.page_id in :pages", nativeQuery = true)
    List<IndexSearch> findByPageAndLemmas(@Param("lemmas") List<Lemma> lemmaList,
                                          @Param("pages") List<Page> pages);
}
