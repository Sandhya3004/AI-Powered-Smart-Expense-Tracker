package com.expense;

import com.expense.entity.BillReminder;
import com.expense.entity.Expense;
import com.expense.entity.Group;
import com.expense.entity.GroupMember;
import com.expense.entity.User;
import com.expense.repository.BillReminderRepository;
import com.expense.repository.ExpenseRepository;
import com.expense.repository.GroupMemberRepository;
import com.expense.repository.GroupRepository;
import com.expense.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootApplication
@EnableJpaAuditing
@Slf4j
public class SmartExpenseApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartExpenseApplication.class, args);
    }

    @Bean
    @Transactional
    CommandLineRunner initData(UserRepository userRepository,
                                ExpenseRepository expenseRepository,
                                BillReminderRepository billReminderRepository,
                                GroupRepository groupRepository,
                                GroupMemberRepository groupMemberRepository,
                                PasswordEncoder passwordEncoder) {
        return args -> {
            // Create demo user if not exists
            User demoUser;
            if (!userRepository.existsByEmailIgnoreCase("demo@expense.com")) {
                demoUser = User.builder()
                        .email("demo@expense.com")
                        .password(passwordEncoder.encode("demo123"))
                        .name("Demo User")
                        .role("USER")
                        .currency("INR")
                        .theme("light")
                        .monthlyBudget(BigDecimal.valueOf(50000))
                        .phone("+91-9876543210")
                        .notificationsEnabled(true)
                        .build();
                demoUser = userRepository.save(demoUser);
                log.info("Demo user created: {}", demoUser.getEmail());

                // Create sample expenses for demo user
                createSampleExpenses(expenseRepository, demoUser);

                // Create sample bills for demo user
                createSampleBills(billReminderRepository, demoUser);

                // Create sample groups for demo user
                createSampleGroups(groupRepository, groupMemberRepository, demoUser, userRepository, passwordEncoder);
            } else {
                demoUser = userRepository.findByEmail("demo@expense.com").orElseThrow();
            }

            // Create admin user if not exists
            if (!userRepository.existsByEmailIgnoreCase("admin@expense.com")) {
                User admin = User.builder()
                        .email("admin@expense.com")
                        .password(passwordEncoder.encode("admin123"))
                        .name("Admin User")
                        .role("ADMIN")
                        .currency("INR")
                        .theme("dark")
                        .build();
                userRepository.save(admin);
                log.info("Admin user created");
            }

            log.info("Application started successfully with sample data!");
            log.info("Demo login: demo@expense.com / demo123");
            log.info("Admin login: admin@expense.com / admin123");
        };
    }

    private void createSampleExpenses(ExpenseRepository expenseRepository, User user) {
        List<Expense> expenses = List.of(
            Expense.builder()
                .user(user)
                .description("Grocery Shopping")
                .amount(BigDecimal.valueOf(2500.50))
                .type("EXPENSE")
                .category("Food")
                .merchant("Big Bazaar")
                .paymentType("Credit Card")
                .account("HDFC Credit")
                .currency("INR")
                .expenseDate(LocalDate.now().minusDays(2))
                .source("manual")
                .status("COMPLETED")
                .tags(List.of("groceries", "monthly"))
                .build(),
            Expense.builder()
                .user(user)
                .description("Monthly Rent")
                .amount(BigDecimal.valueOf(15000.00))
                .type("EXPENSE")
                .category("Housing")
                .merchant("Landlord")
                .paymentType("Bank Transfer")
                .account("HDFC Savings")
                .currency("INR")
                .expenseDate(LocalDate.now().minusDays(5))
                .source("manual")
                .status("COMPLETED")
                .tags(List.of("rent", "monthly"))
                .build(),
            Expense.builder()
                .user(user)
                .description("Internet Bill")
                .amount(BigDecimal.valueOf(999.00))
                .type("EXPENSE")
                .category("Utilities")
                .merchant("Airtel")
                .paymentType("Auto-debit")
                .account("HDFC Savings")
                .currency("INR")
                .expenseDate(LocalDate.now().minusDays(3))
                .source("manual")
                .status("COMPLETED")
                .tags(List.of("utilities", "monthly"))
                .build(),
            Expense.builder()
                .user(user)
                .description("Salary")
                .amount(BigDecimal.valueOf(75000.00))
                .type("INCOME")
                .category("Salary")
                .merchant("Employer Inc")
                .paymentType("Bank Transfer")
                .account("HDFC Savings")
                .currency("INR")
                .expenseDate(LocalDate.now().minusDays(10))
                .source("manual")
                .status("COMPLETED")
                .tags(List.of("salary", "monthly"))
                .build(),
            Expense.builder()
                .user(user)
                .description("Movie Night")
                .amount(BigDecimal.valueOf(800.00))
                .type("EXPENSE")
                .category("Entertainment")
                .merchant("PVR Cinemas")
                .paymentType("UPI")
                .account("PhonePe")
                .currency("INR")
                .expenseDate(LocalDate.now().minusDays(1))
                .source("manual")
                .status("COMPLETED")
                .tags(List.of("entertainment", "weekend"))
                .build(),
            Expense.builder()
                .user(user)
                .description("Petrol")
                .amount(BigDecimal.valueOf(2000.00))
                .type("EXPENSE")
                .category("Transportation")
                .merchant("Indian Oil")
                .paymentType("Credit Card")
                .account("HDFC Credit")
                .currency("INR")
                .expenseDate(LocalDate.now().minusDays(4))
                .source("manual")
                .status("COMPLETED")
                .tags(List.of("fuel", "vehicle"))
                .build(),
            Expense.builder()
                .user(user)
                .description("Gym Membership")
                .amount(BigDecimal.valueOf(1500.00))
                .type("EXPENSE")
                .category("Health")
                .merchant("Gold's Gym")
                .paymentType("Credit Card")
                .account("HDFC Credit")
                .currency("INR")
                .expenseDate(LocalDate.now().minusDays(7))
                .source("manual")
                .status("COMPLETED")
                .isRecurring(true)
                .recurrencePattern("MONTHLY")
                .tags(List.of("health", "fitness", "monthly"))
                .build(),
            Expense.builder()
                .user(user)
                .description("Freelance Work")
                .amount(BigDecimal.valueOf(15000.00))
                .type("INCOME")
                .category("Freelance")
                .merchant("Client XYZ")
                .paymentType("Bank Transfer")
                .account("HDFC Savings")
                .currency("INR")
                .expenseDate(LocalDate.now().minusDays(15))
                .source("manual")
                .status("COMPLETED")
                .tags(List.of("freelance", "project"))
                .build()
        );
        expenseRepository.saveAll(expenses);
        log.info("Created {} sample expenses", expenses.size());
    }

    private void createSampleBills(BillReminderRepository billRepository, User user) {
        List<BillReminder> bills = List.of(
            BillReminder.builder()
                .user(user)
                .billName("Electricity Bill")
                .amount(BigDecimal.valueOf(2500.00))
                .dueDate(LocalDate.now().plusDays(5))
                .category("Utilities")
                .paymentMethod("Auto-debit")
                .recurrencePattern("MONTHLY")
                .isPaid(false)
                .reminderDays(List.of(3, 1))
                .build(),
            BillReminder.builder()
                .user(user)
                .billName("Mobile Bill")
                .amount(BigDecimal.valueOf(699.00))
                .dueDate(LocalDate.now().plusDays(3))
                .category("Utilities")
                .paymentMethod("UPI")
                .recurrencePattern("MONTHLY")
                .isPaid(false)
                .reminderDays(List.of(1))
                .build(),
            BillReminder.builder()
                .user(user)
                .billName("Netflix Subscription")
                .amount(BigDecimal.valueOf(649.00))
                .dueDate(LocalDate.now().plusDays(10))
                .category("Entertainment")
                .paymentMethod("Credit Card")
                .recurrencePattern("MONTHLY")
                .isPaid(true)
                .paidDate(LocalDate.now().minusDays(2))
                .reminderDays(List.of(1))
                .build(),
            BillReminder.builder()
                .user(user)
                .billName("Insurance Premium")
                .amount(BigDecimal.valueOf(5000.00))
                .dueDate(LocalDate.now().plusDays(20))
                .category("Insurance")
                .paymentMethod("Bank Transfer")
                .recurrencePattern("YEARLY")
                .isPaid(false)
                .reminderDays(List.of(7, 3, 1))
                .build()
        );
        billRepository.saveAll(bills);
        log.info("Created {} sample bills", bills.size());
    }

    private void createSampleGroups(GroupRepository groupRepository, GroupMemberRepository groupMemberRepository,
                                     User creator, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        // Create another user for the group
        User member2;
        if (!userRepository.existsByEmailIgnoreCase("friend@expense.com")) {
            member2 = User.builder()
                    .email("friend@expense.com")
                    .password(passwordEncoder.encode("friend123"))
                    .name("Friend User")
                    .role("USER")
                    .build();
            member2 = userRepository.save(member2);
        } else {
            member2 = userRepository.findByEmail("friend@expense.com").orElseThrow();
        }

        Group tripGroup = Group.builder()
                .name("Goa Trip 2024")
                .description("Weekend trip to Goa with friends")
                .type("trip")
                .createdBy(creator)
                .build();
        tripGroup = groupRepository.save(tripGroup);

        // Add members
        GroupMember creatorMember = GroupMember.builder()
                .group(tripGroup)
                .user(creator)
                .role("admin")
                .build();
        groupMemberRepository.save(creatorMember);

        GroupMember member2Member = GroupMember.builder()
                .group(tripGroup)
                .user(member2)
                .role("member")
                .build();
        groupMemberRepository.save(member2Member);

        // Create another group
        Group familyGroup = Group.builder()
                .name("Family Expenses")
                .description("Shared family expenses tracking")
                .type("family")
                .createdBy(creator)
                .build();
        familyGroup = groupRepository.save(familyGroup);

        GroupMember familyCreator = GroupMember.builder()
                .group(familyGroup)
                .user(creator)
                .role("admin")
                .build();
        groupMemberRepository.save(familyCreator);

        log.info("Created {} sample groups", 2);
    }
}