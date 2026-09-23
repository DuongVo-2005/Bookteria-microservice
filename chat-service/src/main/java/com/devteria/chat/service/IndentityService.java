package com.devteria.chat.service;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.devteria.chat.dto.request.IntrospectRequest;
import com.devteria.chat.dto.response.IntrospectResponse;
import com.devteria.chat.repository.httpclient.IndentityClient;

import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IndentityService {

    IndentityClient indentityClient;

    public IntrospectResponse introspect(IntrospectRequest request) {
        try {
            var result = indentityClient.introspect(request).getResult();
            if (Objects.isNull(result)) {
                return IntrospectResponse.builder().valid(false).build();
            }
            return result;
        } catch (FeignException exception) {
            log.info("Introspect failed: {}", exception.getMessage(), exception);
            return IntrospectResponse.builder().valid(false).build();
        }
    }
}
