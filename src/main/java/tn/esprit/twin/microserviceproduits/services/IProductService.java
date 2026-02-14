package tn.esprit.twin.microserviceproduits.services;

import tn.esprit.twin.microserviceproduits.entities.Product;

import java.util.List;

public interface IProductService {
    List<Product> getAllProducts();
    Product getProductById(Long id);
    Product addProduct(Product product);
    Product updateProduct(Long id, Product product);
    void deleteProduct(Long id);
    List<Product> getProductsByCategory(String category);
}
