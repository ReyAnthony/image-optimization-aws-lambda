package com.mutum.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.mutum.lambda.exceptions.AlreadyResizedException;

import java.io.IOException;

public class ResizeRequestHandler extends AbstractHandler<ResizeRequest, ResizeAnswer> {

    @Override
    public ResizeAnswer handleRequest(ResizeRequest req, Context context) {

        logger = context.getLogger();
        logger.log(req.toString());

        final AmazonS3Client client = new AmazonS3Client();
        S3Object s3Object = client.getObject(new GetObjectRequest(req.getInputBucket(), req.getInputObjectKey()));
        String savedUrl;

        try {

            byte[] resizedImageBytes = resize(s3Object, req);
            savedUrl = uploadNewFileToS3(resizedImageBytes, req, client);

        } catch (IOException | AlreadyResizedException e) {
            logger.log(e.getMessage());
            return ResizeAnswer
                    .builder()
                    .success(false)
                    .build();
        }

        return ResizeAnswer
                .builder()
                .success(true)
                .url(savedUrl)
                .build();
    }
}
