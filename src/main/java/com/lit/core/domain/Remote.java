package com.lit.core.domain;

public class Remote {
    private final String name;
    private final String url;

    public Remote(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}
