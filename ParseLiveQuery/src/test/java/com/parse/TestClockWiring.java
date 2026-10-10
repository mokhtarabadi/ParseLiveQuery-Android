// Sibling Docs: ./README.md | Decisions: ./DECISIONS.md
package com.parse;

import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.contains;

import com.parse.livequery.ParseLiveQueryClient;
import com.parse.livequery.ParseLiveQueryClock;
import com.parse.livequery.WebSocketClient;
import com.parse.livequery.WebSocketClientFactory;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.net.URI;

import com.parse.boltsinternal.Task;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Impl-level wiring tests for the injectable clock plus the empty
 * resubscribe guard. Uses the same Robolectric harness as
 * TestParseLiveQueryClient.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 21)
public class TestClockWiring {

    private WebSocketClient webSocketClient;
    private WebSocketClient.WebSocketClientCallback webSocketClientCallback;
    private ParseUser mockUser;

    @Before
    public void setUp() throws Exception {
        Parse.Configuration configuration = new Parse.Configuration.Builder(null)
                .applicationId("1234")
                .build();
        ParsePlugins.initialize(null, configuration);

        mockUser = mock(ParseUser.class);
        ParseCurrentUserController currentUserController = mock(ParseCurrentUserController.class);
        when(currentUserController.getAsync(anyBoolean())).thenAnswer(new Answer<Task<ParseUser>>() {
            @Override
            public Task<ParseUser> answer(InvocationOnMock invocation) throws Exception {
                return Task.forResult(mockUser);
            }
        });
        when(currentUserController.getCurrentSessionTokenAsync()).thenAnswer(new Answer<Task<String>>() {
            @Override
            public Task<String> answer(InvocationOnMock invocation) throws Exception {
                return Task.forResult(mockUser.getSessionToken());
            }
        });
        ParseCorePlugins.getInstance().registerCurrentUserController(currentUserController);
    }

    @After
    public void tearDown() throws Exception {
        ParseCorePlugins.getInstance().reset();
        ParsePlugins.reset();
    }

    private ParseLiveQueryClient newClientWithClock(final ParseLiveQueryClock clock) throws Exception {
        return ParseLiveQueryClient.Factory.getClient(new URI(""),
                new WebSocketClientFactory() {
                    @Override
                    public WebSocketClient createInstance(
                            WebSocketClient.WebSocketClientCallback callback, URI hostUrl) {
                        webSocketClientCallback = callback;
                        webSocketClient = mock(WebSocketClient.class);
                        when(webSocketClient.getState())
                                .thenReturn(WebSocketClient.State.CONNECTED);
                        return webSocketClient;
                    }
                }, new ImmediateExecutor(), clock);
    }

    private static long readWsConnectStartMs(Object client) throws Exception {
        java.lang.reflect.Field field =
                Class.forName("com.parse.livequery.ParseLiveQueryClientImpl")
                        .getDeclaredField("wsConnectStartMs");
        field.setAccessible(true);
        return field.getLong(client);
    }

    @Test
    public void reconnectPath_readsInjectedClock() throws Exception {
        final long fakeNow = 987654321L;
        ParseLiveQueryClient client = newClientWithClock(new ParseLiveQueryClock() {
            @Override
            public long currentTimeMillis() {
                return fakeNow;
            }
        });
        client.reconnect();
        webSocketClientCallback.onOpen();
        // Fails with the system clock: only the injected fake yields this exact value.
        assertEquals(fakeNow, readWsConnectStartMs(client));
    }

    @Test
    public void connectedWithEmptySubscriptions_sendsNothing() throws Exception {
        ParseLiveQueryClient client = newClientWithClock(ParseLiveQueryClock.SYSTEM);
        client.reconnect();
        webSocketClientCallback.onOpen();
        JSONObject connected = new JSONObject();
        connected.put("op", "connected");
        webSocketClientCallback.onMessage(connected.toString());
        verify(webSocketClient, never()).send(contains("\"op\":\"subscribe\""));
    }
}
