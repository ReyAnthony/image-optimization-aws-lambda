# image-optimization-aws-lambda
An aws lambda whose goal is to thumbnail and reduce the quality of images uploaded to an S3 bucket

Expected IAM policy :

``` 
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "logs:CreateLogGroup",
                "logs:CreateLogStream",
                "logs:PutLogEvents"
            ],
            "Resource": "arn:aws:logs:*:*:*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "s3:ListBucket"
            ],
            "Resource": [
                "arn:aws:s3:::BUCKET"
            ]
        },
        {
            "Effect": "Allow",
            "Action": [
                "s3:PutObject",
                "s3:GetObject",
                "s3:DeleteObject",
                "s3:PutObjectAcl"
            ],
            "Resource": [
                "arn:aws:s3:::BUCKET/*"
            ]
        }
    ]
}
```

Two events are supported : 
- S3Event, the lambda has to be configured by env variables directly in the lambda conf
- ResizeRequest has this format : 


``` 
{
  "inputBucket": "alpha-images",
  "inputObjectKey": "uploads/mutum-logo.png",
 
  "outputBucket": "alpha-images",
  "outputPath": "uploads",
  "quality": "0.7",
  "fileExtension": "jpg",
  "resizedWidth": "500",
  "resizedHeight": "500",
  "contentType": "image/jpeg"
} 
```
