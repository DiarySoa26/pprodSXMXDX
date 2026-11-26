package com.sxmxdx.backend.controller.budget;

import com.sxmxdx.backend.service.budget.BudgetService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping("/generer")
    public Map<?, ?> genererBudget() {
        return budgetService.genererBudget();
    }
}
