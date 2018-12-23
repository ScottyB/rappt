package io.rappt.model;

import io.rappt.compiler.FormatUtils;
import io.rappt.compiler.IntermediateModel;

public class OAuth implements PIM {
    public String apiProvider = "";
    public String apiKey = "";
    public String apiSecret = "";
    public String apiVerifierParameter = "";
    public String callback = "";

    // TODO: Generalise for other providers
    public IntermediateModel.OAuth transformComponent(final FormatUtils utils, final AppModel appModel, final IntermediateModel model) {
        IntermediateModel.OAuth oAuth = new IntermediateModel.OAuth();
        oAuth.apiScribeProvider = this.apiProvider + "Api.class";
        return oAuth;
    }

}
