package com.o19s.hl;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.util.TestHarness;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;

/**
 * Make sure the old OffsetFormatter in the com.o19s.labs package still works.
 *
 */
public class OldLabsOffsetClassTests extends SolrTestCaseJ4 {
    @BeforeClass
    public static void beforeClass() throws Exception {
        initCore("solrconfig-v7.xml","schema.xml");
    }

    @Test
    public void testOffsets() {
        HashMap<String, String> args = new HashMap<>();
        args.put("defType", "edismax");
        args.put("hl", "true");
        args.put("hl.fl", "content");
        args.put("qf", "content");
        args.put("q.alt", "*:*");
        TestHarness.LocalRequestFactory sumLRF = h.getRequestFactory(
                "standard", 0, 200, args);

        assertU(adoc("content", "A long day's night.",
                "id", "1"));
        assertU(commit());
        assertU(optimize());
        assertQ("Offset test",
                sumLRF.makeRequest("night"),
                "//lst[@name='highlighting']/lst[@name='1']",
                "//lst[@name='1']/arr[@name='content']/str[contains(text(),'data-num-tokens=\"1\"')]",
                "//lst[@name='1']/arr[@name='content']/str[contains(text(),'data-score=\"1.0\"')]",
                "//lst[@name='1']/arr[@name='content']/str[contains(text(),'data-start-offset=\"13\"')]",
                "//lst[@name='1']/arr[@name='content']/str[contains(text(),'data-end-offset=\"18\"')]"
        );
    }
}