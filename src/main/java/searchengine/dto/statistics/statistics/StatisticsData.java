package searchengine.dto.statistics.statistics;

import java.util.List;

public record StatisticsData(TotalStatistics total, List<DetailedStatisticsItem> detailed) {

}