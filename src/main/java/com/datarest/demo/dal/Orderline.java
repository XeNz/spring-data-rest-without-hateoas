package com.datarest.demo.dal;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "orderline_table")
public class Orderline {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)

    private Long id;
    private long amount = 0;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Transient
    private BigDecimal cost;

    @ManyToOne
    private Product product;
    @ManyToOne
    private Order order;

    public BigDecimal getCost() {
        return product.getPrice().multiply(BigDecimal.valueOf(amount));
    }
}
