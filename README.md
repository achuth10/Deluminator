# Budget Deluminator

An Android budget tracking app. Track expenses, manage categories, and set up recurring expenses.

## Features

### Expense Management

- Add, edit, and delete expenses
- Categorize expenses with budget limits
- Built-in calculator for amount entry
- Date picker with Today/Yesterday shortcuts
- View expenses by category or chronologically

### Recurring Expenses

- Set up daily, weekly, or monthly recurring expenses
- Schedule specific times (daily), days of week (weekly), or dates (monthly)
- Automatic expense generation in background
- Manual sync when background processing unavailable

### Customization

- Light and dark themes
- Multiple currency support with formatting
- Color-coded categories
- 8 default categories included

### Data Storage

- SQLite database for offline use
- Input validation and error handling
- Persistent data storage

## Technical Details

### Built With

- Kotlin
- Android View System with ViewBinding
- Room (SQLite)
- MVVM with Repository pattern
- WorkManager for background processing
- Material Design Components

### Requirements

- Android 8.0+ (API 26)
- Android Studio Arctic Fox or later
- Kotlin 2.0.21+
- Gradle 8.12.0+

## Default Categories

The app includes 8 preconfigured categories:

- Food & Dining ($500 budget)
- Transportation ($300 budget)
- Shopping ($400 budget)
- Entertainment ($200 budget)
- Bills & Utilities ($800 budget)
- Healthcare ($300 budget)
- Personal Care ($150 budget)
- Education ($250 budget)

## Installation

1. Clone the repository

   ```bash
   git clone https://github.com/yourusername/budget-deluminator.git
   cd budget-deluminator
   ```

2. Open in Android Studio

3. Build and run
   ```bash
   ./gradlew assembleDebug
   ```

## Configuration

### Currency Settings

- Default: INR (Indian Rupee)
- Access via Settings (long-press the FAB button)
- Search and filter available currencies

### Theme Settings

- Light and Dark theme options
- Access via Settings screen

### Background Processing

- Automatic recurring expense processing
- Permission request only after creating recurring expenses
- Manual sync fallback for restricted devices

## Database Schema

### Budget Categories

```kotlin
data class BudgetCategory(
    val id: Long,
    val name: String,
    val budgetLimit: Double,
    val color: String,
    val createdAt: Long
)
```

### Expenses

```kotlin
data class Expense(
    val id: Long,
    val categoryId: Long,
    val amount: Double,
    val description: String,
    val createdAt: Long
)
```

### Recurring Expenses

```kotlin
data class RecurringExpense(
    val id: Long,
    val categoryId: Long,
    val amount: Double,
    val description: String,
    val recurrenceType: RecurrenceType,
    val recurrenceValue: Int,
    val isActive: Boolean,
    val lastGeneratedAt: Long,
    val createdAt: Long
)
```

## Input Limits

- Expense Amount: $0.01 - $999,999.99
- Budget Limit: Up to $999,999.99
- Description: 2-200 characters
- Category Name: 2-50 characters

## Calculator Features

- Basic operations: +, -, ×, ÷
- Up to 15 digits with 10 decimal places
- Division by zero protection
- Overflow detection

## Development

### Testing

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

### Linting

```bash
./gradlew lint
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes
4. Test thoroughly
5. Submit a pull request

## License

MIT License
