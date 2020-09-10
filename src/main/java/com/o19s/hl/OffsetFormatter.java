package com.o19s.hl;

import org.apache.lucene.search.highlight.Formatter;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.highlight.HighlightingPluginBase;
import org.apache.solr.highlight.SolrFormatter;

public class OffsetFormatter extends HighlightingPluginBase implements SolrFormatter {
    @Override
    public Formatter getFormatter(String fieldName, SolrParams params) {
        return new SimpleOffsetFormatter(
                params.getFieldParam(fieldName, HighlightParams.SIMPLE_PRE, "<em data-num-tokens=\"$numTokens\" data-score=\"$score\" data-end-offset=\"$endOffset\" data-start-offset=\"$startOffset\">"),
                params.getFieldParam(fieldName, HighlightParams.SIMPLE_POST, "</em>")
        );
    }

    @Override
    public String getDescription() {
        return "OffsetFormatter";
    }
}