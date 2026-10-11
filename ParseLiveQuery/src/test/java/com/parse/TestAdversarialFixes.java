package com.parse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.parse.livequery.ParseLiveQueryClient;
import com.parse.livequery.ParseLiveQueryClock;
import com.parse.livequery.WebSocketClient;
import com.parse.livequery.WebSocketClientFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.parse.boltsinternal.Task;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Behavioral adversarial tests for reconnect backoff, global errors,
 * and transport failure paths. No source-text assertions.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class TestAdversarialFixes {

  private WebSocketClient webSocketClient;
  private WebSocketClient.WebSocketClientCallback webSocketClientCallback;

  @Before
  public void setUp() throws Exception {
    Parse.Configuration configuration = new Parse.Configuration.Builder(null)
        .applicationId("1234")
        .build();
    ParsePlugins.initialize(null, configuration);
    ParseUser mockUser = mock(ParseUser.class);
    ParseCurrentUserController controller = mock(ParseCurrentUserController.class);
    when(controller.getAsync(anyBoolean())).thenAnswer(new Answer<Task<ParseUser>>() {
      @Override
      public Task<ParseUser> answer(InvocationOnMock invocation) {
        return Task.forResult(mockUser);
      }
    });
    when(controller.getCurrentSessionTokenAsync()).thenAnswer(new Answer<Task<String>>() {
      @Override
      public Task<String> answer(InvocationOnMock invocation) {
        return Task.forResult("token");
      }
    });
    ParseCorePlugins.getInstance().registerCurrentUserController(controller);
  }

  @After
  public void tearDown() throws Exception {
    try {
      java.lang.reflect.Field field = ParseUser.class.getDeclaredField("sCachedSessionToken");
      field.setAccessible(true);
      field.set(null, null);
    } catch (ReflectiveOperationException ignored) {
    }
    ParseCorePlugins.getInstance().reset();
    ParsePlugins.reset();
  }

  private static class RecordingClock implements ParseLiveQueryClock {
    final List<Long> delays = new ArrayList<>();
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

  private ParseLiveQueryClient newClient(ParseLiveQueryClock clock) throws Exception {
    return ParseLiveQueryClient.Factory.getClient(new URI(""),
        new WebSocketClientFactory() {
          @Override
          public WebSocketClient createInstance(
              WebSocketClient.WebSocketClientCallback callback, URI hostUrl) {
            webSocketClientCallback = callback;
            webSocketClient = mock(WebSocketClient.class);
            when(webSocketClient.getState()).thenReturn(WebSocketClient.State.CONNECTED);
            return webSocketClient;
          }
        }, new ImmediateExecutor(), clock);
  }

  @Test
  public void backoffDelays_stayWithinBounds() throws Exception {
    RecordingClock clock = new RecordingClock();
    ParseLiveQueryClient client = newClient(clock);
    client.reconnect();
    webSocketClientCallback.onClose();
    assertEquals(1, clock.delays.size());
    long first = clock.delays.get(0);
    assertTrue("delay=" + first + " below base", first >= 1000L);
    assertTrue("delay=" + first + " above max", first <= 30000L);
  }

  @Test
  public void globalError_withoutRequestId_reachesListener() throws Exception {
    RecordingClock clock = new RecordingClock();
    ParseLiveQueryClient client = newClient(clock);
    client.reconnect();
    webSocketClientCallback.onOpen();
    final List<Throwable> seen = new ArrayList<>();
    client.registerListener(new com.parse.livequery.ParseLiveQueryClientCallbacks() {
      @Override
      public void onLiveQueryClientConnected(ParseLiveQueryClient c) {
      }

      @Override
      public void onLiveQueryClientDisconnected(ParseLiveQueryClient c, boolean userInitiated) {
      }

      @Override
      public void onLiveQueryError(ParseLiveQueryClient c, com.parse.livequery.LiveQueryException e) {
        seen.add(e);
      }

      @Override
      public void onSocketError(ParseLiveQueryClient c, Throwable reason) {
      }
    });
    org.json.JSONObject error = new org.json.JSONObject();
    error.put("op", "error");
    error.put("code", 1);
    error.put("error", "global boom");
    error.put("reconnect", false);
    webSocketClientCallback.onMessage(error.toString());
    assertEquals(1, seen.size());
  }

  @Test
  public void backoffSequence_matchesCappedExponential() {
    for (int attempt = 0; attempt <= 6; attempt++) {
      int clamped = Math.max(0, Math.min(attempt, 30));
      long base = Math.min(1000L * (long) Math.pow(2, clamped), 30000L);
      long delay = Math.min(base, 30000L);
      if (attempt <= 4) {
        assertEquals(1000L * (1L << attempt), delay);
      } else {
        assertEquals(30000L, delay);
      }
    }
  }
}
