package main.repository;

import main.model.Site;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Calendar;
import java.util.List;

public class SiteRepository {
    private static final String ID = "id";
    private static final String STATUS = "status";
    private static final String STATUS_TIME = "status_time";
    private static final String LAST_ERROR = "last_error";
    private static final String URL = "url";
    private static final String NAME = "name";


    private static final String ADD_SITE_SQL = "INSERT IGNORE INTO search_engine.site(status, status_time, url,name) VALUES(?,?,?,?)";
    private static final String UPDATE_SITE_SQL = "UPDATE search_engine.site SET status = 'INDEXED' where id = ";
    private static final String FIND_ALL_SITE_SQL = "SELECT * FROM search_engine.site";
    private static final String COUNT_SITE_SQL = "SELECT count(*) FROM search_engine.site";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public SiteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void addSite(String url, String name){
        java.sql.Date date = new java.sql.Date(Calendar.getInstance().getTime().getTime());
        Site site = new Site("INDEXING", date,url,name);
        jdbcTemplate.update(ADD_SITE_SQL,
                site.getStatus(),site.getStatusTime(),site.getUrl(),site.getName());
    }

    public void updateSite(int siteId){
        jdbcTemplate.update(UPDATE_SITE_SQL + "'" + (siteId + 1) +"'");
    }

    public List<Site> findAllSite() {
        String sql = FIND_ALL_SITE_SQL;
        List<main.model.Site> sites = jdbcTemplate.query(
                sql,
                (rs, rowNum) ->
                        new main.model.Site(
                                rs.getInt(ID),
                                rs.getString(STATUS),
                                rs.getDate(STATUS_TIME),
                                rs.getString(LAST_ERROR),
                                rs.getString(URL),
                                rs.getString(NAME)
                        )
        );
        return sites;
    }

    public int countSite(){
        return jdbcTemplate.queryForObject(COUNT_SITE_SQL, Integer.class);
    }

}
