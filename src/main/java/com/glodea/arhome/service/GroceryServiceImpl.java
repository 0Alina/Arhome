package com.glodea.arhome.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.glodea.arhome.dto.GroceryItemResponse;
import com.glodea.arhome.entity.GroceryItem;
import com.glodea.arhome.entity.User;
import com.glodea.arhome.repository.GroceryItemRepository;

@Service
@Transactional
public class GroceryServiceImpl implements GroceryService {

    private final GroceryItemRepository groceryItemRepository;

    public GroceryServiceImpl(GroceryItemRepository groceryItemRepository) {
        this.groceryItemRepository = groceryItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroceryItemResponse> listItems(User user) {
        if (user == null || user.getId() == null) {
            return List.of();
        }
        return groceryItemRepository.findByUserIdOrderByCreatedAtAsc(user.getId()).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    public GroceryItemResponse addItem(User user, String productName, String quantity, String unit) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Authentication required.");
        }

        String normalizedName = productName == null ? "" : productName.trim();
        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException("Product is required.");
        }

        String normalizedQuantity = quantity == null ? "" : quantity.trim();
        if (normalizedQuantity.isBlank()) {
            throw new IllegalArgumentException("Quantity is required.");
        }

        BigDecimal parsedQuantity;
        try {
            parsedQuantity = new BigDecimal(normalizedQuantity.replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid quantity.");
        }

        if (parsedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        }

        String normalizedUnit = unit == null ? "" : unit.trim();
        if (normalizedUnit.isBlank()) {
            normalizedUnit = "unit";
        }

        GroceryItem item = new GroceryItem();
        item.setUser(user);
        item.setProductName(normalizedName);
        item.setQuantity(parsedQuantity);
        item.setUnit(normalizedUnit);
        item.setBought(false);

        GroceryItem saved = groceryItemRepository.save(item);
        return toResponse(saved);
    }

    @Override
    public GroceryItemResponse setBought(User user, Long id, boolean bought) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Authentication required.");
        }

        GroceryItem item = groceryItemRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Item not found."));

        item.setBought(bought);
        GroceryItem saved = groceryItemRepository.save(item);
        return toResponse(saved);
    }

    @Override
    public void deleteItem(User user, Long id) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Authentication required.");
        }

        GroceryItem item = groceryItemRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Item not found."));

        groceryItemRepository.delete(item);
    }

    private GroceryItemResponse toResponse(GroceryItem item) {
        return new GroceryItemResponse(
            item.getId(),
            item.getProductName(),
            item.getQuantity() != null ? item.getQuantity().stripTrailingZeros().toPlainString() : "",
            item.getUnit(),
            item.isBought()
        );
    }
}
