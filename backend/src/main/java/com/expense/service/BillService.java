package com.expense.service;

import com.expense.entity.Bill;
import com.expense.entity.User;
import com.expense.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillService {

    private final BillRepository billRepository;

    public List<Bill> getUserBills(User user) {
        return billRepository.findByUserOrderByDueDateAsc(user);
    }

    public List<Bill> getUpcomingBills(User user, int daysAhead) {
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(daysAhead);
        return billRepository.findByUserAndDueDateBetweenOrderByDueDateAsc(user, today, futureDate);
    }

    public Map<String, Object> getBillsSummary(User user) {
        List<Bill> unpaidBills = billRepository.findByUserAndIsPaidFalse(user);
        long overdueCount = billRepository.countOverdueBills(user.getId(), LocalDate.now());
        BigDecimal totalUnpaidRecurring = billRepository.sumUnpaidRecurringBills(user.getId());

        return Map.of(
            "totalBills", getUserBills(user).size(),
            "unpaidBills", unpaidBills.size(),
            "overdueBills", overdueCount,
            "totalUnpaidRecurring", totalUnpaidRecurring
        );
    }

    public Bill createBill(Bill bill) {
        return billRepository.save(bill);
    }

    public Bill updateBill(Bill bill) {
        return billRepository.save(bill);
    }

    public void deleteBill(Long id) {
        billRepository.deleteById(id);
    }

    public Bill markBillAsPaid(Long id) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found"));
        bill.setPaid(true);
        bill.setPaidDate(LocalDate.now());
        return billRepository.save(bill);
    }

    public Bill getBillById(Long id) {
        return billRepository.findById(id).orElse(null);
    }
}
