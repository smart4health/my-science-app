package com.healthmetrix.myscience.feature.login

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MilestoneProgressViewTest {

    private val weights = List(10) { 1 }

    @Test
    fun `getProgress with only a milestone calculates the correct percentage`() {
        val progress = weights.getProgress(1, 0f)

        assertThat(progress).isEqualTo(.1f)
    }

    @Test
    fun `getProgress with a sub milestone percentage calculates the correct percentage`() {
        val progress = weights.getProgress(1, .5f)

        assertThat(progress).isEqualTo(.15f)
    }

    @Test
    fun `getProgress at the first and last milestone calculates 0 and 100 percent`() {
        val noProgress = weights.getProgress(0, 0f)
        val allProgress = weights.getProgress(10, 0f)

        assertThat(noProgress).isEqualTo(0f)
        assertThat(allProgress).isEqualTo(1f)
    }

    @Test
    fun `getProgress with a negative number fails`() {
        assertThrows<IllegalArgumentException> {
            weights.getProgress(-1, 0f)
        }
    }

    @Test
    fun `getProgress beyond the last milestone fails`() {
        assertThrows<IllegalArgumentException> {
            weights.getProgress(11, 0f)
        }
    }

    @Test
    fun `sub milestone progress on the last milestone is ignored`() {
        val progress = weights.getProgress(10, .5f)
        assertThat(progress).isEqualTo(1f)
    }
}
