package main;

import liquibase.pro.packaged.S;
import main.model.Index;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;


public class Lemmatizer
{
    private static DataBase dataBase;

    public static void lemRank(HashMap documentWeight, String url){
        HashMap<String, Double> rankText = new HashMap();
        for (Object name: documentWeight.keySet()){
            String key = name.toString();
            Double weight = Double.valueOf(documentWeight.get(name).toString());
            HashMap lemValue = lemmatizerText(key, false);
            for (Object qw : lemValue.keySet()){
                Double value = Double.valueOf(lemValue.get(qw).toString());
                if (rankText.containsKey(qw.toString())){
                    Double valueMap = Double.valueOf(rankText.get(qw));
                    rankText.put(qw.toString(), (value * weight) + valueMap);
                }else {
                    rankText.put(qw.toString(), value * weight);
                }
            }
        }

        for (Object name: rankText.keySet()){
            Float weight = Float.valueOf(rankText.get(name).toString());
            Index index = new Index();
            index.setLemmaId(dataBase.getLemmaId(name.toString()));
            index.setRank(weight);
            index.setPageId(dataBase.getPageId(url));
            dataBase.addIndex(index);
        }


    }

    public static HashMap<String, Integer> lemmatizerText(String textLem , Boolean fullText){
        dataBase = new DataBase();
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
                dataBase.outputMap(luceneMap);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return luceneMap;
    }
}
