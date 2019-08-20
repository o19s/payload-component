package com.o19s.payloads;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.PluginInfo;
import org.apache.solr.core.SolrCore;

public class Payloader {
    private SolrCore core;

    public Payloader(SolrCore core) {
        this.core = core;
    }

    public void init(PluginInfo info) {

    }

    public NamedList getPayloads() {
        return null;
    }
}
