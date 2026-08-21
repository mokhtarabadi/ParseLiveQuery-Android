package com.parse.livequery;

import org.json.JSONException;
import org.json.JSONObject;

class ConnectClientOperation extends ClientOperation {

    private final String applicationId;
    private final String clientKey;
    private final String sessionToken;

    ConnectClientOperation(String applicationId, String clientKey, String sessionToken) {
        this.applicationId = applicationId;
        this.clientKey = clientKey;
        this.sessionToken = sessionToken;
    }

    @Override
    JSONObject getJSONObjectRepresentation() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("op", "connect");
        jsonObject.put("applicationId", applicationId);
        if (clientKey != null) {
            jsonObject.put("clientKey", clientKey);
        }
        jsonObject.put("sessionToken", sessionToken);
        return jsonObject;
    }
}
