package com.example.budgetdeluminator.data.model

data class Currency(val code: String, val name: String, val symbol: String, val country: String)

object CurrencyManager {
    val currencies =
            listOf(
                    // Major Currencies
                    Currency("USD", "US Dollar", "$", "United States"),
                    Currency("EUR", "Euro", "€", "Eurozone"),
                    Currency("GBP", "British Pound", "£", "United Kingdom"),
                    Currency("JPY", "Japanese Yen", "¥", "Japan"),
                    Currency("CHF", "Swiss Franc", "CHF", "Switzerland"),
                    Currency("CAD", "Canadian Dollar", "C$", "Canada"),
                    Currency("AUD", "Australian Dollar", "A$", "Australia"),
                    Currency("NZD", "New Zealand Dollar", "NZ$", "New Zealand"),

                    // Asian Currencies
                    Currency("INR", "Indian Rupee", "₹", "India"),
                    Currency("CNY", "Chinese Yuan", "¥", "China"),
                    Currency("KRW", "South Korean Won", "₩", "South Korea"),
                    Currency("SGD", "Singapore Dollar", "S$", "Singapore"),
                    Currency("HKD", "Hong Kong Dollar", "HK$", "Hong Kong"),
                    Currency("THB", "Thai Baht", "฿", "Thailand"),
                    Currency("MYR", "Malaysian Ringgit", "RM", "Malaysia"),
                    Currency("IDR", "Indonesian Rupiah", "Rp", "Indonesia"),
                    Currency("PHP", "Philippine Peso", "₱", "Philippines"),
                    Currency("VND", "Vietnamese Dong", "₫", "Vietnam"),
                    Currency("PKR", "Pakistani Rupee", "Rs", "Pakistan"),
                    Currency("BDT", "Bangladeshi Taka", "৳", "Bangladesh"),
                    Currency("LKR", "Sri Lankan Rupee", "Rs", "Sri Lanka"),
                    Currency("NPR", "Nepalese Rupee", "Rs", "Nepal"),
                    Currency("MMK", "Myanmar Kyat", "K", "Myanmar"),
                    Currency("KHR", "Cambodian Riel", "៛", "Cambodia"),
                    Currency("LAK", "Lao Kip", "₭", "Laos"),

                    // Middle East & Africa
                    Currency("AED", "UAE Dirham", "د.إ", "UAE"),
                    Currency("SAR", "Saudi Riyal", "ر.س", "Saudi Arabia"),
                    Currency("QAR", "Qatari Riyal", "ر.ق", "Qatar"),
                    Currency("KWD", "Kuwaiti Dinar", "د.ك", "Kuwait"),
                    Currency("BHD", "Bahraini Dinar", "ب.د", "Bahrain"),
                    Currency("OMR", "Omani Rial", "ر.ع.", "Oman"),
                    Currency("JOD", "Jordanian Dinar", "د.ا", "Jordan"),
                    Currency("LBP", "Lebanese Pound", "ل.ل", "Lebanon"),
                    Currency("EGP", "Egyptian Pound", "ج.م", "Egypt"),
                    Currency("ZAR", "South African Rand", "R", "South Africa"),
                    Currency("NGN", "Nigerian Naira", "₦", "Nigeria"),
                    Currency("KES", "Kenyan Shilling", "KSh", "Kenya"),
                    Currency("GHS", "Ghanaian Cedi", "₵", "Ghana"),
                    Currency("MAD", "Moroccan Dirham", "د.م.", "Morocco"),
                    Currency("TND", "Tunisian Dinar", "د.ت", "Tunisia"),
                    Currency("ETB", "Ethiopian Birr", "Br", "Ethiopia"),

                    // European Currencies (Non-Euro)
                    Currency("NOK", "Norwegian Krone", "kr", "Norway"),
                    Currency("SEK", "Swedish Krona", "kr", "Sweden"),
                    Currency("DKK", "Danish Krone", "kr", "Denmark"),
                    Currency("PLN", "Polish Zloty", "zł", "Poland"),
                    Currency("CZK", "Czech Koruna", "Kč", "Czech Republic"),
                    Currency("HUF", "Hungarian Forint", "Ft", "Hungary"),
                    Currency("RON", "Romanian Leu", "lei", "Romania"),
                    Currency("BGN", "Bulgarian Lev", "лв", "Bulgaria"),
                    Currency("HRK", "Croatian Kuna", "kn", "Croatia"),
                    Currency("RSD", "Serbian Dinar", "дин", "Serbia"),
                    Currency("RUB", "Russian Ruble", "₽", "Russia"),
                    Currency("UAH", "Ukrainian Hryvnia", "₴", "Ukraine"),
                    Currency("TRY", "Turkish Lira", "₺", "Turkey"),

                    // Latin America
                    Currency("BRL", "Brazilian Real", "R$", "Brazil"),
                    Currency("MXN", "Mexican Peso", "$", "Mexico"),
                    Currency("ARS", "Argentine Peso", "$", "Argentina"),
                    Currency("CLP", "Chilean Peso", "$", "Chile"),
                    Currency("COP", "Colombian Peso", "$", "Colombia"),
                    Currency("PEN", "Peruvian Sol", "S/", "Peru"),
                    Currency("VES", "Venezuelan Bolívar", "Bs", "Venezuela"),
                    Currency("UYU", "Uruguayan Peso", "\$U", "Uruguay"),
                    Currency("BOB", "Bolivian Boliviano", "Bs", "Bolivia"),
                    Currency("PYG", "Paraguayan Guarani", "₲", "Paraguay"),

                    // Others
                    Currency("ILS", "Israeli Shekel", "₪", "Israel"),
                    Currency("TWD", "Taiwan Dollar", "NT$", "Taiwan"),
                    Currency("KZT", "Kazakhstani Tenge", "₸", "Kazakhstan"),
                    Currency("UZS", "Uzbekistani Som", "soʻm", "Uzbekistan"),
                    Currency("BYN", "Belarusian Ruble", "Br", "Belarus"),
                    Currency("GEL", "Georgian Lari", "₾", "Georgia"),
                    Currency("AMD", "Armenian Dram", "֏", "Armenia"),
                    Currency("AZN", "Azerbaijani Manat", "₼", "Azerbaijan"),
                    Currency("AFN", "Afghan Afghani", "؋", "Afghanistan"),
                    Currency("IRR", "Iranian Rial", "﷼", "Iran"),
                    Currency("IQD", "Iraqi Dinar", "ع.د", "Iraq"),
            )

    fun getCurrencyByCode(code: String): Currency? {
        return currencies.find { it.code == code }
    }

    fun getDefaultCurrency(): Currency {
        return getCurrencyByCode("INR") ?: currencies.first()
    }
}
