package com.o19s.payloads;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.QueryTermExtractor;
import org.apache.lucene.search.highlight.WeightedTerm;
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
        IndexReader reader = new TermVectorReusingLeafReader(request.getSearcher().getSlowAtomicReader());


        SchemaField keyField = schema.getUniqueKeyField();
        if (keyField == null) {
            return null;
        }

        String[] fieldnames = getPayloadFields(query, request, fields);
        // --- prefetch code --- Not sure we need it
        // --- fast vector wrapper? --

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

        // Extract the terms from the query
        WeightedTerm[] terms = QueryTermExtractor.getTerms(query);

        // Setup list of target terms to look for in the fields
        List<String> targetTerms = new ArrayList<>();

        for (WeightedTerm term : terms) {
            TokenStream stream = field.getType().getQueryAnalyzer().tokenStream(field.getName(), term.getTerm());
            CharTermAttribute charAtt = stream.addAttribute(CharTermAttribute.class);

            stream.reset();
            for(boolean next = stream.incrementToken(); next; next = stream.incrementToken()) {
                String token = charAtt.toString();
                if (!targetTerms.contains(token)){
                    targetTerms.add(token);
                }
            }

            closeStream(stream);
        }

        // TODO: Multivalued fields won't work with this impl, need to get all fields for name and loop
        IndexableField myField = doc.getField(field.getName());
        String data = myField.stringValue();
        TokenStream stream = field.getType().getIndexAnalyzer().tokenStream(field.getName(), data);
        CharTermAttribute charAtt = stream.addAttribute(CharTermAttribute.class);
        PayloadAttribute payloadAtt = stream.addAttribute(PayloadAttribute.class);

        stream.reset();
        for (boolean next = stream.incrementToken(); next; next = stream.incrementToken()) {
            String token = charAtt.toString();
            if (targetTerms.contains(token) && payloadAtt.getPayload() != null) {
                // Make key in map if it doesn't exist yet
                if (resp.get(token) == null) {
                    resp.add(token, new ArrayList<String>());
                }

                List<String> payloadList = (List) resp.get(token);
                payloadList.add(payloadAtt.getPayload().utf8ToString());
            }
        }
        closeStream(stream);

        return resp;
    }

    // Convenience for closing up token streams
    private void closeStream(TokenStream stream) throws IOException {
        if (stream != null) {
            stream.end();
            stream.close();
        }
    }

    // Brought over from the DefaultSolrHighlighter
    class TermVectorReusingLeafReader extends FilterLeafReader {

        private int lastDocId = -1;
        private Fields tvFields;

        public TermVectorReusingLeafReader(LeafReader in) {
            super(in);
        }

        @Override
        public Fields getTermVectors(int docID) throws IOException {
            if (docID != lastDocId) {
                lastDocId = docID;
                tvFields = in.getTermVectors(docID);
            }
            return tvFields;
        }

        @Override
        public CacheHelper getCoreCacheHelper() {
            return null;
        }

        @Override
        public CacheHelper getReaderCacheHelper() {
            return null;
        }
    }
}
