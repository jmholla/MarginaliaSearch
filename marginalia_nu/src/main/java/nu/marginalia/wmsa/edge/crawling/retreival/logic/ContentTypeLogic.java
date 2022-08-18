package nu.marginalia.wmsa.edge.crawling.retreival.logic;

import nu.marginalia.wmsa.edge.model.EdgeUrl;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class ContentTypeLogic {

    private static final Predicate<String> probableHtmlPattern = Pattern.compile("^.*\\.(htm|html|php|txt)$").asMatchPredicate();
    private static final Predicate<String> probableBinaryPattern = Pattern.compile("^.*\\.[a-z]+$").asMatchPredicate();
    private static final Set<String> blockedContentTypes = Set.of("text/css", "text/javascript");
    private static final List<String> acceptedContentTypePrefixes = List.of(
            "text/",
            "application/xhtml",
            "application/xml",
            "application/atom+xml",
            "application/rss+xml",
            "application/x-rss+xml",
            "application/rdf+xml",
            "x-rss+xml"
    );
    private boolean allowAllContentTypes = false;
    
    public void setAllowAllContentTypes(boolean allowAllContentTypes) {
        this.allowAllContentTypes = allowAllContentTypes;
    }

    public boolean isUrlLikeBinary(EdgeUrl url) {
        String pathLowerCase = url.path.toLowerCase();

        if (probableHtmlPattern.test(pathLowerCase))
            return false;

        return probableBinaryPattern.test(pathLowerCase);
    }

    public boolean isAllowableContentType(String contentType) {
        if (allowAllContentTypes)
            return true;
        if (blockedContentTypes.contains(contentType)) {
            return false;
        }
        for (var prefix : acceptedContentTypePrefixes) {
            if (contentType.startsWith(prefix))
                return true;
        }
        return false;
    }

}
