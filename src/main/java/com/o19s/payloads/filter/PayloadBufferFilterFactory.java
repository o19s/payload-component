package com.o19s.payloads.filter;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.util.Map;

public class PayloadBufferFilterFactory extends TokenFilterFactory {
    public PayloadBufferFilterFactory(Map<String,String> args) {
        super(args);
        if(!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    @Override
    public PayloadBufferFilter create(TokenStream input) {
        return new PayloadBufferFilter(input);
    }
}
