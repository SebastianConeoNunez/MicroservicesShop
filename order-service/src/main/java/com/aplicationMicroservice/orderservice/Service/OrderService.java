package com.aplicationMicroservice.orderservice.Service;

import com.aplicationMicroservice.orderservice.Event.OrderPlaceEvent;
import com.aplicationMicroservice.orderservice.Model.Order;
import com.aplicationMicroservice.orderservice.Model.OrderLineItems;
import com.aplicationMicroservice.orderservice.Repository.OrderRepository;
import com.aplicationMicroservice.orderservice.dto.InventoryResponse;
import com.aplicationMicroservice.orderservice.dto.OrderLineItemsDto;
import com.aplicationMicroservice.orderservice.dto.OrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {


    @Autowired
    private OrderRepository repository;

    @Autowired
    private final WebClient.Builder webClientBuilder;

    @Autowired
    private final KafkaTemplate<String,OrderPlaceEvent> kafkaTemplate;

    public String PlaceOrderr(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        List<OrderLineItems> lista = orderRequest.getOrderLineItemsListDto().stream().map(this::MapTOOrderLineItems).toList();
        order.setOrderLineItemsList(lista);

       List<String> skcodes = order.getOrderLineItemsList().stream().map(OrderlineItem -> OrderlineItem.getSkcode()).toList();


        InventoryResponse[] Arrayresponse = webClientBuilder.build().get()
                        .uri("http://inventory-service/api/inventory",
                                uriBuilder -> uriBuilder.queryParam("skucode",skcodes).build())
                        .retrieve()
                        .bodyToMono(InventoryResponse[].class)
                        .block();

       boolean response = Arrays.stream(Arrayresponse).allMatch(inventoryResponse -> inventoryResponse.isIsInStock());

       int flat = 0;

       for(OrderLineItems Ol : lista){
           for(InventoryResponse in : Arrayresponse){
               if((Ol.getSkcode().equals(in.getSkcode())) && (in.getQuantity() >= Ol.getQuantity())){
                    flat = flat +1 ;
               }
           }
       }

        if (response && (skcodes.size() == Arrayresponse.length ) && (flat == skcodes.size())){
            kafkaTemplate.send("NotificationTopic",new OrderPlaceEvent(order.getOrderNumber()));
            repository.save(order);
            return "Order created successfully";
        }else {
            throw new IllegalArgumentException("There are prodcut that are not in stock");
        }


}

    private OrderLineItems MapTOOrderLineItems(OrderLineItemsDto orderLineItemsDto) {

        OrderLineItems objetoOrder = OrderLineItems.builder()
                .Skcode(orderLineItemsDto.getSkcode())
                .price(orderLineItemsDto.getPrice())
                .quantity(orderLineItemsDto.getQuantity())
                .build();

        return objetoOrder;
    }


    }
