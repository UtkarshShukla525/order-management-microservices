package com.utkarsh.ordermgmt.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utkarsh.ordermgmt.product.dto.ProductRequest;
import com.utkarsh.ordermgmt.product.dto.ProductResponse;
import com.utkarsh.ordermgmt.product.exception.InsufficientStockException;
import com.utkarsh.ordermgmt.product.exception.ProductNotFoundException;
import com.utkarsh.ordermgmt.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Test
    void createProduct_withValidBody_returns201AndLocation() throws Exception {
        ProductRequest request = new ProductRequest("Laptop", "14-inch laptop", new BigDecimal("999.99"), 10);
        when(productService.create(any(ProductRequest.class)))
                .thenReturn(new ProductResponse(1L, "Laptop", "14-inch laptop", new BigDecimal("999.99"), 10));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void createProduct_withBlankName_returns400WithFieldErrors() throws Exception {
        ProductRequest invalid = new ProductRequest("", "desc", new BigDecimal("10.00"), 5);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void getProduct_whenMissing_returns404() throws Exception {
        when(productService.findById(99L)).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void reserveStock_whenInsufficient_returns409() throws Exception {
        when(productService.reserveStock(anyLong(), anyInt()))
                .thenThrow(new InsufficientStockException(1L, 2, 5));

        mockMvc.perform(post("/api/products/1/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":5}"))
                .andExpect(status().isConflict());
    }
}
