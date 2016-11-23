package com.mutum.lambda.exceptions;

public class AlreadyResizedException extends Exception {

    public AlreadyResizedException(){
        super("This image has already been resized");
    }
}
