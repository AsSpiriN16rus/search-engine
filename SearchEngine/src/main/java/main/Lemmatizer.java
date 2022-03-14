package main;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;


public class Lemmatizer
{
    public static void LemmatizerText(JdbcTemplate jdbcTemplate, String textLem){

        try {
            HashMap<String,Integer> luceneMap = new HashMap<>();
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
            outputMap(jdbcTemplate, luceneMap);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

//    public synchronized static void outputMap(JdbcTemplate jdbcTemplate,HashMap luceneMap){
//        for (Object name: luceneMap.keySet()){
//
//            String key = name.toString();
//            jdbcTemplate.update("INSERT INTO lemma(lemma, `frequency`) VALUES('" + key + "',1)" +
//                    "ON DUPLICATE KEY UPDATE frequency = frequency + 1 ");
////            String value = luceneMap.get(name).toString();
////            System.out.println(key + " - " + value);
//        }
//    }
    public synchronized static void outputMap(JdbcTemplate jdbcTemplate,HashMap luceneMap){
        StringBuilder insertQuery = new StringBuilder();
        for (Object name: luceneMap.keySet()){
            String key = name.toString();
            insertQuery.append((insertQuery.length() == 0 ? "" : ",") + "('" + key + "', 1)");
        }
        jdbcTemplate.update("INSERT INTO lemma(lemma,frequency) VALUES "+ insertQuery.toString() +
                "ON DUPLICATE KEY UPDATE frequency = frequency + 1 ");
    }
}
