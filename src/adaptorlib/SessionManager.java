// Copyright 2011 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package adaptorlib;

import com.sun.net.httpserver.HttpExchange;

import java.security.SecureRandom;
import java.util.*;

/**
 * Generic session management, but intended for authn bookkeeping. It implements
 * very lazy session cleanup and does not use any other threads. It cleans up
 * old sessions as it creates a new session, so it is fine with keeping a
 * session around for days past its expiration time if no new sessions are being
 * created.
 */
class SessionManager<E> {
  private final TimeProvider timeProvider;
  private final ClientStore<E> clientStore;
  private final Map<String, Session> sessions = new HashMap<String, Session>();
  private final Map<String, Long> lastAccess = new HashMap<String, Long>();
  /** Lifetime of sessions, in milliseconds. */
  private final long sessionLifetime;
  /** Maximum frequency to check for expired sessions, in milliseconds. */
  private final long cleanupFrequency;
  private long nextCleanup;
  private final Random random = new SecureRandom();

  /**
   * @param clientStore storage for communicating session id with client
   * @param sessionLifetime lifetime of sessions, in milliseconds
   * @param cleanupFrequency maximum frequency to check for expired sessions, in
   *    milliseconds
   */
  public SessionManager(ClientStore<E> clientStore, long sessionLifetime,
                        long cleanupFrequency) {
    this(new SystemTimeProvider(), clientStore, sessionLifetime,
         cleanupFrequency);
  }

  protected SessionManager(TimeProvider timeProvider,
                           ClientStore<E> clientStore, long sessionLifetime,
                           long cleanupFrequency) {
    this.timeProvider = timeProvider;
    this.clientStore = clientStore;
    this.sessionLifetime = sessionLifetime;
    this.cleanupFrequency = cleanupFrequency;
  }

  public Session getSession(E clientState) {
    return getSession(clientState, true);
  }

  public Session getSession(E clientState, boolean create) {
    String value = clientStore.retrieve(clientState);
    if (value == null) {
      // No pre-existing session found.
      return create ? createSession(clientState) : null;
    }

    synchronized (this) {
      Session session = sessions.get(value);
      if (session != null) {
        updateLastAccess(value);
        return session;
      }

      // Could not find session specified. Assume it expired.
      return createSession(clientState);
    }
  }

  protected synchronized Session createSession(E clientState) {
    cleanupExpiredSessions();
    Session session = new Session();
    byte[] rawId = new byte[16];
    random.nextBytes(rawId);
    String id = bytesToString(rawId);
    sessions.put(id, session);
    clientStore.store(clientState, id);
    updateLastAccess(id);
    return session;
  }

  protected synchronized void updateLastAccess(String id) {
    lastAccess.put(id, timeProvider.currentTimeMillis());
  }

  protected synchronized void cleanupExpiredSessions() {
    long currentTime = timeProvider.currentTimeMillis();
    if (nextCleanup > currentTime) {
      return;
    }

    List<String> toRemove = new LinkedList<String>();
    long ageLimit = currentTime - sessionLifetime;
    for (Map.Entry<String, Long> me : lastAccess.entrySet()) {
      if (ageLimit > (long) me.getValue()) {
        toRemove.add(me.getKey());
      }
    }

    for (String id : toRemove) {
      sessions.remove(id);
      lastAccess.remove(id);
    }

    nextCleanup = currentTime + cleanupFrequency;
  }

  /**
   * Formats the provided bytes as a string. {@code b} must be a multiple of 4.
   */
  private static String bytesToString(byte[] b) {
    String s = "";
    for (int i = 0; i < b.length; i += 4) {
      int j = bytesToInt(b, i);
      s += toFixedSizeHexString(j);
    }
    return s;
  }

  /**
   * Packs the first four bytes starting at {@code start} into a single integer.
   */
  private static int bytesToInt(byte[] b, int start) {
    return ((b[start + 0] & 0xff) << 24)
        | ((b[start + 1] & 0xff) << 16)
        | ((b[start + 2] & 0xff) << 8)
        | ((b[start + 3] & 0xff) << 0);
  }

  /**
   * Converts the provided integer to an eight character string.
   */
  private static String toFixedSizeHexString(int i) {
    String s = Integer.toHexString(i);
    int padding = Integer.SIZE / 4 - s.length();
    for (int j = 0; j < padding; j++) {
      s = "0" + s;
    }
    return s;
  }

  /** A single-value storage per client. */
  public static interface ClientStore<E> {
    /** Returns the previously-stored value or {@code null}. */
    public String retrieve(E clientState);

    /** Stores the value for this client. */
    public void store(E clientState, String value);
  }

  public static class HttpExchangeClientStore
      implements ClientStore<HttpExchange> {

    private final String cookieName;
    private final String exchangeAttributeName;

    public HttpExchangeClientStore() {
      this("sessid");
    }

    public HttpExchangeClientStore(String cookieName) {
      if (cookieName == null) {
        throw new NullPointerException();
      }
      this.cookieName = cookieName;
      this.exchangeAttributeName = this.getClass().getName() + "." + cookieName;
    }

    @Override
    public String retrieve(HttpExchange ex) {
      String value = (String) ex.getAttribute(exchangeAttributeName);
      if (value != null) {
        return value;
      }

      String cookies = ex.getRequestHeaders().getFirst("Cookie");
      if (cookies == null) {
        return null;
      }

      String[] cookieArray = cookies.split(";");
      for (String cookie : cookieArray) {
        String[] keyValue = cookie.split("=", 2);
        if (keyValue.length == 1) {
          continue;
        }
        keyValue[0] = keyValue[0].trim();
        if (cookieName.equals(keyValue[0])) {
          return keyValue[1];
        }
      }

      return null;
    }

    @Override
    public void store(HttpExchange ex, String value) {
      ex.setAttribute(exchangeAttributeName, value);
      ex.getResponseHeaders().set("Set-Cookie", cookieName + "=" + value
                                  + "; Path=/");
    }
  }
}
