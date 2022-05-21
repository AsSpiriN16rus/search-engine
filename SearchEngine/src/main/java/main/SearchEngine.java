package main;
import main.model.Index;
import main.model.Lemma;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        lemmaSort(lemmaSearh);

        ArrayList<Index> searchFiltering  = lemmaFilter(lemmaSearh);

        Map<Integer, Float> relativeRelevance = rank(searchFiltering);

        for (Object key : relativeRelevance.keySet()){
            System.out.println("idPage - " +  key.toString());
            System.out.println("rankPage - " +  relativeRelevance.get(key));
        }


    }

    private Map<Integer, Float> rank(ArrayList<Index> searchFiltering){
        HashMap<Integer,Float> absoluteRelevance = new HashMap<>();
        float rank = 0;
        for (int i = 0; i < searchFiltering.size(); i++){
            if (i + 1 < searchFiltering.size()) {
                if (searchFiltering.get(i).getPageId() == searchFiltering.get(i + 1).getPageId()) {
                    rank += searchFiltering.get(i).getRank();
                } else if (searchFiltering.get(i).getPageId() != searchFiltering.get(i + 1).getPageId()) {
                    rank += searchFiltering.get(i).getRank();
                    absoluteRelevance.put(searchFiltering.get(i).getPageId(), rank);
                    rank = 0;
                }
            }else {
                rank += searchFiltering.get(i).getRank();
                absoluteRelevance.put(searchFiltering.get(i).getPageId(), rank);
                absoluteRelevance.put(29, 6.4F);
            }
        }

        float maxRelevance = Collections.max(absoluteRelevance.values());

        Map<Integer,Float> relativeRelevance = new HashMap<>();

        for (Object key : absoluteRelevance.keySet()){
            float keyRelevance = absoluteRelevance.get(key) / maxRelevance;
            relativeRelevance.put((Integer) key, keyRelevance);
        }


//        relativeRelevance.entrySet().stream()
//                .sorted(Map.Entry.<Integer, Float>comparingByValue().reversed());


//        HashMap<Integer, Float> aa = relativeRelevance.entrySet().stream()
//                .sorted(Map.Entry.<Integer, Float>comparingByValue().reversed()).collect(Collectors.mapping(new HashMap<Integer, Float>()));



        return relativeRelevance;
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
//        System.out.println(searchFiltering.size() + "search");
//        for (Index index : searchFiltering){
//            System.out.println(index.getPageId() + " " +  index.getLemmaId() + " " + index.getId());
//        }
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



}
