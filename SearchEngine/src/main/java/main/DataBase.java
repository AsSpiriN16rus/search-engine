package main;

import main.model.Field;
import main.model.Index;
import main.model.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import java.util.HashMap;
import java.util.List;

@Repository
public class DataBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;


//    @Autowired
//    public void setDataSource(DataSource dataSource){
//        this.jdbcTemplate = new JdbcTemplate(dataSource);
//
//    }

//    public static DataSource mysqlDataSource() {
//        DriverManagerDataSource dataSource = new DriverManagerDataSource();
//        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
//        dataSource.setUrl("jdbc:mysql://localhost:3306/search_engine?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC");
//        dataSource.setUsername("root");
//        dataSource.setPassword("q1w2e3r4t5A1");
//        return dataSource;
//    }

    public void dropTable(){
        String sql = "TRUNCATE lemma";
        jdbcTemplate.update(sql);
        sql = "TRUNCATE page";
        jdbcTemplate.update(sql);
        sql = "TRUNCATE `index`";
        jdbcTemplate.update(sql);
    }

    public List<Field> findAllField() {
        String sql = "SELECT * FROM search_engine.field";
        List<Field> fields = jdbcTemplate.query(
                sql,
                (rs, rowNum) ->
                        new Field(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getString("selector"),
                                rs.getDouble("weight")
                        )
        );
        return fields;
    }

    public void addField(Page link){
        jdbcTemplate.update("INSERT IGNORE INTO page(path, code,content) VALUES(?,?,?)",
                link.getPath(),link.getCode(),link.getContent());
    }

    public synchronized void outputMap(HashMap luceneMap){
        StringBuilder insertQuery = new StringBuilder();
        for (Object name: luceneMap.keySet()){
            String key = name.toString();
            insertQuery.append((insertQuery.length() == 0 ? "" : ",") + "('" + key + "', 1)");
        }
        jdbcTemplate.update("INSERT INTO lemma(lemma,frequency) VALUES "+ insertQuery.toString() +
                "ON DUPLICATE KEY UPDATE frequency = frequency + 1 ");
    }

    public void addIndex(Index index){
        jdbcTemplate.update("INSERT IGNORE INTO `index`(page_id, lemma_id,`rank`) VALUES(?,?,?)",
                index.getPageId(),index.getLemmaId(), index.getRank());
    }

    public synchronized Integer getLemmaId(String lemma){
        String sql = "SELECT ID FROM search_engine.lemma WHERE lemma = ?";

        return jdbcTemplate.queryForObject(
                sql, new Object[]{lemma}, Integer.class);
    }

    public synchronized Integer getPageId(String page){
        String sql = "SELECT ID FROM search_engine.page WHERE path = ?";

        return jdbcTemplate.queryForObject(
                sql, new Object[]{page}, Integer.class);
    }
}
