package com.vseonlineshop.shopservice.dto.order.response;

public record CustomerInformationResponse(

        String firstName,

        String lastName,

        String email,

        String phone

) {
}