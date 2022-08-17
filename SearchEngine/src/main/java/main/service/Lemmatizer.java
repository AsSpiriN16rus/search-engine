package main.service;

import main.model.Field;
import main.model.Index;
import main.model.Page;
import main.repository.FieldRepository;
import main.repository.IndexRepository;
import main.repository.LemmaRepository;
import main.repository.PageRepository;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Lemmatizer
{


    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Integer siteId;
    private static Map<Integer, Map<String,Integer>> frequencyLem = new HashMap<>();
    public Lemmatizer(int siteId, JdbcTemplate jdbcTemplate) {
        this.siteId = siteId;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void lemTextOnePage(String url,String rank, String str) throws IOException {

        Document document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com")
                .maxBodySize(0).get();
        SiteCrawling siteCrawling = new SiteCrawling(jdbcTemplate);

        HashMap<String, Double> documentWeight = new HashMap<>();
        StringBuilder strOnePageAdd = new StringBuilder();
        StringBuilder strOnePageDelete = new StringBuilder();
        String href = url.replaceAll(str, "");
        FieldRepository fieldRepository = new FieldRepository(jdbcTemplate);
        for (Field field : fieldRepository.findAllField()) {
            PageRepository pageRepository = new PageRepository(jdbcTemplate);
            if (pageRepository.findPage(href).size() != 0) {
                Document contentDocument = Jsoup.parse(pageRepository.findPage(href).get(0).getContent());
                String tagText = contentDocument.select(field.getSelector()).text();
                documentWeight.put(tagText, field.getWeight());
                strOnePageDelete.append(tagText + " ");
            }
            Document doc = Jsoup.parse(document.toString());
            String tagText = doc.select(field.getSelector()).text();
            documentWeight.put(tagText, field.getWeight());
            strOnePageAdd.append(tagText + " ");
        }

        switch (rank) {
            case "lemmatizer":
                PageRepository pageRepository = new PageRepository(jdbcTemplate);
                try {
                    pageRepository.deletePage(href);
                }catch (Exception ex){}
                org.jsoup.Connection.Response statusCode = Jsoup.connect(url).execute();
                fieldRepository.addField(siteId,href,statusCode.statusCode(),document.toString());

                lemmatizerText(strOnePageAdd.toString(), "onePage");
                if (strOnePageDelete.length() != 0) {
                    lemmatizerText(strOnePageDelete.toString(), "onePageDelete");
                }
                break;
            case "rank":
                lemRank(documentWeight, href);
                break;
            default:
                break;
        }

    }

    public void lemText(String rank){
        PageRepository pageRepository = new PageRepository(jdbcTemplate);
        List<Page> pages = pageRepository.findAllPage();

        for (Page page : pages){
            if (page.getSite_id() == siteId) {
                HashMap<String, Double> documentWeight = new HashMap<>();
                StringBuilder stringBuilder = new StringBuilder();
                FieldRepository fieldRepository = new FieldRepository(jdbcTemplate);
                for (Field field : fieldRepository.findAllField()) {
                    Document doc = Jsoup.parse(page.getContent());
                    String tagText = doc.select(field.getSelector()).text();
                    documentWeight.put(tagText, field.getWeight());
                    stringBuilder.append(tagText + " ");
                }

                switch (rank) {
                    case "lemmatizer":
                        lemmatizerText(stringBuilder.toString(), "morePage");
                        break;
                    case "rank":
                        lemRank(documentWeight, page.getPath());
                        break;
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
            HashMap lemValue = lemmatizerText(tagText, "false");
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
            LemmaRepository lemmaRepository = new LemmaRepository(jdbcTemplate);
            int lemmaId = lemmaRepository.getLemmaId(name.toString(), siteId);
            index.setLemmaId(lemmaId);
            index.setRank(weight);
            PageRepository pageRepository = new PageRepository(jdbcTemplate);
            int pageId = pageRepository.getPageId(url, siteId);
            index.setPageId(pageId);
            IndexRepository indexRepository = new IndexRepository(jdbcTemplate);
            indexRepository.addIndex(index);
        }
    }

    public HashMap<String, Integer> lemmatizerText(String textLem , String fullText){

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
            LemmaRepository lemmaRepository = new LemmaRepository(jdbcTemplate);
            switch (fullText) {
                case "morePage":
                    recordMap(siteId, luceneMap);
                    break;
                case "onePageDelete":
                    for (Map.Entry<String, Integer> entry : luceneMap.entrySet()){
                        lemmaRepository.minusFrequencyLemma(entry.getKey());
                    }
                    break;
                case "onePage":
                    for (Map.Entry<String, Integer> entry : luceneMap.entrySet()){
                        if (lemmaRepository.getLemmaId(entry.getKey(),siteId) == -1){
                            lemmaRepository.addOneLemma(entry.getKey(),siteId);
                        }else {
                            lemmaRepository.plusFrequencyLemma(entry.getKey());
                        }
                    }
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return luceneMap;
    }

    public void recordMap(int siteId, HashMap luceneMap){
        for (Object name: luceneMap.keySet()){
            if (!frequencyLem.containsKey(siteId)){
                frequencyLem.put(siteId, new HashMap<>());
            }
                if (!frequencyLem.get(siteId).containsKey(name.toString())) {
                    frequencyLem.get(siteId).put(name.toString(), 1);
                } else {
                    int frequency = frequencyLem.get(siteId).get(name.toString());
                    frequencyLem.get(siteId).replace(name.toString(), frequency + 1);
                }
        }
    }

    public void outputMap(int siteId){
        StringBuilder insertQuery = new StringBuilder();
        for (Map.Entry<Integer, Map<String, Integer>> entry : frequencyLem.entrySet()) {
            int key = entry.getKey();
            if (key == siteId) {
                Map<String, Integer> childMap = entry.getValue();
                for (Map.Entry<String, Integer> entry2 : childMap.entrySet()) {
                    String childKey = entry2.getKey();
                    Integer childValue = entry2.getValue();
                    insertQuery.append((insertQuery.length() == 0 ? "" : ",") + "('" + childKey + "','" + childValue + "', '" + siteId + "')");
                }
            }
        }
        LemmaRepository lemmaRepository = new LemmaRepository(jdbcTemplate);
        lemmaRepository.addLemma(insertQuery);
    }
}
