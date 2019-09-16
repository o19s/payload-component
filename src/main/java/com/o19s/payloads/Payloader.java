package com.o19s.payloads;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.WeightedSpanTerm;
import org.apache.lucene.search.highlight.WeightedSpanTermExtractor;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.PluginInfo;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.plugin.PluginInfoInitialized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Payloader implements PluginInfoInitialized {
    protected final SolrCore core;

    public Payloader(SolrCore core) {
        this.core = core;
    }

    public void init(PluginInfo info) {
        // No-op for now
    }

    public NamedList getPayloads(DocList docs, Query query, SolrQueryRequest request, String[] fields) throws IOException {
        SolrParams params = request.getParams();

        SolrIndexSearcher searcher = request.getSearcher();
        IndexSchema schema = searcher.getSchema();
        IndexReader reader = request.getSearcher().getSlowAtomicReader();


        SchemaField keyField = schema.getUniqueKeyField();
        if (keyField == null) {
            return null;
        }

        String[] fieldnames = getPayloadFields(query, request, fields);

        NamedList payloads = new SimpleOrderedMap();
        DocIterator iterator = docs.iterator();

        // Loop over the docs and extract payloads
        for (int i = 0; i < docs.size(); i++) {
            int docId = iterator.nextDoc();
            Document doc = searcher.doc(docId);

            NamedList docPayloads = new SimpleOrderedMap();
            for (String fieldname : fieldnames) {
                SchemaField field = schema.getFieldOrNull(fieldname);
                NamedList payloadsForField = getPayloadsForField(doc, docId, field, query, reader, request, params);

                if (payloadsForField != null) {
                    docPayloads.add(fieldname, payloadsForField);
                }
            } // for each field
            payloads.add(schema.printableUniqueKey(doc), docPayloads);
        } // for each doc

        return payloads;
    }

    private String[] getPayloadFields(Query query, SolrQueryRequest request, String[] defaults) {
        // TODO: Rolling with defaults for now, may add support for pl.fl
        return defaults;
    }

    public NamedList getPayloadsForField(Document doc, int docid, SchemaField field, Query query, IndexReader reader, SolrQueryRequest request, SolrParams params) throws IOException {
        NamedList resp = new SimpleOrderedMap();

        // Iterating over getFields handles multivalued field cases
        IndexableField[] fields = doc.getFields(field.getName());
        for (IndexableField currentField : fields) {
            String data = currentField.stringValue();

            WeightedSpanHelper wsh = new WeightedSpanHelper();
            TokenStream queryStream = field.getType().getIndexAnalyzer().tokenStream(field.getName(), data);
            Map<String, WeightedSpanTerm> spanTerms = wsh.getWeightedSpanTerms(query, 1f, queryStream);

            TokenStream stream = field.getType().getIndexAnalyzer().tokenStream(field.getName(), data);

            CharTermAttribute charAtt = stream.addAttribute(CharTermAttribute.class);
            PayloadAttribute payloadAtt = stream.addAttribute(PayloadAttribute.class);
            PositionIncrementAttribute posAtt = stream.addAttribute(PositionIncrementAttribute.class);

            stream.reset();

            int position = -1;
            WeightedSpanTerm weightedSpanTerm = null;

            for (boolean next = stream.incrementToken(); next; next = stream.incrementToken()) {
                String token = charAtt.toString();
                position += posAtt.getPositionIncrement();

                weightedSpanTerm = spanTerms.get(token);

                // Continue if no term found or no payload available
                if (weightedSpanTerm == null || payloadAtt.getPayload() == null) {
                    continue;
                }

                // If term was position sensitive, verify the position or continue
                if (weightedSpanTerm.isPositionSensitive() && !weightedSpanTerm.checkPosition(position)) {
                    continue;
                }

                // Setup the list if it hasn't been added yet
                if(resp.get(token) == null) {
                    resp.add(token, new ArrayList<String>());
                }

                // Add payload to the list for the matching token
                List<String> payloadList = (List) resp.get(token);
                payloadList.add(payloadAtt.getPayload().utf8ToString());
            }
            closeStream(stream);
        }

        return resp;
    }

    // Convenience for closing up token streams
    private void closeStream(TokenStream stream) throws IOException {
        if (stream != null) {
            stream.end();
            stream.close();
        }
    }

    // The WSTE doesn't expose the max chars to analyze so this is just tapping into that.
    private class WeightedSpanHelper extends WeightedSpanTermExtractor {
        public WeightedSpanHelper () {
            this.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);
        }
    }
}
