package com.o19s.payloads;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.FilterLeafReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.search.Query;
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
        IndexReader reader = new TermVectorReusingLeafReader(request.getSearcher().getLeafReader());

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

    public NamedList getPayloadsForField(Document doc, int docid, SchemaField field, Query query, IndexReader reader, SolrQueryRequest request, SolrParams params) {
        return null;
    }

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

    }
}
