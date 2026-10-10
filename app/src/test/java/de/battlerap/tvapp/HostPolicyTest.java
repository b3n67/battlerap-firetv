package de.battlerap.tvapp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HostPolicyTest {

    @Test
    public void allowsSiteAndSubdomains() {
        assertTrue(MainActivity.isAllowedHost("battlerap.de"));
        assertTrue(MainActivity.isAllowedHost("www.battlerap.de"));
        assertTrue(MainActivity.isAllowedHost("BATTLERAP.DE"));
        assertTrue(MainActivity.isAllowedHost("cdn.battlerap.de"));
    }

    @Test
    public void blocksLookalikesAndNull() {
        assertFalse(MainActivity.isAllowedHost("battlerap.de.attacker.com"));
        assertFalse(MainActivity.isAllowedHost("evilbattlerap.de"));
        assertFalse(MainActivity.isAllowedHost("battlerap.de.evil.com"));
        assertFalse(MainActivity.isAllowedHost(null));
        assertFalse(MainActivity.isAllowedHost(""));
    }
}
