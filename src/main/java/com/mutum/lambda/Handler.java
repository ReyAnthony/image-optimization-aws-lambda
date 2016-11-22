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
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.entity.ContentType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static java.lang.System.getenv;

public class Handler {

    public void handle(S3Event s3Event, Context context) throws IOException{

        final LambdaLogger logger = context.getLogger();

        final String OUTPUT_BUCKET = getenv("OUTPUT_BUCKET");
        final String OUTPUT_PATH = getenv("OUTPUT_PATH_IN_BUCKET");
        final double QUALITY = Double.parseDouble(getenv("QUALITY"));
        final String FILE_EXT = getenv("FILE_EXT");
        final int RESIZED_WIDTH = Integer.parseInt(getenv("RESIZED_WIDTH"));
        final int RESIZED_HEIGHT = Integer.parseInt(getenv("RESIZED_HEIGHT"));
        final String CONTENT_TYPE = getenv("CONTENT_TYPE");

        logger.log("OUPUT_BUCKET : " + OUTPUT_BUCKET);
        logger.log("OUTPUT_PATH : " + OUTPUT_PATH);
        logger.log("RESIZED_WIDTH : " + RESIZED_WIDTH);
        logger.log("RESIZED_HEIGHT : " + RESIZED_HEIGHT);
        logger.log("QUALITY : " + QUALITY);
        logger.log("FILE_EXT : " + FILE_EXT);

        final List<S3EventNotificationRecord> records = s3Event.getRecords();
        final AmazonS3Client client = new AmazonS3Client();

        for(S3EventNotificationRecord record : records){

            if(record.getEventName().equals("ObjectCreated:Put") &&
                    record.getEventSource().equals("aws:s3")){

                S3Entity s3Entity = record.getS3();
                S3Object s3Object =  client.getObject(
                                        new GetObjectRequest(
                                            s3Entity.getBucket().getName(),
                                            s3Entity.getObject().getKey()));

                String outputFilename = FilenameUtils.getBaseName(s3Entity.getObject().getKey());

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try(InputStream is = s3Object.getObjectContent()) {

                    Thumbnails.of(is)
                            .size(RESIZED_WIDTH, RESIZED_HEIGHT)
                            .outputQuality(QUALITY)
                            .outputFormat(FILE_EXT)
                            .toOutputStream(baos);
                }

                byte[] baosBytes = baos.toByteArray();
                try(InputStream is = new ByteArrayInputStream(baosBytes)) {

                    ObjectMetadata objectMetadata = new ObjectMetadata();
                    objectMetadata.setContentLength(baosBytes.length);
                    objectMetadata.setContentType(CONTENT_TYPE);

                    client.putObject(OUTPUT_BUCKET,
                            OUTPUT_PATH + outputFilename + "." + FILE_EXT,
                            is,
                            objectMetadata);
                }

                logger.log("Processed " + outputFilename + "." + FILE_EXT);
                logger.log("It was saved in the bucket " + OUTPUT_BUCKET + " at path : " + OUTPUT_PATH);
            }
        }
    }
}
