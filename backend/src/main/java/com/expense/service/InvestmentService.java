package com.expense.service;

import com.expense.entity.Investment;
import com.expense.entity.User;
import com.expense.repository.InvestmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvestmentService {

    private final InvestmentRepository investmentRepository;

    public List<Investment> getUserInvestments(User user) {
        try {
            return investmentRepository.findByUserOrderByPurchaseDateDesc(user);
        } catch (Exception e) {
            log.error("Error fetching user investments", e);
            return List.of();
        }
    }

    public Map<String, Object> getPortfolioSummary(User user) {
        try {
            List<Investment> investments = getUserInvestments(user);
            
            BigDecimal totalInvested = investments.stream()
                    .map(Investment::getInvestedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalCurrentValue = investments.stream()
                    .map(Investment::getCurrentValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalProfitLoss = totalCurrentValue.subtract(totalInvested);
            double profitLossPercentage = totalInvested.compareTo(BigDecimal.ZERO) > 0 ? 
                    totalProfitLoss.divide(totalInvested, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100 : 0;
            
            // Get top performing investments
            List<Map<String, Object>> topPerformers = getTopPerformingInvestments(investments);
            
            return Map.of(
                "totalInvested", totalInvested,
                "totalCurrentValue", totalCurrentValue,
                "totalProfitLoss", totalProfitLoss,
                "profitLossPercentage", profitLossPercentage,
                "investmentCount", investments.size(),
                "topPerformers", topPerformers,
                "lastUpdated", LocalDateTime.now()
            );
        } catch (Exception e) {
            log.error("Error calculating portfolio summary", e);
            return Map.of("error", "Failed to calculate portfolio summary");
        }
    }

    public Investment createInvestment(Investment investment) {
        try {
            investment.setCreatedAt(LocalDateTime.now());
            investment.setUpdatedAt(LocalDateTime.now());
            return investmentRepository.save(investment);
        } catch (Exception e) {
            log.error("Error creating investment", e);
            throw new RuntimeException("Failed to create investment: " + e.getMessage(), e);
        }
    }

    public Investment updateInvestment(Investment investment) {
        try {
            investment.setUpdatedAt(LocalDateTime.now());
            return investmentRepository.save(investment);
        } catch (Exception e) {
            log.error("Error updating investment", e);
            throw new RuntimeException("Failed to update investment: " + e.getMessage(), e);
        }
    }

    public Investment getInvestmentById(Long id) {
        try {
            return investmentRepository.findById(id).orElse(null);
        } catch (Exception e) {
            log.error("Error fetching investment by id", e);
            return null;
        }
    }

    public void deleteInvestment(Long id) {
        try {
            investmentRepository.deleteById(id);
        } catch (Exception e) {
            log.error("Error deleting investment", e);
            throw new RuntimeException("Failed to delete investment: " + e.getMessage(), e);
        }
    }

    public Investment updateInvestmentValue(Long id, BigDecimal newCurrentValue) {
        try {
            Optional<Investment> investmentOpt = investmentRepository.findById(id);
            if (investmentOpt.isEmpty()) {
                throw new RuntimeException("Investment not found");
            }
            
            Investment investment = investmentOpt.get();
            investment.setCurrentValue(newCurrentValue);
            investment.setUpdatedAt(LocalDateTime.now());
            
            return investmentRepository.save(investment);
        } catch (Exception e) {
            log.error("Error updating investment value", e);
            throw new RuntimeException("Failed to update investment value: " + e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> getTopPerformingInvestments(List<Investment> investments) {
        return investments.stream()
                .sorted((a, b) -> b.getCurrentValue().subtract(a.getInvestedAmount())
                        .compareTo(a.getCurrentValue().subtract(a.getInvestedAmount())) < 0 ? 1 : -1)
                .limit(3)
                .map(inv -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", inv.getName());
                    map.put("profitLoss", inv.getCurrentValue().subtract(inv.getInvestedAmount()));
                    map.put("profitLossPercentage", inv.getInvestedAmount().compareTo(BigDecimal.ZERO) > 0 ? 
                            inv.getCurrentValue().subtract(inv.getInvestedAmount())
                                    .divide(inv.getInvestedAmount(), 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100 : 0);
                    return map;
                })
                .collect(Collectors.toList());
    }
}
