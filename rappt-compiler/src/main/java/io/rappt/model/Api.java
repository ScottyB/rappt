package io.rappt.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Api implements PIM {

    public String id;
    public List<Resource> resources = new ArrayList<>();
    public String rootURL = "";
    public boolean createMockData;
    public String apiParamKey = "";
    public String apiParamValue = "";

    public boolean isTokenApp;

    public Optional<OAuth> oauth = Optional.empty();
    public boolean isParseApp;
    public String appId;
    public String clientKey;

    public Api(String id) {
        this.id = id;
    }

    @Override
    public void accept(PIMVisitor visitor) {
        for (Resource ep : resources) {
            ep.accept(visitor);
        }
        visitor.visit(this);
    }
}
