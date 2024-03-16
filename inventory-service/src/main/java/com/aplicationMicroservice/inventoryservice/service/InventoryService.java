package com.aplicationMicroservice.inventoryservice.service;

import com.aplicationMicroservice.inventoryservice.dto.InventoryResponse;
import com.aplicationMicroservice.inventoryservice.repository.InventoryRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    @Autowired
    private final InventoryRepository repository;

    @Transactional(readOnly = true)
    @SneakyThrows
    public List<InventoryResponse> Stock(List<String> skcode) {
        log.info("Wait start here");
        Thread.sleep(100);
        log.info("Wait end here");
        return repository.findByskcodeIn(skcode).stream().map(inventory ->
                InventoryResponse.builder()
                        .skcode(inventory.getSkcode())
                        .IsInStock(inventory.getQuantity() > 0)
                        .quantity(inventory.getQuantity())
                        .build()).toList();
    }
}
