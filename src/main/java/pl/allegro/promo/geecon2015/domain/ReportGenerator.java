package pl.allegro.promo.geecon2015.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import pl.allegro.promo.geecon2015.domain.stats.FinancialStatisticsRepository;
import pl.allegro.promo.geecon2015.domain.stats.FinancialStats;
import pl.allegro.promo.geecon2015.domain.transaction.TransactionRepository;
import pl.allegro.promo.geecon2015.domain.transaction.UserTransaction;
import pl.allegro.promo.geecon2015.domain.transaction.UserTransactions;
import pl.allegro.promo.geecon2015.domain.user.User;
import pl.allegro.promo.geecon2015.domain.user.UserRepository;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class ReportGenerator {
    
    private final FinancialStatisticsRepository financialStatisticsRepository;
    
    private final UserRepository userRepository;
    
    private final TransactionRepository transactionRepository;

    @Autowired
    public ReportGenerator(FinancialStatisticsRepository financialStatisticsRepository,
                           UserRepository userRepository,
                           TransactionRepository transactionRepository) {
        this.financialStatisticsRepository = financialStatisticsRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    public Report generate(ReportRequest request) {
        Report report = new Report();

        FinancialStats financialStats = financialStatisticsRepository
                .listUsersWithMinimalIncome(request.getMinimalIncome(), request.getUsersToCheck());
        for(UUID uuid : financialStats.getUserIds()) {
            User user = getUserOrElseFallback(uuid);
            BigDecimal userTransactionSum = getUserTransactionSumOrElseFallback(uuid);
            report.add(new ReportedUser(uuid, user.getName(), userTransactionSum));
        }
        return report;
    }

    private BigDecimal getUserTransactionSumOrElseFallback(UUID uuid) {
        try {
            return transactionRepository.transactionsOf(uuid).getTransactions().stream()
                    .map(UserTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        }catch (RestClientException ex) {
            return null;
        }
    }

    private User getUserOrElseFallback(UUID uuid) {
        try {
            return userRepository.detailsOf(uuid);
        } catch (RestClientException ex) {
            return new User(uuid, "<failed>");
        }
    }
}
