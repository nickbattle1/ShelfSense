package com.example.shelfsense.data.local

import com.example.shelfsense.data.model.FoodCategory
import com.example.shelfsense.data.model.Outcome

// result shapes for the GROUP BY queries behind Insights and the Home impact card
data class OutcomeCount(val outcome: Outcome, val count: Int)

data class MonthlyOutcomeCount(val month: String, val outcome: Outcome, val count: Int)

data class CategoryCount(val category: FoodCategory, val count: Int)
