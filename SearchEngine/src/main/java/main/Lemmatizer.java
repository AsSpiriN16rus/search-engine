package main;

import main.model.Field;
import main.model.Index;
import main.model.Page;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Lemmatizer
{


    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Integer siteId;

    public Lemmatizer(int siteId, JdbcTemplate jdbcTemplate) {
        this.siteId = siteId;
        this.jdbcTemplate = jdbcTemplate;
    }


    public void lemText(String rank){
        List<Page> pages = findAllPage();

        for (Page page : pages){
            if (page.getSite_id() == siteId) {
                HashMap<String, Double> documentWeight = new HashMap<>();
                StringBuilder stringBuilder = new StringBuilder();
                for (Field field : findAllField()) {
                    Document doc = Jsoup.parse(page.getContent());
                    String tagText = doc.select(field.getSelector()).text();
                    documentWeight.put(tagText, field.getWeight());
                    stringBuilder.append(tagText + " ");
                }

                switch (rank) {
                    case "lemmatizer":
                        lemmatizerText(stringBuilder.toString(), true);
                        break;

                    case "rank":
//                        ExecutorService executorService = Executors.newSingleThreadExecutor();
//                        executorService.execute(new Runnable() {
//                            public void run() {
//
//                                lemRank(documentWeight, page.getPath());
//                            }
//                        });
//                        executorService.shutdown();
//                        break;
                        lemRank(documentWeight, page.getPath());
                    default:
                        break;
                }
            }
        }

    }

    public void lemRank(HashMap documentWeight, String url){

        HashMap<String, Double> rankText = new HashMap();

        for (Object name: documentWeight.keySet()){
            String tagText = name.toString();
            Double weight = Double.valueOf(documentWeight.get(name).toString());
            HashMap lemValue = lemmatizerText(tagText, false);
            for (Object lemma : lemValue.keySet()){
                Double value = Double.valueOf(lemValue.get(lemma).toString());
                if (rankText.containsKey(lemma.toString())){
                    Double valueMap = Double.valueOf(rankText.get(lemma));
                    rankText.put(lemma.toString(), (value * weight) + valueMap);
                }else {
                    rankText.put(lemma.toString(), value * weight);
                }
            }
        }

        for (Object name: rankText.keySet()){
            Float weight = Float.valueOf(rankText.get(name).toString());
            Index index = new Index();
            index.setLemmaId(getLemmaId(name.toString()));
            index.setRank(weight);


//            for (Page page : getPageId(url)){
//                if (page.getSite_id() == siteId){
//
//                    index.setPageId(page.getSite_id());
//                }
//            }
            int aa = getPageId(url, siteId);
            index.setPageId(aa);
            addIndex(index);
        }


    }


    public HashMap<String, Integer> lemmatizerText(String textLem , Boolean fullText){

        HashMap<String,Integer> luceneMap = new HashMap<>();
        try {

            LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
            textLem = textLem.replaceAll("[ёЁ]", "e");
            String[] textSplit = textLem.replaceAll("[^а-яА-ЯёЁ ]", " ").toLowerCase().split("\\s+");
            for (String string : textSplit){
                if (string.length() == 0){
                    continue;
                }
                boolean textValid = true;
                List<String> wordBaseForms = luceneMorphology.getMorphInfo(string);
                for (String morphInfo : wordBaseForms){
                    String morphInfoSplit[] = morphInfo.split("\\s+");
                    String lastWord = morphInfoSplit[morphInfoSplit.length - 1];
                    if (lastWord.equals("ПРЕДЛ") || lastWord.equals("МЕЖД") || lastWord.equals("СОЮЗ") || lastWord.equals("ЧАСТ")){
                        textValid = false;
                        break;
                    }
                }
                if (textValid){
                    List<String> wordBase = luceneMorphology.getNormalForms(string);
                    for (String s : wordBase){
                        if (luceneMap.containsKey(s)){
                            luceneMap.put(s,luceneMap.get(s) + 1);
                        }else {
                            luceneMap.put(s,1);
                        }
                    }
                }
            }
            if (fullText == true) {
                outputMap(siteId, luceneMap);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return luceneMap;
    }

    public List<Page> findAllPage() {
        String sql = "SELECT * FROM search_engine.page";
        List<Page> pages = jdbcTemplate.query(
                sql,
                (rs, rowNum) ->
                        new Page(
                                rs.getInt("id"),
                                rs.getInt("site_id"),
                                rs.getString("path"),
                                rs.getInt("code"),
                                rs.getString("content")
                        )
        );
        return pages;
    }

    public List<Field> findAllField() {
        String sql = "SELECT * FROM search_engine.field";
        List<Field> fields = jdbcTemplate.query(
                sql,
                (rs, rowNum) ->
                        new Field(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getString("selector"),
                                rs.getDouble("weight")
                        )
        );
        return fields;
    }

    public Integer getPageId(String page, Integer siteId){
        String sql = "SELECT ID FROM search_engine.page WHERE path = ? AND site_id = ?";


        return jdbcTemplate.queryForObject(
                sql, new Object[] { page,siteId }, Integer.class);
    }

    public void outputMap(int siteId, HashMap luceneMap){
        StringBuilder insertQuery = new StringBuilder();
        for (Object name: luceneMap.keySet()){
            String key = name.toString();
//            jdbcTemplate.update("INSERT INTO lemma(lemma, frequency) VALUES('" + key +"', 1) ON DUPLICATE KEY UPDATE frequency = frequency + 1 ");
            insertQuery.append((insertQuery.length() == 0 ? "" : ",") + "('" + key + "', 1, '" + siteId + "')");
        }
        jdbcTemplate.update("INSERT INTO lemma(lemma,frequency, site_id) VALUES "+ insertQuery.toString() +
                "ON DUPLICATE KEY UPDATE frequency = frequency + 1 ");
    }




    public Integer getLemmaId(String lemma){
        String sql = "SELECT ID FROM search_engine.lemma WHERE lemma = ?";

        return jdbcTemplate.queryForObject(
                sql, new Object[]{lemma}, Integer.class);
    }



    public void addIndex(Index index){
        jdbcTemplate.update("INSERT IGNORE INTO `index`(page_id, lemma_id,`rank`) VALUES(?,?,?)",
                index.getPageId(),index.getLemmaId(), index.getRank());
    }

}
