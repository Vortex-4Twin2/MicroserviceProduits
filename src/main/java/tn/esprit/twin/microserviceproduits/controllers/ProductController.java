package tn.esprit.twin.microserviceproduits.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tn.esprit.twin.microserviceproduits.client.Offre;
import tn.esprit.twin.microserviceproduits.entities.Product;
import tn.esprit.twin.microserviceproduits.services.IProductService;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final IProductService productService;
    
    /**
     * Récupérer toutes les offres

     */
    @GetMapping("/offres")
    public List<Offre> getAllOffres() {
        return productService.getOffres();
    }

    /**
     * Récupérer une offre par ID (

     */
    @GetMapping("/offres/{id}")
    public Offre getOffreById(@PathVariable int id) {
        return productService.getOffreById(id);
    }
    
    /**
     * Récupérer les offres actives d'un produit

     */
    @GetMapping("/{id}/offres/actives")
    public ResponseEntity<List<Offre>> getOffresActivesByProductId(@PathVariable Long id) {
        List<Offre> offres = productService.getOffresActivesByProductId(id);
        return ResponseEntity.ok(offres);
    }
    @GetMapping("/exists/{id}")
    public Boolean checkProductExists(@PathVariable("id") Long id){
        return productService.checkProductExists(id);
    }



    @GetMapping
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    public Product addProduct(@RequestBody Product product) {
        return productService.addProduct(product);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        return ResponseEntity.ok(productService.updateProduct(id, product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public List<Product> getProductsByCategory(@RequestParam String category) {
        return productService.getProductsByCategory(category);
    }
}
