package main.repository;

import main.model.Field;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class FieldRepository {

    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String SELECTOR = "selector";
    private static final String WEIGHT = "weight";

    private static final String FIND_ALL_FIELD_SQL = "SELECT * FROM search_engine.field";
    private static final String ADD_FIELD_SQL = "INSERT IGNORE INTO page(site_id, path, code,content) VALUES(?,?,?,?)";



    @Autowired
    private JdbcTemplate jdbcTemplate;

    public FieldRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Field> findAllField() {
        List<Field> fields = jdbcTemplate.query(
                FIND_ALL_FIELD_SQL,
                (rs, rowNum) ->
                        new Field(
                                rs.getInt(ID),
                                rs.getString(NAME),
                                rs.getString(SELECTOR),
                                rs.getDouble(WEIGHT)
                        )
        );
        return fields;
    }


    public void addField(Integer siteId, String path, int code, String content){
        jdbcTemplate.update(ADD_FIELD_SQL,
                siteId,path,code,content);
    }


}
