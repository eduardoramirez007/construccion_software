package com.olva.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class RipleyOrderDTO {

    private String orderNumber;
    private String cudNumber;
    private String uniqueCode;
    private Integer storeId;
    private OffsetDateTime deliveryDate;

    private String fullName;
    private String address;
    private String district;

    private String productCode;
    private String productDescription;
    private Integer qty;
    private BigDecimal price;

    private Integer deliveryStore;
    private String phone;

    private String workingDay;
    private BigDecimal mtrcub;

    private String dispatchId;
    private String observation;
    private String boxVirtual;

    private String sizeCode;
    private String sizeName;

    private String email;

    // getters y setters
}