package main.repository;

import main.model.Lemma;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class LemmaRepository {
    private static final String ID = "id";
    private static final String SITE_ID = "site_id";
    private static final String LEMMA = "lemma";
    private static final String FREQUENCY = "frequency";

    private static final String COUNT_LEMMA_SQL = "SELECT count(*) FROM search_engine.lemma";
    private static final String COUNT_LEMMA_WITH_ID_SQL = "SELECT count(*) FROM search_engine.lemma where site_id = ";
    private static final String LEMMA_ID_SQL = "SELECT ID FROM search_engine.lemma WHERE lemma = ? AND site_id = ?";
    private static final String ADD_LEMMA_SQL = "INSERT INTO lemma(lemma,frequency,site_id) VALUES ";
    private static final String ADD_ONE_LEMMA_SQL = "INSERT INTO `lemma`(lemma, frequency,site_id) VALUES(?,?,?)";
    private static final String MINUS_FREQUENCY_LEMMA_SQL = "UPDATE search_engine.lemma SET frequency = frequency - 1 where lemma = ";
    private static final String PLUS_FREQUENCY_LEMMA_SQL = "UPDATE search_engine.lemma SET frequency = frequency + 1 where lemma = ";
    private static final String LEMMA_SEARCH_SQL = "SELECT * FROM search_engine.lemma where lemma = ? and site_id = ?";


    @Autowired
    private JdbcTemplate jdbcTemplate;

    public LemmaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int countLemma(){
        return jdbcTemplate.queryForObject(COUNT_LEMMA_SQL, Integer.class);
    }
    public int countLemmaWithId(int id){
        return jdbcTemplate.queryForObject(COUNT_LEMMA_WITH_ID_SQL + "'" + (id) +"'", Integer.class);
    }

    public Integer getLemmaId(String lemma, Integer siteId){
        int lemmaId;
        try {
        lemmaId = jdbcTemplate.queryForObject(LEMMA_ID_SQL, new Object[]{lemma,siteId}, Integer.class);
        }catch (Exception ex){
            lemmaId = -1;
        }
        return lemmaId;

    }

    public void addLemma(StringBuilder insertQuery){
        jdbcTemplate.update(ADD_LEMMA_SQL + insertQuery.toString());
    }

    public void addOneLemma(String entry, int siteId){
        jdbcTemplate.update(ADD_ONE_LEMMA_SQL,
                entry,1, siteId);
    }

    public void minusFrequencyLemma(String entry){
        jdbcTemplate.update(MINUS_FREQUENCY_LEMMA_SQL + "'" + entry + "'");
    }

    public void plusFrequencyLemma(String entry){
        jdbcTemplate.update(PLUS_FREQUENCY_LEMMA_SQL + "'" + entry + "'");
    }

    public List<Lemma> getLemmaSearch(String lemmaSearch, int siteId) {
        List<Lemma> lemmaList = jdbcTemplate.query(LEMMA_SEARCH_SQL,
                new Object[]{lemmaSearch,siteId}, (ResultSet rs, int rowNum) ->{
                    Lemma lemma = new Lemma();
                    lemma.setId(rs.getInt(ID));
                    lemma.setSite_id(rs.getInt(SITE_ID));
                    lemma.setLemma(rs.getString(LEMMA));
                    lemma.setFrequency(rs.getInt(FREQUENCY));
                    return lemma;
                });
        return new ArrayList<>(lemmaList);
    }
}
