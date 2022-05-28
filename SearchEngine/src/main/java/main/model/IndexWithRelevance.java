package main.model;

import java.util.ArrayList;

public class IndexWithRelevance extends Index{

    private float relativeRelevance;
    private float absoluteRelevance;
//    private ArrayList<Integer> lemmaIDList;
//
//    public ArrayList<Integer> getLemmaList() {
//        return lemmaIDList;
//    }
//
//    public void setLemma(ArrayList<Integer> lemmaIDList) {
//        this.lemmaIDList = lemmaIDList;
//    }

    public float getRelativeRelevance() {
        return relativeRelevance;
    }

    public void setRelativeRelevance(float relativeRelevance) {
        this.relativeRelevance = relativeRelevance;
    }

    public float getAbsoluteRelevance() {
        return absoluteRelevance;
    }

    public void setAbsoluteRelevance(float absoluteRelevance) {
        this.absoluteRelevance = absoluteRelevance;
    }
}
