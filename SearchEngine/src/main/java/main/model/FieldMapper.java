package main.model;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class FieldMapper implements RowMapper<Field> {
    @Override
    public Field mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        Field field = new Field();
        field.setId(resultSet.getInt("id"));
        field.setName(resultSet.getString("name"));
        field.setSelector(resultSet.getString("selector"));
        field.setWeight(resultSet.getInt("weight"));
        return field;
    }
}
