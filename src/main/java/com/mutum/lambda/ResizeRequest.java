package com.mutum.lambda;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ResizeRequest {

    private String outputBucket;
    private String outputPath;
    private double quality;
    private String fileExtension;
    private int resizedWidth;
    private int resizedHeight;
    private String contentType;

    private String inputBucket;
    private String inputObjectKey;
}
