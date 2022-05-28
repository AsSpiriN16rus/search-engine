package main.model;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "page")
public class Page implements Serializable
{

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    public Page() {
    }

    public Page(int id,int site_id, String path, int code, String content) {
        this.site_id = site_id;
        this.path = path;
        this.code = code;
        this.content = content;
    }
    
    @Column(nullable = false)
    private String path;
    @Column(nullable = false)
    private int code;
    @Column(nullable = false)
    private String content;
    @Column(nullable = false)
    private int site_id;


    public int getSite_id() {
        return site_id;
    }

    public void setSite_id(int site_id) {
        this.site_id = site_id;
    }



    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }


}
