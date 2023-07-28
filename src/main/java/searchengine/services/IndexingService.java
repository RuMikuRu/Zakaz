package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.dto.response.ResultDTO;

@Service
public interface IndexingService {
    ResultDTO startIndexing();

    ResultDTO stopIndexing();

    ResultDTO indexPage(String urlPage);

}
