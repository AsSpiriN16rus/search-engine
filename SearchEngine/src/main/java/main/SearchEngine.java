package main;
import liquibase.pro.packaged.I;
import main.model.Index;
import main.model.Lemma;
import main.model.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.util.*;

public class SearchEngine
{
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String searchText;

    public SearchEngine(JdbcTemplate jdbcTemplate, String searchText) {
        this.jdbcTemplate = jdbcTemplate;
        this.searchText = searchText;
    }

    public void search(){
        Lemmatizer lemmatizer = new Lemmatizer(jdbcTemplate);
        HashMap searchLem = lemmatizer.lemmatizerText(searchText,false);
        List<Lemma> lemmaSearh = new ArrayList<>();

        for (Object key : searchLem.keySet()){
            List<Lemma> lemmaList = getLemmaSearch(key.toString());
            for (Lemma lemma : lemmaList){
                lemmaSearh.add(lemma);
            }
        }

        lemmaSearh.sort(new Comparator<Lemma>() {
            @Override
            public int compare(Lemma o1, Lemma o2) {
                if (o1.getFrequency() == o2.getFrequency()) return 0;
                else if (o1.getFrequency() > o2.getFrequency()) return 1;
                else return -1;
            }
        });

        for(int i = 0 ; i < lemmaSearh.toArray().length; i++)
        {
//            System.out.println(lemmaSearh.get(i).getLemma() + " " + lemmaSearh.get(i).getFrequency());
//            System.out.println(lemmaSearh.get(i).getId());


           List<Index> indexList = getIndexSearch(lemmaSearh.get(i).getId());
           for (Index index : indexList){
               System.out.println(index.getPageId() + " " +  index.getLemmaId());
           }
            System.out.println("-0--------------");
        }


    }

    public List<Lemma> getLemmaSearch(String lemmaSearch) {
        List<Lemma> lemmaList = jdbcTemplate.query("SELECT * FROM search_engine.lemma where lemma = ?", new Object[]{lemmaSearch}, (ResultSet rs, int rowNum) ->{
            Lemma lemma = new Lemma();
            lemma.setId(rs.getInt("id"));
            lemma.setLemma(rs.getString("lemma"));
            lemma.setFrequency(rs.getInt("frequency"));
            return lemma;
        });
        return new ArrayList<>(lemmaList);
    }

    public List<Index> getIndexSearch(int id) {
        List<Index> indexList = jdbcTemplate.query("SELECT * FROM search_engine.index where lemma_id = ?", new Object[]{id}, (ResultSet rs, int rowNum) ->{
            Index index = new Index();
            index.setId(rs.getInt("id"));
            index.setPageId(rs.getInt("page_id"));
            index.setLemmaId(rs.getInt("lemma_id"));
            index.setRank(rs.getInt("rank"));
            return index;
        });
        return new ArrayList<>(indexList);
    }



}
