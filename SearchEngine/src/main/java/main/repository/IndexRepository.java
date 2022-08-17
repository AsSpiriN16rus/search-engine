package main.repository;

import liquibase.pro.packaged.A;
import main.model.Index;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class IndexRepository {

    private static final String ID = "id";
    private static final String PAGE_ID = "page_id";
    private static final String LEMMA_ID = "lemma_id";
    private static final String RANK = "rank";

    private static final String ADD_INDEX_SQL = "INSERT IGNORE INTO `index`(page_id, lemma_id,`rank`) VALUES(?,?,?)";
    private static final String INDEX_SEARCH_SQL = "SELECT * FROM search_engine.index where lemma_id = ?";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public IndexRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void addIndex(Index index){
        jdbcTemplate.update(ADD_INDEX_SQL,
                index.getPageId(),index.getLemmaId(), index.getRank());
    }

    public List<Index> getIndexSearch(int id) {
        List<Index> indexList = jdbcTemplate.query(INDEX_SEARCH_SQL, new Object[]{id}, (ResultSet rs, int rowNum) ->{
            Index index = new Index();
            index.setId(rs.getInt(ID));
            index.setPageId(rs.getInt(PAGE_ID));
            index.setLemmaId(rs.getInt(LEMMA_ID));
            index.setRank(rs.getFloat(RANK));
            return index;
        });
        return new ArrayList<>(indexList);
    }
}
