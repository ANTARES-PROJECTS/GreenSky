package com.greencodes.greensky.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ProgressBarTest {

    @Test
    void rendersLikeTheReadme() {
        assertEquals("██████████ 100%", ProgressBar.render(5, 5, 10));
        assertEquals("██████░░░░ 60%", ProgressBar.render(3, 5, 10));
        assertEquals("░░░░░░░░░░ 0%", ProgressBar.render(0, 5, 10));
    }

    @Test
    void neverShows100PercentBeforeComplete() {
        assertEquals("█████████░ 99%", ProgressBar.render(99, 100, 10));
    }

    @Test
    void clampsAndHandlesEmpty() {
        assertEquals("██████████ 100%", ProgressBar.render(9, 5, 10));
        assertEquals("░░░░░░░░░░ 0%", ProgressBar.render(-2, 5, 10));
        assertEquals("", ProgressBar.render(1, 0, 10));
    }
}
