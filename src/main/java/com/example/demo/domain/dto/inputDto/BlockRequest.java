package com.example.demo.domain.dto.inputDto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockRequest {

    @NotNull
    private String targetUserEmail;

}
