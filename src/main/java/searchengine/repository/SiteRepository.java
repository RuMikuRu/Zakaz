package searchengine.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.SitePage;



@Repository
public interface SiteRepository extends JpaRepository<SitePage, Long> {

    @Transactional
    SitePage findByUrl(String url);
}