package com.mutum.lambda;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.SetObjectAclRequest;
import com.mutum.lambda.exceptions.AlreadyResizedException;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FilenameUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.amazonaws.services.s3.model.CannedAccessControlList.PublicRead;

abstract class AbstractHandler<T,U> implements RequestHandler<T,U> {

    private static final String ALREADY_RESIZED_KEY = "resized-and-compressed";
    protected static LambdaLogger logger;

    protected byte[] resize(S3Object imageToResize, ResizeRequest req) throws IOException, AlreadyResizedException {

        logger.log("Starting conversion of : " + req.getInputObjectKey());
        if (imageToResize.getObjectMetadata().getUserMetaDataOf(ALREADY_RESIZED_KEY) != null) {
            logger.log("The file with key : " + req.getInputObjectKey() + " was already processed.");
            throw new AlreadyResizedException();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try(InputStream is = imageToResize.getObjectContent()) {

            Thumbnails.of(is)
                    .size(req.getResizedWidth(), req.getResizedHeight())
                    .outputQuality(req.getQuality())
                    .outputFormat(req.getFileExtension())
                    .toOutputStream(baos);
        }

        return baos.toByteArray();
    }

    protected String uploadNewFileToS3(byte[] imageBytes, ResizeRequest req, AmazonS3Client client) throws IOException {

        final String s3ObjectKey = req.getInputObjectKey();
        final String outputFilename = FilenameUtils.getBaseName(s3ObjectKey);
        final String savedFileKey = req.getOutputPath() + outputFilename + "." + req.getFileExtension();

        logger.log("Uploading the file "  + outputFilename);

        try(InputStream is = new ByteArrayInputStream(imageBytes)) {

            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(imageBytes.length);
            objectMetadata.setContentType(req.getContentType());
            objectMetadata.addUserMetadata(ALREADY_RESIZED_KEY, "true");

            client.putObject(req.getOutputBucket(), savedFileKey, is, objectMetadata);
        }

        logger.log("Processed " + savedFileKey);
        addPublicReadRights(client, req, savedFileKey);
        return client.getResourceUrl(req.getOutputBucket(), savedFileKey);
    }

    private void addPublicReadRights(AmazonS3Client client, ResizeRequest req, String savedFileKey){

        logger.log("Setting " + savedFileKey + " rights");
        client.setObjectAcl(new SetObjectAclRequest(req.getOutputBucket(), savedFileKey, PublicRead));
        logger.log("PublicRead rights were given to : " + savedFileKey);
    }
}
