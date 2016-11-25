package com.mutum.lambda;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResizeAnswer {

    private boolean success;
    private String url;
}
