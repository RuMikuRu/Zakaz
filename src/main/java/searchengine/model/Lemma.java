package searchengine.model;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "lemma")
public class Lemma implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", referencedColumnName = "id")
    private SitePage siteModelId;

    @OneToMany(mappedBy = "lemma", cascade = CascadeType.ALL)
    private List<IndexSearch> index = new ArrayList<>();

    private String lemma;

    private int frequency;

    public Lemma(String lemma, int frequency, SitePage siteModelId) {
        this.lemma = lemma;
        this.frequency = frequency;
        this.siteModelId = siteModelId;
    }

    public Lemma() {
    }
}