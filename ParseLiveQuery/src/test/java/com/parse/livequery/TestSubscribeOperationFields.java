package com.parse.livequery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.parse.ParseObject;
import com.parse.ParseQuery;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;

/**
 * Adversarial tests for SubscribeClientOperation V5 fix:
 * verifying that "objectId" is always present in the fields array.
 */
public class TestSubscribeOperationFields {

    /**
     * When selectedKeys omits "objectId", it should be injected automatically.
     */
    @Test
    public void objectIdInjectedWhenMissing() throws Exception {
        ParseQuery<ParseObject> query = new ParseQuery<>("Test");
        query.selectKeys(Arrays.asList("name", "email"));

        // Access internal state via reflection
        java.lang.reflect.Field stateField = ParseQuery.class.getDeclaredField("queryState");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ParseQuery.State<ParseObject> state =
            (ParseQuery.State<ParseObject>) stateField.get(query);

        SubscribeClientOperation<ParseObject> op =
            new SubscribeClientOperation<>(1, state, "test-session");

        JSONObject json = op.getJSONObjectRepresentation();
        JSONObject queryObj = json.getJSONObject("query");
        JSONArray fields = queryObj.getJSONArray("fields");

        boolean found = false;
        for (int i = 0; i < fields.length(); i++) {
            if ("objectId".equals(fields.getString(i))) {
                found = true;
                break;
            }
        }
        assertTrue("objectId must be present in fields array even when caller omits it", found);
        assertTrue("name must be present", fields.toString().contains("name"));
        assertTrue("email must be present", fields.toString().contains("email"));
    }

    /**
     * When selectedKeys is null or empty, fields array should NOT be added.
     */
    @Test
    public void noFieldsArray_whenNoSelectedKeys() throws Exception {
        ParseQuery<ParseObject> query = new ParseQuery<>("Test");
        // No selectKeys called — selectedKeys is null

        java.lang.reflect.Field stateField = ParseQuery.class.getDeclaredField("queryState");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ParseQuery.State<ParseObject> state =
            (ParseQuery.State<ParseObject>) stateField.get(query);

        SubscribeClientOperation<ParseObject> op =
            new SubscribeClientOperation<>(1, state, "test-session");

        JSONObject json = op.getJSONObjectRepresentation();
        JSONObject queryObj = json.getJSONObject("query");
        assertFalse("fields should NOT be present when selectedKeys is null",
            queryObj.has("fields"));
    }

    /**
     * When selectedKeys already contains "objectId", it should NOT be duplicated.
     */
    @Test
    public void noDuplicateObjectId_whenAlreadyPresent() throws Exception {
        ParseQuery<ParseObject> query = new ParseQuery<>("Test");
        query.selectKeys(Arrays.asList("objectId", "name"));

        java.lang.reflect.Field stateField = ParseQuery.class.getDeclaredField("queryState");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ParseQuery.State<ParseObject> state =
            (ParseQuery.State<ParseObject>) stateField.get(query);

        SubscribeClientOperation<ParseObject> op =
            new SubscribeClientOperation<>(1, state, "test-session");

        JSONObject json = op.getJSONObjectRepresentation();
        JSONArray fields = json.getJSONObject("query").getJSONArray("fields");

        int count = 0;
        for (int i = 0; i < fields.length(); i++) {
            if ("objectId".equals(fields.getString(i))) count++;
        }
        assertEquals("objectId must appear exactly once", 1, count);
    }
}
