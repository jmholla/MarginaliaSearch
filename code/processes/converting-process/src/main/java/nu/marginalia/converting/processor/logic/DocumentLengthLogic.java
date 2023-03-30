package nu.marginalia.converting.processor.logic;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import nu.marginalia.converting.model.DisqualifiedException;
import nu.marginalia.language.model.DocumentLanguageData;
import nu.marginalia.model.idx.DocumentFlags;

import java.util.EnumSet;

@Singleton
public class DocumentLengthLogic {
    private final int minDocumentLength;
    private final int shortDocumentLength = 2500;
    private final int longDocumentLength = 7500;

    @Inject
    public DocumentLengthLogic(@Named("min-document-length") Integer minDocumentLength) {
        this.minDocumentLength = minDocumentLength;
    }

    public int getEncodedAverageLength(DocumentLanguageData dld) {
        int totalWords = dld.totalNumWords();
        int numSentences = dld.sentences.length;

        if (totalWords == 0 || numSentences == 0) {
            return 0;
        }

        return (int) Math.round((totalWords / (double) numSentences) / 4.);
    }

    public void validateLength(DocumentLanguageData dld) throws DisqualifiedException {
        if (dld.totalNumWords() < minDocumentLength) {
            throw new DisqualifiedException(DisqualifiedException.DisqualificationReason.LENGTH);
        }
    }

}
