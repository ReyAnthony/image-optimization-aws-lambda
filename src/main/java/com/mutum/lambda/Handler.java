package com.mutum.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.event.S3EventNotification.S3Entity;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static java.lang.System.getenv;

public class Handler {

    public void handle(S3Event s3Event, Context context) throws IOException{

        final String OUTPUT_BUCKET = getenv("OUTPUT_BUCKET");
        final String OUTPUT_PATH = getenv("OUTPUT_PATH_IN_BUCKET");
        final double QUALITY = Double.parseDouble(getenv("QUALITY"));
        final String FILE_EXT = getenv("FILE_EXT");

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
                //TODO We must do this in memory to gain time
                File outputFile = new File("/tmp/" + outputFilename + "." + FILE_EXT);

                try(InputStream is = s3Object.getObjectContent()) {

                    Thumbnails.of(is)
                            .size(500, 500)
                            .outputQuality(QUALITY)
                            .outputFormat(FILE_EXT).toFile(outputFile);
                }

                client.putObject(OUTPUT_BUCKET,
                                 OUTPUT_PATH + outputFilename + "." + FILE_EXT,
                                 outputFile);
            }
        }
    }
}
