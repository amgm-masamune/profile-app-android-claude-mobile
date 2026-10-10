package com.amgm.personallog.ledger

import com.amgm.personallog.data.model.CategoryTotal
import com.amgm.personallog.data.model.Expense
import com.amgm.personallog.ui.ledger.LedgerLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LedgerLogicTest {
    private fun e(id: Long, date: String, amount: Int, cat: String = "食費") =
        Expense(id = id, date = date, amount = amount, category = cat, createdAt = 0)

    @Test
    fun shiftMonth_crossesYear() {
        assertEquals("2026-09", LedgerLogic.shiftMonth("2026-10", -1))
        assertEquals("2027-01", LedgerLogic.shiftMonth("2026-12", 1))
        assertEquals("2025-12", LedgerLogic.shiftMonth("2026-01", -1))
    }

    @Test
    fun recentMonths_isOldestFirst() {
        assertEquals(
            listOf("2026-05", "2026-06", "2026-07", "2026-08", "2026-09", "2026-10"),
            LedgerLogic.recentMonths("2026-10"),
        )
        assertEquals("2025-11", LedgerLogic.recentMonths("2026-04").first())
    }

    @Test
    fun monthLabel_formats() {
        assertEquals("2026年10月", LedgerLogic.monthLabel("2026-10"))
        assertEquals("2026年1月", LedgerLogic.monthLabel("2026-01"))
    }

    @Test
    fun groupByDate_sortsDescendingAndSums() {
        val groups = LedgerLogic.groupByDate(
            listOf(e(1, "2026-10-01", 100), e(2, "2026-10-03", 300), e(3, "2026-10-01", 50)),
        )
        assertEquals(listOf("2026-10-03", "2026-10-01"), groups.map { it.date })
        assertEquals(150L, groups[1].total)
        assertEquals(2, groups[1].items.size)
    }

    @Test
    fun categoryShares_percentages() {
        val shares = LedgerLogic.categoryShares(listOf(CategoryTotal("外食", 1000), CategoryTotal("食費", 3000)))
        assertEquals("食費", shares[0].category)
        assertEquals(75, shares[0].percent)
        assertEquals(25, shares[1].percent)
        assertTrue(LedgerLogic.categoryShares(emptyList()).isEmpty())
    }

    @Test
    fun monthlyTotals_includesEmptyMonths() {
        val totals = LedgerLogic.monthlyTotals(
            listOf(e(1, "2026-10-01", 100), e(2, "2026-08-15", 200), e(3, "2026-10-31", 5)),
            listOf("2026-08", "2026-09", "2026-10"),
        )
        assertEquals(listOf(200L, 0L, 105L), totals.map { it.total })
    }

    @Test
    fun filterMonth_matchesPrefix() {
        val list = listOf(e(1, "2026-10-01", 1), e(2, "2026-11-01", 1))
        assertEquals(1, LedgerLogic.filterMonth(list, "2026-10").size)
    }

    @Test
    fun validate_acceptsGoodInput() {
        val v = LedgerLogic.validate("2026-10-05", "1200", "食費")
        assertTrue(v.isValid)
        assertEquals(1200, v.amount)
        assertEquals("2026-10-05", v.date)
    }

    @Test
    fun validate_rejectsBadAmounts() {
        for (bad in listOf("", "0", "-5", "+5", "12.5", "abc", "99999999999", "1,000")) {
            assertNotNull("amount '$bad' should be rejected", LedgerLogic.validate("2026-10-05", bad, "食費").amountError)
        }
    }

    @Test
    fun validate_requiresCategoryAndValidDate() {
        assertNotNull(LedgerLogic.validate("2026-10-05", "100", null).categoryError)
        assertNotNull(LedgerLogic.validate("2026-10-05", "100", " ").categoryError)
        assertNotNull(LedgerLogic.validate("2026-02-30", "100", "食費").dateError)
        assertNotNull(LedgerLogic.validate("10/05", "100", "食費").dateError)
        assertNull(LedgerLogic.validate("2026-10-05", "100", "食費").dateError)
        assertFalse(LedgerLogic.validate("", "", null).isValid)
    }

    @Test
    fun formatting() {
        assertEquals("¥1,234,567", LedgerLogic.formatYen(1234567))
        assertEquals("¥0", LedgerLogic.formatYen(0))
        assertEquals("9999", LedgerLogic.compactYen(9999))
        assertEquals("1万", LedgerLogic.compactYen(10000))
        assertEquals("1.2万", LedgerLogic.compactYen(12345))
        assertEquals("10月5日(月)", LedgerLogic.dayLabel("2026-10-05"))
    }

    @Test
    fun defaultDate_usesTodayOnlyInCurrentMonth() {
        val today = LocalDate.of(2026, 10, 10)
        assertEquals("2026-10-10", LedgerLogic.defaultDate("2026-10", today))
        assertEquals("2026-09-01", LedgerLogic.defaultDate("2026-09", today))
    }
}
