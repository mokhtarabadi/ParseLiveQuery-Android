// Sibling Docs: ./README.md | Decisions: ./DECISIONS.md
package com.parse.livequery;

/**
 * Injectable time source for the LiveQuery client.
 *
 * <p>Production code uses {@link #SYSTEM}; tests inject a fake to make
 * reconnect timing deterministic. Satisfies the Clock Injection rule in
 * {@code docs/conventions.md} — no direct clock reads in business logic.
 */
public interface ParseLiveQueryClock {

    long currentTimeMillis();

    ParseLiveQueryClock SYSTEM = System::currentTimeMillis;
}
