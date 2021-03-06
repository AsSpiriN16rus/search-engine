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
import java.util.Map;


public class Lemmatizer
{


    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String ID = "id";
    private static final String SITE_ID = "site_id";
    private static final String PATH = "path";
    private static final String CODE = "code";
    private static final String CONTENT = "content";
    private static final String NAME = "name";
    private static final String SELECTOR = "selector";
    private static final String WEIGHT = "weight";


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
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder stringBuilder1 = new StringBuilder();
        String href = url.replaceAll(str, "");
        for (Field field : findAllField()) {
            if (findPage(href).size() != 0) {
                Document doc1 = Jsoup.parse(findPage(href).get(0).getContent());
                String tagText1 = doc1.select(field.getSelector()).text();
                documentWeight.put(tagText1, field.getWeight());
                stringBuilder1.append(tagText1 + " ");
            }
            Document doc = Jsoup.parse(document.toString());
            String tagText = doc.select(field.getSelector()).text();
            documentWeight.put(tagText, field.getWeight());
            stringBuilder.append(tagText + " ");
        }

        switch (rank) {
            case "lemmatizer":
                jdbcTemplate.update("delete from search_engine.page where path = '" + href + "'");
                org.jsoup.Connection.Response statusCode = Jsoup.connect(url).execute();
                siteCrawling.addField(siteId,href,statusCode.statusCode(),document.toString());

                lemmatizerText(stringBuilder.toString(), "onePage");
                if (stringBuilder1.length() != 0) {
                    lemmatizerText(stringBuilder1.toString(), "onePageDelete");
                }
                break;

            case "rank":
                lemRank(documentWeight, href);
            default:
                break;
        }

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
                        lemmatizerText(stringBuilder.toString(), "morePage");
                        break;

                    case "rank":
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
            int lemmaId = getLemmaId(name.toString(), siteId);
            index.setLemmaId(lemmaId);
            index.setRank(weight);
            int pageId = getPageId(url, siteId);
            index.setPageId(pageId);
            addIndex(index);
        }


    }


    public HashMap<String, Integer> lemmatizerText(String textLem , String fullText){

        HashMap<String,Integer> luceneMap = new HashMap<>();
        try {

            LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
            textLem = textLem.replaceAll("[????]", "e");
            String[] textSplit = textLem.replaceAll("[^??-????-?????? ]", " ").toLowerCase().split("\\s+");
            for (String string : textSplit){
                if (string.length() == 0){
                    continue;
                }
                boolean textValid = true;
                List<String> wordBaseForms = luceneMorphology.getMorphInfo(string);
                for (String morphInfo : wordBaseForms){
                    String morphInfoSplit[] = morphInfo.split("\\s+");
                    String lastWord = morphInfoSplit[morphInfoSplit.length - 1];
                    if (lastWord.equals("??????????") || lastWord.equals("????????") || lastWord.equals("????????") || lastWord.equals("????????")){
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

            switch (fullText) {
                case "morePage":
                    recordMap(siteId, luceneMap);
                    break;
                case "onePageDelete":
                    for (Map.Entry<String, Integer> entry : luceneMap.entrySet()){
                        jdbcTemplate.update("UPDATE search_engine.lemma SET frequency = frequency - 1 where lemma = '" + entry.getKey() + "'");
                    }
                    break;
                case "onePage":
                    for (Map.Entry<String, Integer> entry : luceneMap.entrySet()){

                        if (getLemmaId(entry.getKey(),siteId) == null){
                            jdbcTemplate.update("INSERT INTO `lemma`(lemma, frequency,site_id) VALUES(?,?,?)",
                                    entry.getKey(),1, siteId);;
                        }else {
                            jdbcTemplate.update("UPDATE search_engine.lemma SET frequency = frequency + 1 where lemma = '" + entry.getKey() + "'");
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
    public List<Page> findPage(String urlPage) {
        String sql = "SELECT * FROM search_engine.page where path = '" + urlPage + "'";
        List<Page> pages = jdbcTemplate.query(
                sql,
                (rs, rowNum) ->
                        new Page(
                                rs.getInt(ID),
                                rs.getInt(SITE_ID),
                                rs.getString(PATH),
                                rs.getInt(CODE),
                                rs.getString(CONTENT)
                        )
        );
        return pages;
    }

    public List<Page> findAllPage() {
        String sql = "SELECT * FROM search_engine.page";
        List<Page> pages = jdbcTemplate.query(
                sql,
                (rs, rowNum) ->
                        new Page(
                                rs.getInt(ID),
                                rs.getInt(SITE_ID),
                                rs.getString(PATH),
                                rs.getInt(CODE),
                                rs.getString(CONTENT)
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
                                rs.getInt(ID),
                                rs.getString(NAME),
                                rs.getString(SELECTOR),
                                rs.getDouble(WEIGHT)
                        )
        );
        return fields;
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
        try {
        jdbcTemplate.update("INSERT INTO lemma(lemma,frequency,site_id) VALUES "+ insertQuery.toString());
        }catch (Exception ex){

        }
    }

    public Integer getPageId(String path, Integer siteId){

        String sql = "SELECT ID FROM search_engine.page WHERE path = ? AND site_id = ?";
        return jdbcTemplate.queryForObject(
                sql, new Object[] { path,siteId }, Integer.class);
    }

    public Integer getLemmaId(String lemma, Integer siteId){
        String sql = "SELECT ID FROM search_engine.lemma WHERE lemma = ? AND site_id = ?";
            return jdbcTemplate.queryForObject(
                    sql, new Object[]{lemma,siteId}, Integer.class);

    }



    public void addIndex(Index index){
        jdbcTemplate.update("INSERT IGNORE INTO `index`(page_id, lemma_id,`rank`) VALUES(?,?,?)",
                index.getPageId(),index.getLemmaId(), index.getRank());
    }

}
