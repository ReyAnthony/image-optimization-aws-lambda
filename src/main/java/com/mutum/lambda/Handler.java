package com.mutum.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.event.S3EventNotification.S3Entity;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.GetObjectRequest;
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
import java.util.List;

import static com.amazonaws.services.s3.model.CannedAccessControlList.PublicRead;
import static java.lang.System.getenv;

public class Handler {

    private static final String ALREADY_RESIZED_KEY = "resized-and-compressed";
    private static LambdaLogger logger;

    public void s3Handler(S3Event s3Event, Context context)  {

        final String OUTPUT_BUCKET = getenv("OUTPUT_BUCKET");
        final String OUTPUT_PATH = getenv("OUTPUT_PATH_IN_BUCKET");
        final double QUALITY = Double.parseDouble(getenv("QUALITY"));
        final String FILE_EXT = getenv("FILE_EXT");
        final int RESIZED_WIDTH = Integer.parseInt(getenv("RESIZED_WIDTH"));
        final int RESIZED_HEIGHT = Integer.parseInt(getenv("RESIZED_HEIGHT"));
        final String CONTENT_TYPE = getenv("CONTENT_TYPE");

        logger = context.getLogger();
        logger.log("OUPUT_BUCKET : " + OUTPUT_BUCKET);
        logger.log("OUTPUT_PATH : " + OUTPUT_PATH);
        logger.log("RESIZED_WIDTH : " + RESIZED_WIDTH);
        logger.log("RESIZED_HEIGHT : " + RESIZED_HEIGHT);
        logger.log("QUALITY : " + QUALITY);
        logger.log("FILE_EXT : " + FILE_EXT);

        final ResizeRequest resizeRequest =
            ResizeRequest
                .builder()
                .outputBucket(OUTPUT_BUCKET)
                .outputPath(OUTPUT_PATH)
                .resizedWidth(RESIZED_WIDTH)
                .resizedHeight(RESIZED_HEIGHT)
                .quality(QUALITY)
                .fileExtension(FILE_EXT)
                .contentType(CONTENT_TYPE)
                .build();

        handleS3Event(resizeRequest, s3Event);
    }

    public void resizeEventHandler(ResizeRequest req, Context context){

        logger.log(req.toString());

        final AmazonS3Client client = new AmazonS3Client();
        S3Object s3Object = client.getObject(new GetObjectRequest(req.getInputBucket(), req.getInputObjectKey()));

        try {

            byte[] resizedImageBytes = resize(s3Object, req);
            uploadNewFileToS3(resizedImageBytes, req, client);

        } catch (IOException | AlreadyResizedException e) {
            logger.log(e.getMessage());
        }
    }

    private void handleS3Event(ResizeRequest req, S3Event s3Event) {

        final AmazonS3Client client = new AmazonS3Client();
        final List<S3EventNotificationRecord> records = s3Event.getRecords();

        for(S3EventNotificationRecord record : records) {

            if (record.getEventName().equals("ObjectCreated:Put") &&
                    record.getEventSource().equals("aws:s3")) {

                S3Entity s3Entity = record.getS3();
                String s3ObjectKey = s3Entity.getObject().getKey();
                S3Object s3Object = client.getObject(
                        new GetObjectRequest(
                                s3Entity.getBucket().getName(),
                                s3ObjectKey));

                //setup the informations missing from the resizeRequest
                req.setInputObjectKey(s3Entity.getObject().getKey());
                req.setInputBucket(s3Entity.getBucket().getName());

                try {

                    byte[] resizedImageBytes = resize(s3Object, req);
                    uploadNewFileToS3(resizedImageBytes, req, client);

                } catch (IOException | AlreadyResizedException e) {
                    logger.log(e.getMessage());
                }
            }
        }
    }

    private byte[] resize(S3Object imageToResize, ResizeRequest req) throws IOException, AlreadyResizedException {

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

    private void uploadNewFileToS3(byte[] imageBytes, ResizeRequest req, AmazonS3Client client) throws IOException {

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
    }

    private void addPublicReadRights(AmazonS3Client client, ResizeRequest req, String savedFileKey){

        logger.log("Setting " + savedFileKey + " rights");
        client.setObjectAcl(new SetObjectAclRequest(req.getOutputBucket(), savedFileKey, PublicRead));
        logger.log("PublicRead rights were given to : " + savedFileKey);
    }
}
