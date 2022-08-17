package main.service;
import main.model.*;
import main.repository.IndexRepository;
import main.repository.LemmaRepository;
import main.repository.PageRepository;
import main.service.Lemmatizer;
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
    private int siteId;

    public SearchEngine(JdbcTemplate jdbcTemplate, String searchText, int siteId) {
        this.jdbcTemplate = jdbcTemplate;
        this.searchText = searchText;
        this.siteId = siteId;
    }

    public ArrayList<PageSearchRelevance> search(){
        Lemmatizer lemmatizer = new Lemmatizer(0,jdbcTemplate);
        HashMap searchLem = lemmatizer.lemmatizerText(searchText,"false");
        List<Lemma> lemmaSearh = new ArrayList<>();

        for (Object key : searchLem.keySet()){
            LemmaRepository lemmaRepository = new LemmaRepository(jdbcTemplate);
            List<Lemma> lemmaList = lemmaRepository.getLemmaSearch(key.toString(),siteId);
            for (Lemma lemma : lemmaList){
                lemmaSearh.add(lemma);
            }
        }
        ArrayList<IndexWithRelevance> searchRelevance;
        if (lemmaSearh != null && lemmaSearh.size() != 0) {
            lemmaSort(lemmaSearh);

            ArrayList<Index> searchFiltering = lemmaFilter(lemmaSearh);

            searchRelevance = rank(searchFiltering);
            searchRelevance.sort(((o1, o2) -> Float.compare(o2.getAbsoluteRelevance(), o1.getAbsoluteRelevance())));

            ArrayList<PageSearchRelevance> pageSearchRelevancesList = pageRelevancesList(searchRelevance, lemmaSearh); // Final Search list!!

            return pageSearchRelevancesList;
        }
        return null;
    }

    private ArrayList<PageSearchRelevance> pageRelevancesList(ArrayList<IndexWithRelevance> searchRelevance, List<Lemma> lemmaSearh){
        ArrayList<PageSearchRelevance> pageSearchRelevancesList = new ArrayList<>();
        PageRepository pageRepository = new PageRepository(jdbcTemplate);
        for (IndexWithRelevance index : searchRelevance){
            List<Page> pageList = pageRepository.getPageSearch(index.getPageId());
            if (pageList != null && pageList.size() > 0 ) {
                PageSearchRelevance pageSearchRelevance = new PageSearchRelevance();
                pageSearchRelevance.setRelevance(index.getRelativeRelevance());
                pageSearchRelevance.setUri(pageList.get(0).getPath());


                Document doc = Jsoup.parse(pageList.get(0).getContent());
                String tagText = doc.select("title").text();
                pageSearchRelevance.setTitle(tagText);
                String tagTextBody = doc.text();
                String[] textSplit = tagTextBody.split("[\\.\\!\\?]");

                StringBuilder sb = new StringBuilder();
                for (String string : textSplit) {
                    StringBuilder stringBuilder1 = new StringBuilder();
                    for (Lemma lemma : lemmaSearh) {

                        stringBuilder1.append(lemma.getLemma() + " ");
                        int indexJava = string.indexOf(lemma.getLemma());
                        int sizeLemma = lemma.getLemma().length();


                        if (indexJava != -1) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(string);
                            stringBuilder.insert(indexJava + sizeLemma, "</b>");
                            stringBuilder.insert(indexJava, "<b>");
                            sb.append(stringBuilder + ".");
                            pageSearchRelevance.setSnippet(sb.toString());
                        }
                    }
                }


                pageSearchRelevancesList.add(pageSearchRelevance);
            }
        }
        return pageSearchRelevancesList;
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
                index.setLemmaId(searchFiltering.get(i).getLemmaId());
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
        IndexRepository indexRepository = new IndexRepository(jdbcTemplate);
        for(int i = 0 ; i < lemmaSearh.toArray().length; i++)
        {
            List<Index> indexList = indexRepository.getIndexSearch(lemmaSearh.get(i).getId());
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

}
