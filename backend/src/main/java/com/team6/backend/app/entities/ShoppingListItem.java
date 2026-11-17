package com.team6.backend.app.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="shopping_list_item")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ShoppingListItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shopping_list_id", nullable = false)
    private ShoppingList shoppingList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_reference", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Short quantity;
}
