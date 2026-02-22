package tn.esprit.twin.microserviceproduits.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

@FeignClient(name = "MicroServiceDouaa")
public interface OffreClient {

    @GetMapping("/offres")
    List<Offre> getAllOffres();

    @GetMapping("/offres/{id}")
    Offre getOffreById(@PathVariable("id") int id);
}

