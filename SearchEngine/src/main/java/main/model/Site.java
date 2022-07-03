package main.model;

import javax.persistence.*;
import java.sql.Date;
import java.time.LocalDateTime;

@Entity
@Table(name = "site")
public class Site
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    private String status;

    private Date statusTime;

    private String lastError;

    private String url;

    private String name;

    public Site(String url, String name) {
        this.url = url;
        this.name = name;
    }

    public Site(String status, Date statusTime, String url, String name) {
        this.status = status;
        this.statusTime = statusTime;
        this.url = url;
        this.name = name;
    }

    public Site(int id, String status, Date statusTime, String lastError, String url, String name) {
        this.id = id;
        this.status = status;
        this.statusTime = statusTime;
        this.lastError = lastError;
        this.url = url;
        this.name = name;
    }
    public Site() {
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getStatusTime() {
        return statusTime;
    }

    public void setStatusTime(Date statusTime) {
        this.statusTime = statusTime;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
