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

    public ArrayList<PageSearchRelevance> search(){
        Lemmatizer lemmatizer = new Lemmatizer(0,jdbcTemplate);
        HashMap searchLem = lemmatizer.lemmatizerText(searchText,"false");

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

            ArrayList<PageSearchRelevance> pageSearchRelevancesList = pageRelevancesList(searchRelevance, lemmaSearh); // Final Search list!!

            System.out.println(pageSearchRelevancesList.size());
            for (PageSearchRelevance pageSearchRelevance : pageSearchRelevancesList){
                System.out.println(pageSearchRelevance.getUri());
                System.out.println("----------");
            }

            return pageSearchRelevancesList;
        }else {
            System.out.println("Не найдено результатов(");
        }
        return null;
    }

    private ArrayList<PageSearchRelevance> pageRelevancesList(ArrayList<IndexWithRelevance> searchRelevance, List<Lemma> lemmaSearh){
        ArrayList<PageSearchRelevance> pageSearchRelevancesList = new ArrayList<>();

        for (IndexWithRelevance index : searchRelevance){

            List<Page> pageList = getPageSearch(index.getPageId());
            PageSearchRelevance pageSearchRelevance = new PageSearchRelevance();
            pageSearchRelevance.setRelevance(index.getRelativeRelevance());
            pageSearchRelevance.setUri(pageList.get(0).getPath());


            Document doc = Jsoup.parse(pageList.get(0).getContent());
            String tagText = doc.select("title").text();
            pageSearchRelevance.setTitle(tagText);

            String[] textSplit = doc.text().split("\\.");

            StringBuilder sb = new StringBuilder();

            for (String string : textSplit){
                if (string.length() == 0){
                    continue;
                }
                for (Lemma lemma : lemmaSearh){

                    int indexJava = string.indexOf(lemma.getLemma());
                    int sizeLemma = lemma.getLemma().length();
                    if (indexJava != -1){
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(string);
                        stringBuilder.insert(indexJava + sizeLemma, "<b>" );
                        stringBuilder.insert(indexJava, "<b>" );
                        sb.append(stringBuilder + ".\n");

                    }
                }
            }

            pageSearchRelevance.setSnippet(sb.toString());

            pageSearchRelevancesList.add(pageSearchRelevance);
//                System.out.println(sb);
//                System.out.println();
        }
        return pageSearchRelevancesList;
    }

    private ArrayList<IndexWithRelevance> rank(ArrayList<Index> searchFiltering){
        ArrayList<IndexWithRelevance> searchRelevance = new ArrayList<>();

        float rank = 0.0F;
//        ArrayList<Integer> lemmaId = new ArrayList<>();

        for (int i = 0; i < searchFiltering.size(); i++){
//            System.out.println(searchFiltering.get(i).getLemmaId() + " idLEmma");
//            System.out.println(searchFiltering.get(i).getPageId());

            if (i + 1 < searchFiltering.size()) {
                if (searchFiltering.get(i).getPageId() == searchFiltering.get(i + 1).getPageId()) {
                    rank +=searchFiltering.get(i).getRank();
//                    lemmaId.add(searchFiltering.get(i).getLemmaId());
                } else if (searchFiltering.get(i).getPageId() != searchFiltering.get(i + 1).getPageId()) {
                    rank +=searchFiltering.get(i).getRank();
                    IndexWithRelevance index = new IndexWithRelevance();

//                    lemmaId.add(searchFiltering.get(i).getLemmaId());
//                    index.setLemma(lemmaId);

                    index.setAbsoluteRelevance(rank);
                    index.setPageId(searchFiltering.get(i).getPageId());

                    searchRelevance.add(index);

//                    lemmaId.clear();
                    rank = 0;
                }
            }else {
                rank += searchFiltering.get(i).getRank();
                IndexWithRelevance index = new IndexWithRelevance();
                index.setAbsoluteRelevance(rank);
                index.setLemmaId(searchFiltering.get(i).getLemmaId());
                index.setPageId(searchFiltering.get(i).getPageId());

//                lemmaId.add(searchFiltering.get(i).getLemmaId());
//                index.setLemma(lemmaId);

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
            lemma.setId(rs.getInt("site_id"));
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
            page.setId(rs.getInt("site_id"));
            page.setPath(rs.getString("path"));
            page.setCode(rs.getInt("code"));
            page.setContent(rs.getString("content"));
            return page;
        });
        return new ArrayList<>(indexList);
    }



}
