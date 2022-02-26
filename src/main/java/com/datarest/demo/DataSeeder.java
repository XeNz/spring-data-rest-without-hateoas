package com.datarest.demo;

import com.datarest.demo.dal.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DataSeeder {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final AuthorRepository authorRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        Author author = Author.builder().firstName("alexander").lastName("willemsen").build();
        authorRepository.save(author);

        Customer customer = Customer.builder().name("De Striep").build();
        customerRepository.save(customer);

        Product product = Product.builder()
                .title("Boekje")
                .author(author)
                .price(BigDecimal.valueOf(15L))
                .build();
        productRepository.save(product);

        Orderline orderline = Orderline.builder().product(product).amount(5).build();
        Order order = Order.builder()
                .customer(customer)
                .deliverDate(LocalDate.now().plusDays(4))
                .orderStatus(OrderStatus.DRAFT)
                .build();

        order.add(orderline);
        orderRepository.save(order);
    }
}
