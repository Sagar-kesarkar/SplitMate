package com.splitmate.app.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class QuickExpenseRoutingTest {
    @Test
    fun noGroupRoutesToPersonalExpenses() {
        assertEquals(QuickExpenseDestination.PERSONAL, quickExpenseDestination(null))
    }

    @Test
    fun selectedGroupRoutesToSharedExpenses() {
        assertEquals(QuickExpenseDestination.SHARED, quickExpenseDestination("group_1"))
    }
}
