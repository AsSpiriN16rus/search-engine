package main;

import main.model.Field;
import main.model.Page;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.xml.sax.InputSource;

import javax.lang.model.util.Elements;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;



public class Lemmatizer
{


    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Lemmatizer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

//    public static void lemRank(HashMap documentWeight, String url){
//        HashMap<String, Double> rankText = new HashMap();
//        for (Object name: documentWeight.keySet()){
//            String key = name.toString();
//            Double weight = Double.valueOf(documentWeight.get(name).toString());
//            HashMap lemValue = lemmatizerText(key, false);
//            for (Object qw : lemValue.keySet()){
//                Double value = Double.valueOf(lemValue.get(qw).toString());
//                if (rankText.containsKey(qw.toString())){
//                    Double valueMap = Double.valueOf(rankText.get(qw));
//                    rankText.put(qw.toString(), (value * weight) + valueMap);
//                }else {
//                    rankText.put(qw.toString(), value * weight);
//                }
//            }
//        }
//
//        for (Object name: rankText.keySet()){
//            Float weight = Float.valueOf(rankText.get(name).toString());
//            Index index = new Index();
//            index.setLemmaId(dataBase.getLemmaId(name.toString()));
//            index.setRank(weight);
//            index.setPageId(dataBase.getPageId(url));
//            dataBase.addIndex(index);
//        }
//
//
//    }



//    public void lemText(Document document, String url){
//        StringBuilder stringBuilder = new StringBuilder();
//        HashMap <String,Double> documentWeight = new HashMap<>();
//        for (Field tag :  findAllField()){
//            String tagText = document.select(tag.getSelector()).text();
//            documentWeight.put(tagText, tag.getWeight());
//            stringBuilder.append(tagText + " ");
//        }
//
//
////        Lemmatizer.lemmatizerText(stringBuilder.toString(), true);
////        Lemmatizer.lemRank(documentWeight, url);
//    }

    public void lemText(String url){
        HashMap <String,String> getDocumet = new HashMap<>();
        url = url.charAt(url.length() - 1) == '/'  ?
                url.substring(0, url.length() - 1) : url.substring(0, url.length() );
        for (Page page : findAllPage()){
            getDocumet.put(url + page.getPath(), page.getContent());
            for (Field field : findAllField()){

                Document doc = Jsoup.parse(page.getContent());
                String tagText = doc.select("body").text();
                System.out.println((field.getSelector()));
                System.out.println(tagText);
                System.out.println("-----------");
            }
            System.out.println("+++++++++++++++");
        }

        StringBuilder stringBuilder = new StringBuilder();
        HashMap <String,Double> documentWeight = new HashMap<>();
//        for (Field tag :  findAllField()){
//            String tagText = document.select(tag.getSelector()).text();
//            documentWeight.put(tagText, tag.getWeight());
//            stringBuilder.append(tagText + " ");
//        }


    //        Lemmatizer.lemmatizerText(stringBuilder.toString(), true);
    //        Lemmatizer.lemRank(documentWeight, url);
    }

    public static HashMap<String, Integer> lemmatizerText(String textLem , Boolean fullText){

        HashMap<String,Integer> luceneMap = new HashMap<>();
        try {

            LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
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
//                outputMap(luceneMap);
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

    public void outputMap(HashMap luceneMap){
        StringBuilder insertQuery = new StringBuilder();
        for (Object name: luceneMap.keySet()){
            String key = name.toString();
            insertQuery.append((insertQuery.length() == 0 ? "" : ",") + "('" + key + "', 1)");
        }
        jdbcTemplate.update("INSERT INTO lemma(lemma,frequency) VALUES "+ insertQuery.toString() +
                "ON DUPLICATE KEY UPDATE frequency = frequency + 1 ");
    }
}
