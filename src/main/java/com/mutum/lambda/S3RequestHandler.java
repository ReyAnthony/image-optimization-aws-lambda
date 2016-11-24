package com.mutum.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.event.S3EventNotification.S3Entity;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.mutum.lambda.exceptions.AlreadyResizedException;

import java.io.IOException;
import java.util.List;

import static java.lang.System.getenv;

public class S3RequestHandler extends AbstractHandler<S3Event, Boolean> {

    @Override
    public Boolean handleRequest(S3Event s3Event, Context context) {

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

        try{
             handleS3Event(resizeRequest, s3Event);
        } catch (IOException | AlreadyResizedException e) {
            logger.log(e.getMessage());
            return false;
        }

        return true;
    }

    private void handleS3Event(ResizeRequest req, S3Event s3Event) throws IOException, AlreadyResizedException {

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

                byte[] resizedImageBytes = resize(s3Object, req);
                uploadNewFileToS3(resizedImageBytes, req, client);

            }
        }
    }

}
