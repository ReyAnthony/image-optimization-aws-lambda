package com.mutum.lambda;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
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

    public String toString() {
        return "com.mutum.lambda.ResizeRequest(outputBucket=" + this.getOutputBucket() + ", outputPath=" + this.getOutputPath() + ", quality=" + this.getQuality() + ", fileExtension=" + this.getFileExtension() + ", resizedWidth=" + this.getResizedWidth() + ", resizedHeight=" + this.getResizedHeight() + ", contentType=" + this.getContentType() + ", inputBucket=" + this.getInputBucket() + ", inputObjectKey=" + this.getInputObjectKey() + ")";
    }
}
