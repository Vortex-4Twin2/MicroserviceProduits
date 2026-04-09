package tn.esprit.twin.microserviceproduits.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import tn.esprit.twin.microserviceproduits.client.Offre;
import tn.esprit.twin.microserviceproduits.client.OffreClient;
import tn.esprit.twin.microserviceproduits.entities.Product;
import tn.esprit.twin.microserviceproduits.repositories.ProductRepository;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements IProductService {

    private final ProductRepository productRepository;
    private final OffreClient offreClient;
    
    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    @Override
    public Boolean checkProductExists( Long id){
        return productRepository.existsById(id);
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    @Override
    public Product addProduct(Product product) {
        checkForSpamOrDuplicate(product, null);
        return productRepository.save(product);
    }

    @Override
    public Product updateProduct(Long id, Product product) {
        checkForSpamOrDuplicate(product, id);
        
        Product existingProduct = getProductById(id);
        existingProduct.setName(product.getName());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setStockQuantity(product.getStockQuantity());
        existingProduct.setCategory(product.getCategory());
        return productRepository.save(existingProduct);
    }

    @Override
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    @Override
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategoryIgnoreCase(category);
    }
    
    @Override
    public List<String> getAllCategories() {
        return productRepository.findAllDistinctCategories();
    }
    
    @Override
    public List<Offre> getOffres() {
        return offreClient.getAllOffres();
    }

    @Override
    public Offre getOffreById(int id) {
        return offreClient.getOffreById(id);
    }

    // ========== Communication synchrone avec le microservice OFFRE ==========
    @Override
    public List<Offre> getOffresActivesByProductId(Long productId) {
        try {
            log.info("Calling OFFRE service via Feign (Eureka) for product: {}", productId);
            
            List<Offre> allOffres = offreClient.getAllOffres();
            
            if (allOffres == null) {
                log.warn("OFFRE service returned null list");
                return List.of();
            }
            
            List<Offre> offresActives = allOffres.stream()
                    .filter(offre -> offre.getProductId() != null && 
                                    offre.getProductId().equals(productId) && 
                                    offre.getStatut() == tn.esprit.twin.microserviceproduits.client.StatutOffre.ACTIVE)
                    .toList();
            
            log.info("Successfully retrieved {} active offers for product {}", offresActives.size(), productId);
            return offresActives;
        } catch (Exception e) {
            log.error("CRITICAL ERROR: Failed to communicate with OFFRE microservice: {}", e.getMessage());
            throw new RuntimeException("Communication Error with OFFRE service. Please check if it's running and registered in Eureka.", e);
        }
    }

    // ========== Advanced Business Logic (Métier Avancé) ==========
    
    @Override
    public List<Product> getLowStockProducts(Integer threshold) {
        List<Product> lowStockProducts = productRepository.findByStockQuantityLessThanEqual(threshold);
        
        if (!lowStockProducts.isEmpty()) {
            log.warn("=== SMART STOCK ALERT ===");
            for (Product p : lowStockProducts) {
                log.warn("ALERT: Product [{}] '{}' is running low on stock! Current stock: {}", 
                        p.getId(), p.getName(), p.getStockQuantity());
                
                // Demande de l'utilisateur : N'envoyer le mail QUE si le stock est inférieur à 5
                if (p.getStockQuantity() != null && p.getStockQuantity() < 5) {
                    try {
                        if (mailSender != null) {
                            SimpleMailMessage message = new SimpleMailMessage();
                            message.setTo("souhir.krizi02@gmail.com"); 
                            message.setSubject("🚨 URGENT: Stock Critique  - " + p.getName());
                            message.setText("Bonjour Administrateur,\n\n" +
                                            "Le produit '" + p.getName() + "' (ID: " + p.getId() + ") " +
                                            "est en situation de STOCK CRITIQUE.\n" +
                                            "Il reste actuellement : " + p.getStockQuantity() + " unité(s).\n\n" +
                                            "Veuillez réapprovisionner ce produit immédiatement avant la rupture totale.");
                            
                            mailSender.send(message);
                            log.info("AUTOMATIC NOTIFICATION: Email de stock critique envoyé à souhir.krizi02@gmail.com pour '{}'.", p.getName());
                        } else {
                            log.info("MAIL CONFIG MISSING: Impossible d'envoyer l'email. Simulation pour '{}'.", p.getName());
                        }
                    } catch (Exception e) {
                        log.error("Erreur lors de l'envoi de l'email : {}", e.getMessage());
                    }
                } else {
                    log.info("Le produit '{}' est bas (Stock: {}) mais toujours >= 5. Pas de mail envoyé.", p.getName(), p.getStockQuantity());
                }
            }
        }
        return lowStockProducts;
    }

    // ========== Anti-Spam / Fake Product Detection ==========

    private void checkForSpamOrDuplicate(Product newProduct, Long excludedId) {
        if (newProduct == null || newProduct.getName() == null) return;
        
        List<Product> existingProducts = productRepository.findAll();
        
        for (Product existing : existingProducts) {
            if (excludedId != null && existing.getId().equals(excludedId)) {
                continue;
            }
            
            // 1. Check exact name duplicate
            if (existing.getName() != null && 
                existing.getName().trim().equalsIgnoreCase(newProduct.getName().trim())) {
                log.warn("🚨 ANTI-SPAM: Tentative d'ajout d'un produit avec un nom dupliqué '{}'", newProduct.getName());
                throw new RuntimeException("Anti-Spam : Un produit avec exactement ce nom existe déjà. Création bloquée.");
            }
            
            // 2. Check description similarity (Jaccard Index)
            if (existing.getDescription() != null && newProduct.getDescription() != null) {
                double similarity = calculateJaccardSimilarity(existing.getDescription(), newProduct.getDescription());
                if (similarity > 0.80) { // 80% similarity threshold
                    log.error("🚨 ANTI-SPAM: Description copiée détectée (Similarité {}%) avec le produit '{}'", 
                            String.format("%.2f", similarity * 100), existing.getName());
                    throw new RuntimeException("Anti-Spam : La description de ce produit est trop similaire (copiée) d'un produit existant. Veuillez écrire une description unique.");
                }
            }
        }
    }

    private double calculateJaccardSimilarity(String s1, String s2) {
        if (s1.trim().isEmpty() || s2.trim().isEmpty()) return 0.0;
        
        Set<String> words1 = new HashSet<>(Arrays.asList(s1.toLowerCase().split("\\W+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(s2.toLowerCase().split("\\W+")));
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        if (union.isEmpty()) return 0.0;
        return (double) intersection.size() / union.size();
    }
}
