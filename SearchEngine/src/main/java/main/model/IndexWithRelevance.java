package main.model;

public class IndexWithRelevance extends Index{

    private float relativeRelevance;
    private float absoluteRelevance;

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
