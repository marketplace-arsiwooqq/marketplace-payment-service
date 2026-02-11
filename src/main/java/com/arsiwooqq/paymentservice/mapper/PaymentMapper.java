package com.arsiwooqq.paymentservice.mapper;

import com.arsiwooqq.paymentservice.dto.PaymentResponse;
import com.arsiwooqq.paymentservice.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentMapper {
    PaymentResponse toResponse(Payment payment);
}
