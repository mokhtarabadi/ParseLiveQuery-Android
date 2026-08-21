package com.parse.livequery;

import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.PointerEncoder;
import com.parse.livequery.ClientOperation;

import org.json.JSONException;
import org.json.JSONObject;

class SubscribeClientOperation<T extends ParseObject> extends ClientOperation {

    private final int requestId;
    private final ParseQuery.State<T> state;
    private final String sessionToken;

    /* package */ SubscribeClientOperation(int requestId, ParseQuery.State<T> state, String sessionToken) {
        this.requestId = requestId;
        this.state = state;
        this.sessionToken = sessionToken;
    }

    @Override
    /* package */ JSONObject getJSONObjectRepresentation() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("op", "subscribe");
        jsonObject.put("requestId", requestId);
        jsonObject.put("sessionToken", sessionToken);

        JSONObject queryJsonObject = new JSONObject();
        queryJsonObject.put("className", state.className());

        // PR #110: Add fields support for selective key subscription
        // V5 FIX: Always ensure "objectId" is present in the fields array.
        // Without objectId the server cannot route events back to the correct
        // subscription, which leads to silently dropped CREATE/UPDATE/DELETE.
        if (state.selectedKeys() != null && state.selectedKeys().size() > 0) {
            org.json.JSONArray fieldsArray = new org.json.JSONArray();
            boolean hasObjectId = false;
            for (String key : state.selectedKeys()) {
                fieldsArray.put(key);
                if ("objectId".equals(key)) {
                    hasObjectId = true;
                }
            }
            if (!hasObjectId) {
                fieldsArray.put("objectId");
            }
            queryJsonObject.put("fields", fieldsArray);
        }

        PointerEncoder pointerEncoder = PointerEncoder.get();
        queryJsonObject.put("where", pointerEncoder.encode(state.constraints()));

        jsonObject.put("query", queryJsonObject);

        return jsonObject;
    }
}
