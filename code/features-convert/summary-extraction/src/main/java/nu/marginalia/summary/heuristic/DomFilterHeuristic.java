package nu.marginalia.summary.heuristic;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import nu.marginalia.summary.SummaryExtractionFilter;
import org.jsoup.nodes.Document;

public class DomFilterHeuristic implements SummaryHeuristic {
    private final int maxSummaryLength;

    @Inject
    public DomFilterHeuristic(@Named("max-summary-length") Integer maxSummaryLength) {
        this.maxSummaryLength = maxSummaryLength;
    }

    @Override
    public String summarize(Document doc) {
        doc = doc.clone();

        var filter = new SummaryExtractionFilter();

        doc.filter(filter);

        return filter.getSummary(maxSummaryLength+32);
    }
}
