// Sibling Docs: ./README.md | Decisions: ./DECISIONS.md
package com.parse;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
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
@Config(sdk = 28)
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
        try {
            java.lang.reflect.Field field =
                    ParseUser.class.getDeclaredField("sCachedSessionToken");
            field.setAccessible(true);
            field.set(null, null);
        } catch (ReflectiveOperationException ignored) {
            // Upstream SDK without the token cache: nothing to clear.
        }
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

            @Override
            public void postDelayed(Runnable runnable, long delayMs) {
            }

            @Override
            public void removeCallbacks(Runnable runnable) {
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

    /** Recording fake clock: captures backoff delays and fires manually. */
    private static class RecordingClock implements ParseLiveQueryClock {
        final java.util.List<Long> delays = new java.util.ArrayList<>();
        private long now = 1000L;

        @Override
        public long currentTimeMillis() {
            return now;
        }

        @Override
        public void postDelayed(Runnable runnable, long delayMs) {
            delays.add(delayMs);
        }

        @Override
        public void removeCallbacks(Runnable runnable) {
        }
    }

    @Test
    public void backoffDelay_isDeterministicThroughClock() throws Exception {
        RecordingClock clock = new RecordingClock();
        ParseLiveQueryClient client = newClientWithClock(clock);
        client.reconnect();
        // Socket drops before any handshake: first reconnect must wait exactly BASE delay.
        webSocketClientCallback.onClose();
        assertEquals(1, clock.delays.size());
        assertEquals(Long.valueOf(1000L), clock.delays.get(0));
    }

    @Test
    public void staleSocketCallback_doesNotScheduleReconnect() throws Exception {
        RecordingClock clock = new RecordingClock();
        ParseLiveQueryClient client = newClientWithClock(clock);
        client.reconnect();
        WebSocketClient.WebSocketClientCallback firstSocket = webSocketClientCallback;
        client.reconnect();
        WebSocketClient.WebSocketClientCallback secondSocket = webSocketClientCallback;
        // Old socket close arriving late must not schedule anything.
        firstSocket.onClose();
        assertTrue(clock.delays.isEmpty());
        // Current socket close schedules exactly one reconnect.
        secondSocket.onClose();
        assertEquals(1, clock.delays.size());
    }
}
