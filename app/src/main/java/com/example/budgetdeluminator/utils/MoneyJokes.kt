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
                    "Your expenses are more social than you are... they're everywhere! 🎪🌍",
                    "Your money has commitment issues - it never sticks around 💸",
                    "Congrats on turning your bank account into modern art: minimalist 🎨",
                    "Your spending habits are more consistent than your Wi-Fi 📶💳",
                    "Fun fact: Money doesn't grow on credit cards either 💳🌱",
                    "Your budget is more flexible than a yoga instructor 📊",
                    "Breaking: Local person discovers ATMs require actual money 🏧",
                    "Your expenses multiply faster than rabbits in spring 📈🐰",
                    "Your financial discipline went out for cigarettes and never came back 🚬",
                    "You've mastered the art of making money disappear without magic 🎩",
                    "Your accounting skills would make Enron jealous 📊",
                    "Your money management is like a horror movie - everything vanishes 🎬💸",
                    "Your savings account is playing hard to get... because it's empty 💰",
                    "Your budget exists in a parallel universe where math doesn't work 🤔📋",
                    "You could sell masterclasses on creative expense justification 🎓💸",
                    "Your wallet is lighter than your commitment to saving 👛",
                    "Your budget is having trust issues with reality 📰📋",
                    "Your expenses travel in packs like wolves 🐺💸",
                    "You've turned window shopping into contact sport 🪟💳",
                    "Your money is more transient than a summer romance 💰💔",
                    "Your financial goals are more elusive than Bigfoot 🦶🎯",
                    "Your spending patterns are easier to predict than the weather 🌦️💳",
                    "Reminder: Budgets work better when you actually follow them 📋",
                    "Your credit card gets more action than a Hollywood stunt double 💳🎬",
                    "Today's revelation: Math still applies to personal finances 🧮",
                    "Your wallet died doing what it loved: being empty 💀💰",
                    "You speak fluent 'expense' - every purchase makes sense to you 💸",
                    "Your budget has the lifespan of a mayfly 🦟📋"
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
                    "Plot armor: Your ability to find {category} expenses everywhere! 🛡️🔍",
                    "Your {category} spending has commitment issues - to your budget 💸",
                    "Your {category} expenses are training for the overspending Olympics 🏃‍♂️💳",
                    "Your {category} budget is more abstract than modern art 🎭📊",
                    "Your {category} expenses have taken your wallet hostage 💰🔫",
                    "Your {category} spending has a PhD in budget destruction 🎓💸",
                    "Your {category} expenses are everywhere - like glitter after a craft project 💳✨",
                    "Your {category} budget is filing a restraining order against reality ⚖️📋",
                    "Your {category} spending defies the laws of financial physics 🌍💸",
                    "You're fluent in {category} expense - every purchase makes perfect sense 👂💸",
                    "Your {category} budget is more flexible than a contortionist 📊🤸‍♀️",
                    "Your {category} expenses multiply faster than your excuses 🐰💸",
                    "Your {category} spending doesn't believe in the concept of 'enough' 🚫💸",
                    "Your {category} budget has left the building - Elvis style 🏃‍♂️📋",
                    "Your {category} expenses are collectibles - gotta spend on 'em all ⚡💸",
                    "Your {category} spending justification skills are Olympic-level 💭🏆",
                    "Your {category} budget is vacationing in the danger zone ⚠️📮",
                    "Your {category} expenses vs. common sense: expenses are winning 🧠💸",
                    "Your {category} spending is more reliable than your alarm clock 🌅💸",
                    "Your {category} budget has declared independence from math 🗽📊",
                    "Your {category} expenses cling to your money like static electricity 💔💰",
                    "You're a {category} spending ninja - silent but deadly to budgets 🥷💸",
                    "Your {category} budget is like a unicorn - mythical and rarely seen 👻📊",
                    "Your {category} expenses are renewable - they keep regenerating ♻️💸"
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
                    "Your expenses are throwing a party... your budget wasn't invited! 🎉💸",
                    "Mission failed: Your budget has been thoroughly demolished 🔴💥",
                    "Your budget is in witness protection from your spending habits 🕵️‍♂️📋",
                    "Newsflash: Budgets aren't just decorative spreadsheet art 📰💔",
                    "Budget overload detected - system failure imminent 📻⚠️",
                    "Your expenses ignore limits like teenagers ignore curfew 👂🚫",
                    "Your budget is updating its resume - clearly this job isn't working out 🎭💼",
                    "Congratulations: You've achieved expert-level budget destruction 🏆💥",
                    "Your budget is sending distress signals from deep in the red 📡🔴",
                    "Your budget and reality are no longer on speaking terms 📰💔",
                    "Your expenses have never met a limit they respected 🤔💸",
                    "Budget integrity has left the chat 🚨🔧",
                    "Your budget is like a speed limit in Italy - purely theoretical 🚗💨",
                    "You've turned budget-breaking into an art form 🛡️📈",
                    "Your budget is limbo dancing with zero 🕺📊",
                    "Your budget is playing hide and seek - and losing 📰🛏️",
                    "Your expenses have gravitational pull on your bank account 🌍⬇️",
                    "Budget vs. spending habits: spending wins by knockout 🚨⚔️",
                    "Your budget needs therapy for abandonment issues 🛋️💰",
                    "PSA: Budgets work better when acknowledged 📋✋",
                    "Your expenses are Olympic-level overachievers 🏃‍♂️🏆",
                    "Your budget has filed for financial bankruptcy 📰💸",
                    "Your budget has the staying power of a New Year's resolution 🍰📋",
                    "Financial discipline has officially clocked out 🚨🏃‍♂️",
                    "Your budget is sending postcards from rock bottom 📮⚠️"
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
                    "Breaking: Miracles do happen... you stuck to your budget! ✨💸",
                    "Well, look who discovered basic math 💃📊",
                    "Alert: Actual budget compliance detected 🚨✅",
                    "This moment deserves a commemorative plaque 🖼️📋",
                    "Shocking development: You can actually add and subtract 🤯🧮",
                    "Your budget and reality are finally speaking the same language 📰🤝",
                    "Your budget is writing a memoir: 'The Day Math Worked' 📚💰",
                    "Rare achievement: Mathematical accuracy in personal finance 🏆🎯",
                    "Who are you and what did you do with the overspender? 👽💸",
                    "Confirmed: Financial discipline still exists 📻🔍",
                    "Your budget is considering a career change - this actually worked 📋🎤",
                    "You've discovered the ancient art of numerical limits 🛡️📊",
                    "Your budget is throwing a very small, very confused party 📰🎉",
                    "Your budget is sending a thank-you card to basic arithmetic 🌸💰",
                    "Mission accomplished: You followed simple instructions ✅🎯",
                    "Your budget: 'Finally, someone who gets it' 📋💬",
                    "Historic peace treaty signed between budget and reality 🚨🕊️",
                    "Your budget is updating its status: 'Actually functional' 💼📋",
                    "Plot twist: Budgets work when you use them correctly 📋❤️",
                    "Rare sighting: Mathematical precision in personal finance 📰🎯",
                    "Your budget is nominating you for 'Competent Adult' award 🏆📋",
                    "Achievement: You can follow basic financial guidelines 🏆👑",
                    "Your expenses: 'Wait, we're supposed to have limits?' 💸🤔"
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
                    "Your savings account is doing cartwheels! 🤸‍♂️💰",
                    "Your budget is sending a formal thank-you letter 💕💰",
                    "Alert: Signs of financial maturity detected 🚨🧠",
                    "Your expenses: 'We don't have to spend everything? Mind blown.' 💸🤔",
                    "Your budget is doing a modest victory lap 📰🏃‍♂️",
                    "Your wallet is grateful for your newfound restraint 💌💪",
                    "Turns out self-control is actually useful - who knew? 🦸‍♂️💰",
                    "Your budget is considering a career in success stories 📋🎯",
                    "You've achieved basic financial competence 🏆🥋",
                    "Your savings account is cautiously optimistic 🎉💰",
                    "Rare sighting: Responsible spending in its natural habitat 📻🔍",
                    "Your budget is updating its status: 'Actually working' 📋💼",
                    "Congratulations on discovering the word 'enough' 📰✋",
                    "Your future self is sending a polite thank-you note 👴🎊",
                    "You've developed immunity to impulse buying 🛡️🛍️",
                    "Your budget: 'This is what I was designed for' 📋💡",
                    "Your expenses have learned the concept of 'limits' 🚨📚",
                    "Your wallet is reconsidering its opinion of you 💰🤝",
                    "Shocking: Math works when you don't ignore it 📰🧮",
                    "Your budget is acknowledging your common sense 🌸🧠",
                    "You've achieved financial zen - or at least financial calm 🏆🧘‍♂️",
                    "Your expenses: 'Stopping before the limit? Revolutionary concept.' 💸🤯",
                    "Apparently budgets respond well to being followed 📋❤️",
                    "Your savings account is composing a thank-you haiku 💰📝",
                    "Level up: Basic financial responsibility unlocked 🚨⬆️",
                    "Your budget is nominating you for 'Functional Adult' status 📋🏆",
                    "Evidence suggests self-control isn't completely extinct 📰🦕",
                    "Your retirement fund is cautiously optimistic 👴🤸‍♂️"
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
