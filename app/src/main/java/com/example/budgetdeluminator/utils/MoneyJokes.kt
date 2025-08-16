package com.example.budgetdeluminator.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.budgetdeluminator.data.model.CategoryWithExpenses
import kotlin.random.Random

object MoneyJokes {

    private const val PREFS_NAME = "money_jokes_prefs"
    private const val KEY_LAST_JOKE_INDEX = "last_joke_index"
    private const val KEY_APP_LAUNCH_COUNT = "app_launch_count"

    // Generic roasts for when no specific expense data is available
    private val genericSpendingRoasts =
            listOf(
                    "Look who's back to see the damage! 👀💸",
                    "Ready to face the financial music? 🎵💰",
                    "Your wallet called... it's filing a missing persons report 🕵️‍♂️",
                    "Another day, another dollar... gone! 💨💵",
                    "Welcome back, big spender! 🎪💳",
                    "Your bank account is giving you the silent treatment 🤐",
                    "Checking your budget? That's adorable! 😏📊",
                    "Your expenses are throwing a party... without inviting your savings 🎉",
                    "Plot twist: Your money has trust issues now 🎭💔",
                    "Your spending habits are more consistent than your gym routine 🏋️‍♂️💸",
                    "Breaking news: Local person discovers money doesn't grow on apps 📱🌱",
                    "Your future self is sending angry emails from retirement 👴📧",
                    "Congratulations! You're winning at making money disappear 🎩✨",
                    "Your budget is like a diet plan... more of a suggestion 📋🤷‍♂️",
                    "Emergency meeting: Your savings account vs. your shopping cart 🛒⚔️",
                    "Your credit card is getting more exercise than you are 🏃‍♂️💳",
                    "Plot armor: Your ability to find things to buy 🛡️🛍️",
                    "Your money has commitment issues... it never stays long 💍💸",
                    "Achievement unlocked: Professional money relocator! 🏆📦",
                    "Your expenses are more social than you are... they're everywhere! 🎪🌍"
            )

    // Dynamic roast templates that work with any category name
    private val categoryRoastTemplates =
            listOf(
                    "Your {category} budget is having an identity crisis! 💸😵",
                    "Breaking: {category} expenses are not a charity case! 📰💰",
                    "Your {category} spending is more consistent than your sleep schedule! 😴💸",
                    "Plot twist: {category} costs money. Who knew? 🤷‍♂️💡",
                    "Your {category} budget called... it wants a restraining order! 📞⚖️",
                    "Emergency alert: Your {category} expenses have left the chat! 🚨💬",
                    "Your {category} spending is throwing a party... your budget wasn't invited! 🎉💸",
                    "Breaking news: Local {category} budget found crying in corner! 📰😭",
                    "Your {category} expenses are overachievers... your wallet disagrees! 🏆💔",
                    "Achievement unlocked: {category} spending champion! 🏆💸",
                    "Your {category} budget is having trust issues with reality! 🤔💰",
                    "Plot armor: Your ability to find {category} expenses everywhere! 🛡️🔍"
            )

    // Over-budget specific roasts
    private val overBudgetRoasts =
            listOf(
                    "Houston, we have a budget problem! 🚀💸",
                    "Your budget is crying in the corner... comfort it! 😢💰",
                    "Breaking: Math still works. Your expenses > Your budget! 📊😱",
                    "Your budget called... it wants a restraining order! 📞⚖️",
                    "Plot twist: Budgets are meant to be followed, not ignored! 📋😏",
                    "Your expenses are overachievers... your budget is not impressed! 🏆💸",
                    "Emergency alert: Your spending has left the chat! 🚨💬",
                    "Your budget is having an identity crisis... it forgot its purpose! 🤔💰",
                    "Breaking news: Local budget found crying in spreadsheet! 📰😭",
                    "Your expenses are throwing a party... your budget wasn't invited! 🎉💸"
            )

    // Perfect budget roasts (when spending exactly matches budget)
    private val perfectBudgetRoasts =
            listOf(
                    "Wow, you actually followed your budget! Are you feeling okay? 😷📊",
                    "Breaking: Local person discovers budgets work when followed! 📰💡",
                    "Your budget is shocked... it's actually being respected! 😱💰",
                    "Plot twist: Math works when you let it! 🧮✨",
                    "Your budget is sending thank you cards! 💌📊",
                    "Achievement unlocked: Budget whisperer! 🏆💰",
                    "Your expenses and budget are finally on speaking terms! 💬📋",
                    "Breaking: Miracles do happen... you stuck to your budget! ✨💸"
            )

    // Under-budget roasts (positive reinforcement with humor)
    private val underBudgetRoasts =
            listOf(
                    "Look who's being financially responsible... show off! 😏💰",
                    "Your budget is doing a happy dance! 💃📊",
                    "Breaking: Local person discovers self-control exists! 📰🎯",
                    "Your future self is sending thank you notes! 💌👴",
                    "Plot twist: You can actually say no to expenses! 🚫💸",
                    "Your wallet is finally getting some rest! 😴💰",
                    "Achievement unlocked: Budget boss! 🏆📋",
                    "Your savings account is doing cartwheels! 🤸‍♂️💰"
            )

