package com.example.portfoliotracker.service;

import com.example.portfoliotracker.entity.AmfiNav;
import com.example.portfoliotracker.entity.AmfiScheme;
import com.example.portfoliotracker.entity.FundHouse;
import com.example.portfoliotracker.repository.AmfiImportRepository;
import com.example.portfoliotracker.repository.AmfiSchemeRepository;
import com.example.portfoliotracker.repository.FundHouseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class AmfiPersistService {

    private final FundHouseRepository fundHouseRepository;
    private final AmfiSchemeRepository schemeRepository;
    private final JdbcTemplate jdbcTemplate;
    private final AmfiImportRepository importRepository;

    @PersistenceContext
    private EntityManager em;

    public AmfiPersistService(FundHouseRepository fundHouseRepository,
                              AmfiSchemeRepository schemeRepository,
                              JdbcTemplate jdbcTemplate, AmfiImportRepository importRepository) {
        this.fundHouseRepository = fundHouseRepository;
        this.schemeRepository = schemeRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.importRepository = importRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistFundHousesChunk(List<FundHouse> chunk) {
        if (chunk == null || chunk.isEmpty()) return;
        fundHouseRepository.saveAll(chunk);
        fundHouseRepository.flush();
        em.flush();
        em.clear();
        log.debug("Persisted {} fund houses (chunk)", chunk.size());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistSchemesChunk(List<AmfiScheme> chunk) {
        if (chunk == null || chunk.isEmpty()) return;
        schemeRepository.saveAll(chunk);
        schemeRepository.flush();
        em.flush();
        em.clear();
        log.debug("Persisted {} schemes (chunk)", chunk.size());
    }

    /**
     * Batch insert NAVs via JdbcTemplate. Uses ON DUPLICATE KEY UPDATE id = id (no-op)
     * so we avoid per-row SELECTs. Each call runs in its own transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistNavsJdbcChunk(List<AmfiNav> navSlice) {
        if (navSlice == null || navSlice.isEmpty()) return;

        final String sql = "INSERT INTO amfi_nav (scheme_code, nav_date, nav_value, source, created_at) " +
                "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE id = id";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                AmfiNav n = navSlice.get(i);
                ps.setString(1, n.getSchemeCode());
                ps.setDate(2, java.sql.Date.valueOf(n.getNavDate()));
                ps.setBigDecimal(3, n.getNavValue());
                ps.setString(4, n.getSource());
                ps.setTimestamp(5, java.sql.Timestamp.valueOf(n.getCreatedAt() != null ? n.getCreatedAt() : LocalDateTime.now()));
            }

            @Override
            public int getBatchSize() {
                return navSlice.size();
            }
        });

        // no EM flush needed because we used JdbcTemplate; still clear EM to avoid cross-talk
        em.flush();
        em.clear();
        log.debug("Persisted {} nav rows (jdbc chunk)", navSlice.size());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistFundHouseUpdatesChunk(List<FundHouse> chunk) {
        if (chunk == null || chunk.isEmpty()) return;
        fundHouseRepository.saveAll(chunk);
        fundHouseRepository.flush();
        em.flush();
        em.clear();
    }
}
