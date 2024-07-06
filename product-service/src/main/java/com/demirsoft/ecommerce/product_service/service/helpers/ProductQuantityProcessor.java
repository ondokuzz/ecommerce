package com.demirsoft.ecommerce.product_service.service.helpers;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.demirsoft.ecommerce.product_service.entity.Product;
import com.demirsoft.ecommerce.product_service.event.OrderCreated;
import com.demirsoft.ecommerce.product_service.event.OrderItem;

public class ProductQuantityProcessor {

    private final List<String> productIds;

    private final Map<String, Integer> mapProductIdToRequestedQuantity;

    private final List<String> missingItems;

    public ProductQuantityProcessor(OrderCreated order) {
        productIds = extractProductIdList(order);
        mapProductIdToRequestedQuantity = extractProductIdQuantityMap(order);
        missingItems = new LinkedList<String>();
    }

    public void processRequest(List<Product> products) {
        checkAndDecrementProductQuantities(products);
    }

    public List<String> getProductIds() {
        return productIds;
    }

    public boolean areThereMissingItems() {
        return missingItems.size() > 0;
    }

    public List<String> getMissingItems() {
        return missingItems;
    }

    public void checkAndDecrementProductQuantities(List<Product> products) {

        products.forEach(product -> {

            if (mapProductIdToRequestedQuantity.containsKey(product.getId())) {

                int requestedQuantity = mapProductIdToRequestedQuantity.get(product.getId());
                if (product.getQuantity() < requestedQuantity) {

                    missingItems.add(String.format("item: %s is missing by amount %d", product.getId(),
                            requestedQuantity - product.getQuantity()));

                } else {

                    product.setQuantity(product.getQuantity() - requestedQuantity);

                }

                productIds.remove(product.getId());
            }
        });

        // process remaining ids
        productIds.stream().forEach(productId -> {

            int requestedQuantity = mapProductIdToRequestedQuantity.get(productId);
            missingItems.add(String.format("item: %s is missing by amount %d", productId, requestedQuantity));

        });
    }

    private List<String> extractProductIdList(OrderCreated order) {
        return order.getItems().stream()
                .map(OrderItem::getProductId)
                .toList();

    }

    private Map<String, Integer> extractProductIdQuantityMap(OrderCreated order) {
        return order.getItems().stream()
                .collect(Collectors.toMap(OrderItem::getProductId, OrderItem::getQuantity));

    }

}
