package tqs.zeromonos.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "municipalities")
public class Municipality {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    public Municipality() {
    }

    public Municipality(String name) {
        this.name = name;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }
    // Não fazemos setter do id porque é auto-gerado

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}
