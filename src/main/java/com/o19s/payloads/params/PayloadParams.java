package com.o19s.payloads.params;

public interface PayloadParams {
    // Name of the component
    public static final String NAME = "payloads";

    // Set pl to on/true to enable the payload component
    public static final String PL = "pl";

    // pl.q is used internally inside prepare/process to setup the query
    public static final String PLQ = "pl.q";
}
