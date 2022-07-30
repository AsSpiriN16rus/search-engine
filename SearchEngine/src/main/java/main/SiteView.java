package main;

import main.model.Site;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Calendar;
import java.util.List;

public class SiteView {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public SiteView(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void addSite(String url, String name){
        java.sql.Date date = new java.sql.Date(Calendar.getInstance().getTime().getTime());
        Site site = new Site("INDEXING", date,url,name);
        jdbcTemplate.update("INSERT IGNORE INTO search_engine.site(status, status_time, url,name) VALUES(?,?,?,?)",
                site.getStatus(),site.getStatusTime(),site.getUrl(),site.getName());
    }

    public void updateSite(int siteId){
        jdbcTemplate.update("UPDATE search_engine.site SET status = 'INDEXED' where id = '" + (siteId + 1) +"'");
    }

    public List<main.model.Site> findAllSite() {
        String sql = "SELECT * FROM search_engine.site";
        List<main.model.Site> sites = jdbcTemplate.query(
                sql,
                (rs, rowNum) ->
                        new main.model.Site(
                                rs.getInt("id"),
                                rs.getString("status"),
                                rs.getDate("status_time"),
                                rs.getString("last_error"),
                                rs.getString("url"),
                                rs.getString("name")
                        )
        );
        return sites;
    }

    public void dropTable(){
        jdbcTemplate.update("TRUNCATE lemma");
        jdbcTemplate.update("TRUNCATE page");
        jdbcTemplate.update("TRUNCATE `index`");
        jdbcTemplate.update("TRUNCATE site");
    }
}
