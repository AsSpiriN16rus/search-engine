package main;
import main.model.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
        ArrayList<IndexWithRelevance> searchRelevance;
        if (lemmaSearh != null) {
            lemmaSort(lemmaSearh);

            ArrayList<Index> searchFiltering = lemmaFilter(lemmaSearh);
            searchRelevance = rank(searchFiltering);
            searchRelevance.sort(((o1, o2) -> Float.compare(o2.getAbsoluteRelevance(), o1.getAbsoluteRelevance())));
            for (IndexWithRelevance index : searchRelevance){
                System.out.println("idPage - " + index.getPageId());
                System.out.println("rankPage - " + index.getRelativeRelevance());

                List<Page> pageList = getPageSearch(index.getPageId());
                PageSearchRelevance pageSearchRelevance = new PageSearchRelevance();
                pageSearchRelevance.setRelevance(index.getRelativeRelevance());
                pageSearchRelevance.setUri(pageList.get(0).getPath());

                System.out.println(pageList.get(0).getPath() + " path");


                Document doc = Jsoup.parse(pageList.get(0).getContent());
                String tagText = doc.select("title").text();
                pageSearchRelevance.setTitle(tagText);
                System.out.println(tagText);

            }





        }else {
            System.out.println("Не найдено результатов(");
        }



    }

    private ArrayList<IndexWithRelevance> rank(ArrayList<Index> searchFiltering){
        ArrayList<IndexWithRelevance> searchRelevance = new ArrayList<>();
        float rank = 0.0F;
        for (int i = 0; i < searchFiltering.size(); i++){
            if (i + 1 < searchFiltering.size()) {
                if (searchFiltering.get(i).getPageId() == searchFiltering.get(i + 1).getPageId()) {
                    rank +=searchFiltering.get(i).getRank();
                } else if (searchFiltering.get(i).getPageId() != searchFiltering.get(i + 1).getPageId()) {
                    rank +=searchFiltering.get(i).getRank();
                    IndexWithRelevance index = new IndexWithRelevance();
                    index.setAbsoluteRelevance(rank);
                    index.setPageId(searchFiltering.get(i).getPageId());
                    searchRelevance.add(index);
                    rank = 0;
                }
            }else {
                rank += searchFiltering.get(i).getRank();
                IndexWithRelevance index = new IndexWithRelevance();
                index.setAbsoluteRelevance(rank);
                index.setPageId(searchFiltering.get(i).getPageId());
                searchRelevance.add(index);
            }
        }

        float maxRelevance = searchRelevance.get(0).getAbsoluteRelevance();


        int a = 0;
        for (int i = 0; i < searchRelevance.size(); i++){
            if (searchRelevance.get(i).getAbsoluteRelevance() > maxRelevance){
                maxRelevance = searchRelevance.get(i).getAbsoluteRelevance();
            }
            if (a == 1){
                searchRelevance.get(i).setRelativeRelevance(searchRelevance.get(i).getAbsoluteRelevance() / maxRelevance);
            }
            if (i == searchRelevance.size() - 1 && a == 0){
                i = -1;
                a++;
            }
        }
        return searchRelevance;
    }

    private ArrayList<Index> lemmaFilter(List<Lemma> lemmaSearh){
        ArrayList<Index> searchFiltering = new ArrayList<>();

        for(int i = 0 ; i < lemmaSearh.toArray().length; i++)
        {
            List<Index> indexList = getIndexSearch(lemmaSearh.get(i).getId());
            ArrayList<Index> searchFiltering3 = new ArrayList<>();
            for (Index index : indexList){
                if (i == 0) {
                    searchFiltering.add(index);
                }else {
                    int a = 0;
                    for (Index index1 : searchFiltering){
                        if (index.getPageId() == index1.getPageId() && a == 0){
                            searchFiltering3.add(index);
                            searchFiltering3.add(index1);
                            a++;
                        }else if (index.getPageId() == index1.getPageId()){
                            searchFiltering3.add(index1);
                        }
                    }
                }

            }

            if (i != 0) {
                searchFiltering.clear();
                searchFiltering.addAll(searchFiltering3);
            }

        }
        return searchFiltering;
    }


    private void lemmaSort(List<Lemma> lemmaSearh){
        lemmaSearh.sort(new Comparator<Lemma>() {
            @Override
            public int compare(Lemma o1, Lemma o2) {
                if (o1.getFrequency() == o2.getFrequency()) return 0;
                else if (o1.getFrequency() > o2.getFrequency()) return 1;
                else return -1;
            }
        });

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
            index.setRank(rs.getFloat("rank"));
            return index;
        });
        return new ArrayList<>(indexList);
    }

    public List<Page> getPageSearch(int id) {
        List<Page> indexList = jdbcTemplate.query("SELECT * FROM search_engine.page where id = ?", new Object[]{id}, (ResultSet rs, int rowNum) ->{
            Page page = new Page();
            page.setId(rs.getInt("id"));
            page.setPath(rs.getString("path"));
            page.setCode(rs.getInt("code"));
            page.setContent(rs.getString("content"));
            return page;
        });
        return new ArrayList<>(indexList);
    }



}
