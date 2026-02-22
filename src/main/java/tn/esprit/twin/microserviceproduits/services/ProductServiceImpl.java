package tn.esprit.twin.microserviceproduits.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.twin.microserviceproduits.client.Offre;
import tn.esprit.twin.microserviceproduits.client.OffreClient;
import tn.esprit.twin.microserviceproduits.entities.Product;
import tn.esprit.twin.microserviceproduits.repositories.ProductRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements IProductService {

    private final ProductRepository productRepository;
    private final OffreClient offreClient;

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
        return productRepository.save(product);
    }

    @Override
    public Product updateProduct(Long id, Product product) {
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
}
