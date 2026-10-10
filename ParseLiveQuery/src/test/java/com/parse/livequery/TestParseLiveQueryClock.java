// Sibling Docs: ./README.md | Decisions: ./DECISIONS.md
package com.parse.livequery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Contract tests for the injectable {@link ParseLiveQueryClock}.
 * The reconnect path must read time only through this abstraction so
 * tests can run deterministically (see task 02 acceptance criteria).
 */
public class TestParseLiveQueryClock {

    @Test
    public void systemClock_returnsSaneTime() {
        long before = System.currentTimeMillis();
        long actual = ParseLiveQueryClock.SYSTEM.currentTimeMillis();
        long after = System.currentTimeMillis();
        assertTrue("system clock out of range", before <= actual && actual <= after);
    }

    @Test
    public void fakeClock_isDeterministic() {
        ParseLiveQueryClock fake = new ParseLiveQueryClock() {
            @Override
            public long currentTimeMillis() {
                return 123456789L;
            }
        };
        assertEquals(123456789L, fake.currentTimeMillis());
        assertEquals(fake.currentTimeMillis(), fake.currentTimeMillis());
    }
}
