package main.repository;

import main.model.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PageRepository {

    private static final String ID = "id";
    private static final String SITE_ID = "site_id";
    private static final String PATH = "path";
    private static final String CODE = "code";
    private static final String CONTENT = "content";


    private static final String COUNT_PAGE_SQL = "SELECT count(*) FROM search_engine.page";
    private static final String COUNT_PAGE_WITH_ID_SQL = "SELECT count(*) FROM search_engine.page where site_id = ";
    private static final String DELETE_PAGE_SQL = "delete from search_engine.page where path = ";
    private static final String FIND_PAGE_SQL = "SELECT * FROM search_engine.page where path = ";
    private static final String FIND_ALL_PAGE_SQL = "SELECT * FROM search_engine.page";
    private static final String PAGE_ID_SQL = "SELECT ID FROM search_engine.page WHERE path = ? AND site_id = ?";
    private static final String PAGE_SEARCH_SQL = "SELECT * FROM search_engine.page where id = ?";



    @Autowired
    private JdbcTemplate jdbcTemplate;

    public PageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int countPage(){
        return jdbcTemplate.queryForObject(COUNT_PAGE_SQL, Integer.class);
    }
    public int countPageWithId(int id){
        return jdbcTemplate.queryForObject(COUNT_PAGE_WITH_ID_SQL + "'" + (id) +"'", Integer.class);
    }
    public void deletePage(String href){
       jdbcTemplate.update(DELETE_PAGE_SQL + "'" + href + "'");
    }
    public List<Page> findPage(String urlPage) {
        String sql = FIND_PAGE_SQL + "'" + urlPage + "'";
        List<Page> pages = jdbcTemplate.query(
                sql,
                (rs, rowNum) ->
                        new Page(
                                rs.getInt(ID),
                                rs.getInt(SITE_ID),
                                rs.getString(PATH),
                                rs.getInt(CODE),
                                rs.getString(CONTENT)
                        )
        );
        return pages;
    }
    public List<Page> findAllPage() {
        List<Page> pages = jdbcTemplate.query(
                FIND_ALL_PAGE_SQL,
                (rs, rowNum) ->
                        new Page(
                                rs.getInt(ID),
                                rs.getInt(SITE_ID),
                                rs.getString(PATH),
                                rs.getInt(CODE),
                                rs.getString(CONTENT)
                        )
        );
        return pages;
    }

    public Integer getPageId(String path, Integer siteId){
        return jdbcTemplate.queryForObject(
                PAGE_ID_SQL, new Object[] { path,siteId }, Integer.class);
    }

    public List<Page> getPageSearch(int id) {
        List<Page> indexList = jdbcTemplate.query(PAGE_SEARCH_SQL, new Object[]{id}, (ResultSet rs, int rowNum) ->{
            Page page = new Page();
            page.setId(rs.getInt(ID));
            page.setId(rs.getInt(SITE_ID));
            page.setPath(rs.getString(PATH));
            page.setCode(rs.getInt(CODE));
            page.setContent(rs.getString(CONTENT));
            return page;
        });
        return new ArrayList<>(indexList);
    }
}