    fun getRandomJoke(context: Context): String {
        val prefs = getPreferences(context)
        val currentLaunchCount = prefs.getInt(KEY_APP_LAUNCH_COUNT, 0)
        val lastJokeIndex = prefs.getInt(KEY_LAST_JOKE_INDEX, -1)

        // Increment launch count
        prefs.edit().putInt(KEY_APP_LAUNCH_COUNT, currentLaunchCount + 1).apply()

        // Get a different roast than the last one shown
        var newJokeIndex: Int
        do {
            newJokeIndex = Random.nextInt(genericSpendingRoasts.size)
        } while (newJokeIndex == lastJokeIndex && genericSpendingRoasts.size > 1)

        // Save the new joke index
        prefs.edit().putInt(KEY_LAST_JOKE_INDEX, newJokeIndex).apply()

        return genericSpendingRoasts[newJokeIndex]
    }

    /** Get an expense-specific roast based on spending patterns */
    fun getExpenseSpecificRoast(
            context: Context,
            categoriesWithExpenses: List<CategoryWithExpenses>,
            totalBudget: Double,
            totalSpent: Double
    ): String {
        val prefs = getPreferences(context)

        // Analyze spending patterns
        val categoryRoastCandidates = mutableListOf<String>()
        val budgetRoastCandidates = mutableListOf<String>()

        // Check for over-budget categories and add category-specific roasts (highest priority)
        categoriesWithExpenses.forEach { categoryWithExpenses ->
            val category = categoryWithExpenses.category
            val spent = categoryWithExpenses.totalSpent

            if (spent > category.budgetLimit) {
                // Generate dynamic category roasts using actual category name
                val categoryRoasts = generateCategoryRoasts(category.name)
                categoryRoastCandidates.addAll(categoryRoasts)
            }
        }

        // Also add roasts for the highest spending categories (even if not over budget)
        val topSpendingCategories =
                categoriesWithExpenses
                        .filter { it.totalSpent > 0 }
                        .sortedByDescending { it.totalSpent }
                        .take(2) // Top 2 spending categories

        topSpendingCategories.forEach { categoryWithExpenses ->
            val categoryRoasts = generateCategoryRoasts(categoryWithExpenses.category.name)
            categoryRoastCandidates.addAll(
                    categoryRoasts.take(3)
            ) // Add a few more to increase chances
        }

        // Check overall budget status (secondary priority)
        when {
            totalSpent > totalBudget -> budgetRoastCandidates.addAll(overBudgetRoasts)
            kotlin.math.abs(totalSpent - totalBudget) < 0.01 ->
                    budgetRoastCandidates.addAll(perfectBudgetRoasts)
            totalSpent < totalBudget * 0.8 -> budgetRoastCandidates.addAll(underBudgetRoasts)
        }

        // Prioritize category-specific roasts: 70% chance for category roasts, 30% for budget
        // roasts
        val roastCandidates = mutableListOf<String>()
        if (categoryRoastCandidates.isNotEmpty()) {
            // Add category roasts multiple times to increase their probability
            roastCandidates.addAll(categoryRoastCandidates)
            roastCandidates.addAll(categoryRoastCandidates) // Add twice for higher priority
            roastCandidates.addAll(budgetRoastCandidates.take(2)) // Add fewer budget roasts
        } else {
            roastCandidates.addAll(budgetRoastCandidates)
        }

        // If no specific roasts found, use generic ones
        if (roastCandidates.isEmpty()) {
            roastCandidates.addAll(genericSpendingRoasts)
        }

        // Get a different roast than the last one shown
        var selectedRoast: String
        do {
            selectedRoast = roastCandidates.random()
        } while (roastCandidates.size > 1 &&
                selectedRoast == prefs.getString("last_expense_roast", null))

        // Save the selected roast
        prefs.edit().putString("last_expense_roast", selectedRoast).apply()

        return selectedRoast
    }

    /** Generate dynamic category-specific roasts using actual category name */
    private fun generateCategoryRoasts(categoryName: String): List<String> {
        return categoryRoastTemplates.map { template ->
            template.replace("{category}", categoryName.lowercase())
        }
    }

    fun getTodaysJoke(context: Context): String {
        val prefs = getPreferences(context)
        val lastJokeIndex = prefs.getInt(KEY_LAST_JOKE_INDEX, 0)

        // If no roast has been set yet, get a random one
        return if (lastJokeIndex >= 0 && lastJokeIndex < genericSpendingRoasts.size) {
            genericSpendingRoasts[lastJokeIndex]
        } else {
            getRandomJoke(context)
        }
    }

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
