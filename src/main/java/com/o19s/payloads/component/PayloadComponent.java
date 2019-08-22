package com.o19s.payloads.component;

import com.o19s.payloads.Payloader;
import com.o19s.payloads.params.PayloadParams;
import org.apache.lucene.search.Query;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.PluginInfo;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.handler.component.ShardRequest;
import org.apache.solr.handler.component.ShardResponse;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.util.SolrPluginUtils;
import org.apache.solr.util.plugin.PluginInfoInitialized;
import org.apache.solr.util.plugin.SolrCoreAware;

import java.io.IOException;
import java.util.List;

/*
    The payload component

    Returns the terms that matched the query along with their payloads.  Unlike
    highlighting no scoring or snippets are provided.

    {
        ....SOLR RESPONSE....
        "payloads" : {
            "one cool doc": {           // Doc ID
                "quick": [              // Term
                    "123 123 123 123",  // Payloads associated with the term
                    "123 123 123 123"
                ],
                "fox": [
                    "123 123 123 123",
                    "123 123 123 123"
                ]
            }
        }
    }
 */
public class PayloadComponent extends SearchComponent implements PluginInfoInitialized, SolrCoreAware {
    private PluginInfo info = PluginInfo.EMPTY_INFO;
    private Payloader payloader;

    @Override
    public void init(PluginInfo pluginInfo) {
        this.info = pluginInfo;
    }

    @Override
    public void prepare(ResponseBuilder responseBuilder) throws IOException {
        // No-op since the response builder isn't friendly to storing out parsed queries outside q/hl.q
    }

    @Override
    public void process(ResponseBuilder responseBuilder) throws IOException {
        SolrParams params = responseBuilder.req.getParams();
        boolean payloadsEnabled = payloadsEnabled(params);

        if (payloadsEnabled) {
            Query payloadQuery = null;

            // TODO: Would be nice to get the query parsing logic in prepare()
            // TODO: Maybe support pl.q in the future, using the main query for now
            // Read in query, utilize a lucene query type
            String plQ = params.get(CommonParams.Q);
            String parserType = QParserPlugin.DEFAULT_QTYPE;

            // Parse the query if possible
            if (plQ != null) {
                try {
                    QParser parser = QParser.getParser(plQ, parserType, responseBuilder.req);
                    payloadQuery = parser.getQuery();
                } catch (SyntaxError e) {
                    throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, e);
                }
            }

            // Setup payload fields
            String[] payloadFields;
            if (responseBuilder.getQparser() != null) {
                payloadFields = responseBuilder.getQparser().getDefaultHighlightFields();
            } else {
                payloadFields = params.getParams(CommonParams.DF);
            }

            // If the query isn't null, lets load some payloads
            if (payloadQuery != null) {
                NamedList payloadData = payloader.getPayloads(
                        responseBuilder.getResults().docList,
                        payloadQuery,
                        responseBuilder.req,
                        payloadFields
                );

                if (payloadData != null) {
                    responseBuilder.rsp.add(PayloadParams.NAME, payloadData);
                }
            }
        }
    }

    @Override
    public void finishStage(ResponseBuilder responseBuilder) {
        SolrParams params = responseBuilder.req.getParams();

        if (payloadsEnabled(params) && responseBuilder.stage == ResponseBuilder.STAGE_GET_FIELDS) {

            NamedList.NamedListEntry[] arr = new NamedList.NamedListEntry[responseBuilder.resultIds.size()];

            // TODO: make a generic routine to do automatic merging of id keyed data
            for (ShardRequest sreq : responseBuilder.finished) {
                if ((sreq.purpose & ShardRequest.PURPOSE_GET_HIGHLIGHTS) == 0) continue;
                for (ShardResponse srsp : sreq.responses) {
                    if (srsp.getException() != null) {
                        // can't expect the highlight content if there was an exception for this request
                        // this should only happen when using shards.tolerant=true
                        continue;
                    }
                    NamedList hl = (NamedList)srsp.getSolrResponse().getResponse().get(PayloadParams.NAME);
                    SolrPluginUtils.copyNamedListIntoArrayByDocPosInResponse(hl, responseBuilder.resultIds, arr);
                }
            }

            // remove nulls in case not all docs were able to be retrieved
            responseBuilder.rsp.add(PayloadParams.NAME, SolrPluginUtils.removeNulls(arr, new SimpleOrderedMap<>()));
        }
    }

    @Override
    public String getDescription() {
        return PayloadParams.NAME;
    }

    @Override
    public void inform(SolrCore core) {
        List<PluginInfo> children = info.getChildren(PayloadParams.NAME);
        if(children.isEmpty()) {
            Payloader defPayloader = new Payloader(core);
            defPayloader.init(PluginInfo.EMPTY_INFO);
            payloader = defPayloader;
        } else {
            payloader = core.createInitInstance(children.get(0),Payloader.class,null, Payloader.class.getName());
        }

    }

    @Override
    public void modifyRequest(ResponseBuilder responseBuilder, SearchComponent who, ShardRequest shardRequest) {
        SolrParams params = responseBuilder.req.getParams();
        if (!payloadsEnabled(params)) return;

        // Turn on highlighting only when retrieving fields
        // TODO: Verify this works as expected and doesn't step on highlighter component feet
        if ((shardRequest.purpose & ShardRequest.PURPOSE_GET_FIELDS) != 0) {
            shardRequest.purpose |= ShardRequest.PURPOSE_GET_HIGHLIGHTS;
            // should already be true...
            shardRequest.params.set(PayloadParams.PL, "true");
        } else {
            shardRequest.params.set(PayloadParams.PL, "false");
        }
    }

    @Override
    public void handleResponses(ResponseBuilder responseBuilder, ShardRequest shardRequest){
        // No-op
    }

    private boolean payloadsEnabled(SolrParams params) {
        return params.getBool(PayloadParams.PL, false);
    }
}
