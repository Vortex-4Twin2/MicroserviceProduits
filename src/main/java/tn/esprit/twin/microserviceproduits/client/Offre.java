package tn.esprit.twin.microserviceproduits.client;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class Offre implements Serializable {

    private Integer id;

    private String nomOffre;

    private TypeOffre typeOffre;

    private Double valeurReduction;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateDebut;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateFin;

    private StatutOffre statut;
    private Long productId;
}