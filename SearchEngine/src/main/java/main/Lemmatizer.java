package main;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class Lemmatizer
{
    private static HashMap<String,Integer> luceneMap = new HashMap<>();
    public static void LemmatizerText(String textLem){

        try {
            LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
            String[] textSplit = textLem.replaceAll("[^а-яА-ЯёЁ ]", "").toLowerCase().split("\\s+");
            for (String string : textSplit){
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
            outputMap();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void outputMap(){
        for (String name: luceneMap.keySet()){

            String key = name.toString();
            String value = luceneMap.get(name).toString();
            System.out.println(key + " - " + value);
        }
    }
}
