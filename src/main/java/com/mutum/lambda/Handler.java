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
    private final String OUTPUT_BUCKET = getenv("OUTPUT_BUCKET");
    private final String OUTPUT_PATH = getenv("OUTPUT_PATH_IN_BUCKET");
    private final double QUALITY = Double.parseDouble(getenv("QUALITY"));
    private final String FILE_EXT = getenv("FILE_EXT");
    private final int RESIZED_WIDTH = Integer.parseInt(getenv("RESIZED_WIDTH"));
    private final int RESIZED_HEIGHT = Integer.parseInt(getenv("RESIZED_HEIGHT"));
    private final String CONTENT_TYPE = getenv("CONTENT_TYPE");

    public void handle(S3Event s3Event, Context context) throws IOException {

        final LambdaLogger logger = context.getLogger();

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
                String s3ObjectKey = s3Entity.getObject().getKey();
                S3Object s3Object =  client.getObject(
                                        new GetObjectRequest(
                                            s3Entity.getBucket().getName(),
                                             s3ObjectKey));

                if(s3Object.getObjectMetadata().getUserMetaDataOf(ALREADY_RESIZED_KEY) != null){

                    logger.log("The file with key : " + s3ObjectKey + " was already processed.");
                    continue;
                }

                String outputFilename = FilenameUtils.getBaseName(s3ObjectKey);
                logger.log("Starting conversion of : " + s3ObjectKey);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try(InputStream is = s3Object.getObjectContent()) {

                    Thumbnails.of(is)
                            .size(RESIZED_WIDTH, RESIZED_HEIGHT)
                            .outputQuality(QUALITY)
                            .outputFormat(FILE_EXT)
                            .toOutputStream(baos);
                }

                byte[] baosBytes = baos.toByteArray();
                String savedFileKey = OUTPUT_PATH + outputFilename + "." + FILE_EXT;

                try(InputStream is = new ByteArrayInputStream(baosBytes)) {

                    ObjectMetadata objectMetadata = new ObjectMetadata();
                    objectMetadata.setContentLength(baosBytes.length);
                    objectMetadata.setContentType(CONTENT_TYPE);
                    //the value could be wathever really
                    objectMetadata.addUserMetadata(ALREADY_RESIZED_KEY, "true");

                    client.putObject(OUTPUT_BUCKET,
                            savedFileKey,
                            is,
                            objectMetadata);
                }

                logger.log("Processed " + savedFileKey);

                logger.log("Setting " + savedFileKey + " rights");
                client.setObjectAcl(new SetObjectAclRequest(OUTPUT_BUCKET, savedFileKey, PublicRead));
                logger.log("Right were given to : " + savedFileKey);

            }
        }
    }
}
