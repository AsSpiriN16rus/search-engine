package main.configs;

import liquibase.database.AbstractJdbcDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.AbstractJdbcInsert;

import javax.sql.DataSource;
import java.security.spec.NamedParameterSpec;

@Configuration
public class DataBaseConfig extends AbstractJdbcConfiguration
{
    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource){
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
