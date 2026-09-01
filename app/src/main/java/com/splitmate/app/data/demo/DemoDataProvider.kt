package com.splitmate.app.data.demo

import com.splitmate.app.model.*
import java.util.Calendar
import kotlin.math.max

object DemoDataProvider {
    const val PERSONAL_SAMPLE_DATA_VERSION = "1"
    const val GOA_GROUP_ID = "demo_group_goa"
    const val CURRENT_USER_ID = "u1"
    const val ALEX_ID = "u2"
    const val SARAH_ID = "u3"
    const val MICHAEL_ID = "u4"

    val currentUser = User(CURRENT_USER_ID, "You", "sagar@splitmate.app", "", true)
    val friendUsers = listOf(
        User(ALEX_ID, "Alex Rivera", "alex.r@example.com", ""),
        User(SARAH_ID, "Sarah Chen", "sarah.c@example.com", ""),
        User(MICHAEL_ID, "Michael Scott", "michael.s@example.com", ""),
        User("u5", "Emma Watson", "emma.w@example.com", "")
    )
    val allDemoUsers: List<User> get() = listOf(currentUser) + friendUsers
    val demoFriends = friendUsers.map { user -> Friend(user.id, user.name, user.email, user.avatar) }

    private fun currentMonthDay(day: Int, hour: Int = 12): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, day.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Returns a recent timestamp that always stays inside the current month and never enters the future. */
    private fun recentCurrentMonthTime(hoursAgo: Int): Long {
        val now = System.currentTimeMillis()
        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return max(startOfMonth, now - hoursAgo * 60L * 60L * 1_000L)
    }

    val demoGroups: List<Group> get() = listOf(
        Group(
            id = GOA_GROUP_ID,
            name = "Trip to Goa",
            description = "Weekend getaway expenses, beach shacks, and car rentals",
            icon = "🌴",
            memberIds = listOf(CURRENT_USER_ID, ALEX_ID, SARAH_ID, MICHAEL_ID),
            createdAt = currentMonthDay(1),
            budget = 20_500.00,
            ownerUserId = CURRENT_USER_ID
        ),
        Group("demo_group_apartment", "Apartment 402", "Monthly household expenses", "🏠", listOf(CURRENT_USER_ID, ALEX_ID, "u5"), currentMonthDay(2), ownerUserId = ALEX_ID),
        Group("demo_group_foodies", "Foodie Gang", "Weekend meals and takeout", "🍕", listOf(CURRENT_USER_ID, SARAH_ID, "u5"), currentMonthDay(3), ownerUserId = SARAH_ID)
    )

    val demoExpenses: List<Expense> get() = listOf(
        Expense(
            id = "demo_goa_beach_dinner", title = "Beach Shack Dinner", amount = 640.00,
            paidByUserId = CURRENT_USER_ID, groupId = GOA_GROUP_ID, category = Category.FOOD,
            dateMillis = currentMonthDay(5, 19), participantIds = listOf(CURRENT_USER_ID, ALEX_ID, SARAH_ID, MICHAEL_ID),
            splitType = SplitType.EQUAL, notes = "Dinner by the beach"
        ),
        Expense(
            id = "demo_goa_car_rental", title = "Car Rental", amount = 1_200.00,
            paidByUserId = SARAH_ID, groupId = GOA_GROUP_ID, category = Category.TRAVEL,
            dateMillis = currentMonthDay(6, 10), participantIds = listOf(CURRENT_USER_ID, ALEX_ID, SARAH_ID, MICHAEL_ID),
            splitType = SplitType.EQUAL, notes = "Rental car for the group"
        ),
        Expense(
            id = "demo_goa_hotel_deposit", title = "Hotel Deposit", amount = 640.00,
            paidByUserId = CURRENT_USER_ID, groupId = GOA_GROUP_ID, category = Category.RENT,
            dateMillis = currentMonthDay(7, 16), participantIds = listOf(CURRENT_USER_ID, ALEX_ID, SARAH_ID, MICHAEL_ID),
            splitType = SplitType.EXACT,
            customSplits = mapOf(CURRENT_USER_ID to 240.0, ALEX_ID to 160.0, SARAH_ID to 40.0, MICHAEL_ID to 200.0),
            notes = "Exact room allocation deposit"
        ),
        Expense("demo_apartment_wifi", "Monthly Fiber WiFi", 60.0, CURRENT_USER_ID, "demo_group_apartment", Category.UTILITIES, currentMonthDay(8), listOf(CURRENT_USER_ID, ALEX_ID, "u5")),
        Expense("demo_apartment_groceries", "Organic Groceries", 135.0, "u5", "demo_group_apartment", Category.GROCERIES, currentMonthDay(9), listOf(CURRENT_USER_ID, ALEX_ID, "u5")),
        Expense("demo_foodies_pizza", "Italian Pizza", 75.0, SARAH_ID, "demo_group_foodies", Category.FOOD, currentMonthDay(10), listOf(CURRENT_USER_ID, SARAH_ID, "u5")),
        Expense("demo_shared_airport", "Airport Cab", 45.0, CURRENT_USER_ID, null, Category.TRAVEL, currentMonthDay(11), listOf(CURRENT_USER_ID, ALEX_ID)),
        Expense("demo_shared_concert", "Concert Tickets", 200.0, CURRENT_USER_ID, null, Category.ENTERTAINMENT, currentMonthDay(12), listOf(CURRENT_USER_ID, MICHAEL_ID))
    )

    val demoSettlements: List<Settlement> get() = listOf(
        Settlement("demo_goa_alex_settlement", ALEX_ID, CURRENT_USER_ID, 320.0, currentMonthDay(8, 11), GOA_GROUP_ID, PaymentMethod.UPI, "Alex settled Goa expenses"),
        Settlement("demo_goa_michael_settlement", MICHAEL_ID, CURRENT_USER_ID, 20.0, currentMonthDay(9, 11), GOA_GROUP_ID, PaymentMethod.UPI, "Michael partial settlement")
    )

    val demoPersonalExpenses: List<PersonalExpense> get() = listOf(
        PersonalExpense("demo_personal_rent", "Monthly Room Rent", 480.0, Category.RENT, "Personal room rent contribution", recentCurrentMonthTime(80)),
        PersonalExpense("demo_personal_food", "Dinner and Coffee", 235.0, Category.FOOD, "Dinner at a neighbourhood cafe", recentCurrentMonthTime(52)),
        PersonalExpense("demo_personal_entertainment", "Movie and Gaming", 220.0, Category.ENTERTAINMENT, "Movie tickets and gaming pass", recentCurrentMonthTime(30)),
        PersonalExpense("demo_personal_groceries", "Weekly Groceries", 135.0, Category.GROCERIES, "Fruit, milk, bread, and household basics", recentCurrentMonthTime(18)),
        PersonalExpense("demo_personal_utilities", "Electricity Bill", 60.0, Category.UTILITIES, "Personal share of the electricity bill", recentCurrentMonthTime(9)),
        PersonalExpense("demo_personal_travel", "Metro Card Recharge", 45.0, Category.TRAVEL, "Local commute recharge", recentCurrentMonthTime(2))
    )
}
