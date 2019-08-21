package com.o19s.payloads;

import org.apache.lucene.search.Query;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.PluginInfo;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.DocList;

public class Payloader {
    private SolrCore core;

    public Payloader(SolrCore core) {
        this.core = core;
    }

    public void init(PluginInfo info) {

    }

    public NamedList getPayloads(DocList docs, Query query, SolrQueryRequest request, String[] fields) {
        return null;
    }
}
