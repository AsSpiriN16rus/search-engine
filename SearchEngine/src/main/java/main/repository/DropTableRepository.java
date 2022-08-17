package main.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class DropTableRepository {
    private static final String TRUNCATE_LEMMA_SQL = "TRUNCATE lemma";
    private static final String TRUNCATE_PAGE_SQL = "TRUNCATE page";
    private static final String TRUNCATE_INDEX_SQL = "TRUNCATE `index`";
    private static final String TRUNCATE_SITE_SQL = "TRUNCATE site";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public DropTableRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    public void dropTable(){
        jdbcTemplate.update(TRUNCATE_LEMMA_SQL);
        jdbcTemplate.update(TRUNCATE_PAGE_SQL);
        jdbcTemplate.update(TRUNCATE_INDEX_SQL);
        jdbcTemplate.update(TRUNCATE_SITE_SQL);
    }
}
