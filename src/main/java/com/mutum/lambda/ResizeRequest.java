package com.mutum.lambda;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
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
